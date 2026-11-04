-- =====================================================================
--  AXOLOTL FITNESS CLUB · Acceso por reconocimiento facial
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  Qué agrega (no borra ni cambia tablas existentes):
--    1. rostros_clientes  : vector facial (192 números) por cliente. No fotos.
--    2. clientes.dentro   : estado booleano  TRUE (1) = dentro / FALSE (0) = fuera
--       clientes.ultimo_movimiento : fecha y hora del último movimiento
--    3. registros_acceso  : bitácora de movimientos
--                           es_entrada TRUE (1) = entrada, FALSE (0) = salida
--    4. Cada ENTRADA también se guarda en asistencias (metodo = 'FACIAL'),
--       así la gráfica de afluencia y los reportes siguen funcionando igual.
--
--  Funciones que usa la app:
--    registrar_rostro_cliente(cliente, vector)  Recepción registra/actualiza rostro
--    clientes_rostro_registrado()               ids de clientes con rostro
--    acceso_por_rostro(vector)                  identifica y alterna entrada/salida
--    registrar_movimiento_manual(cliente, bool) botones Entrada / Salida
--    ocupacion_actual()                         personas dentro ahora
-- =====================================================================

-- 1. Rostros ------------------------------------------------------------
create table if not exists public.rostros_clientes (
    cliente_id  bigint primary key references public.clientes(id) on delete cascade,
    embedding   real[] not null check (array_length(embedding, 1) = 192),
    actualizado timestamptz not null default now()
);

-- La key de la app no lee la tabla directo, solo a través de las funciones
alter table public.rostros_clientes enable row level security;
revoke all on public.rostros_clientes from anon, authenticated;

-- 2. Estado booleano del cliente (1 = dentro, 0 = fuera) ---------------
alter table public.clientes add column if not exists dentro boolean not null default false;
alter table public.clientes add column if not exists ultimo_movimiento timestamptz;

-- 3. Bitácora de entradas (1) y salidas (0) -----------------------------
create table if not exists public.registros_acceso (
    id          bigint generated always as identity primary key,
    cliente_id  bigint not null references public.clientes(id) on delete cascade,
    es_entrada  boolean not null,              -- TRUE = entrada (1) · FALSE = salida (0)
    fecha_hora  timestamptz not null default now(),
    metodo      text not null default 'FACIAL' -- FACIAL o MANUAL
);

create index if not exists registros_acceso_cliente_idx
    on public.registros_acceso (cliente_id, fecha_hora desc);
create index if not exists registros_acceso_fecha_idx
    on public.registros_acceso (fecha_hora);

grant select on public.registros_acceso to anon, authenticated;

-- Si asistencias.metodo tiene una regla CHECK con valores fijos (QR, etc.)
-- se quita para permitir 'FACIAL' y 'MANUAL'.
do $$
declare c record;
begin
    for c in
        select conname from pg_constraint
        where conrelid = 'public.asistencias'::regclass
          and contype = 'c'
          and pg_get_constraintdef(oid) ilike '%metodo%'
    loop
        execute format('alter table public.asistencias drop constraint %I', c.conname);
    end loop;
end $$;

-- 4. Utilidades ---------------------------------------------------------

-- Umbral de similitud para decir "es la misma persona" (0 a 1).
-- Si confunde a dos personas súbelo (0.75); si no reconoce, bájalo (0.65).
create or replace function public._umbral_rostro()
returns real language sql immutable
as $$ select 0.70::real $$;

-- Similitud coseno. Los vectores llegan normalizados, basta el producto punto.
create or replace function public._similitud(a real[], b real[])
returns real language sql immutable
as $$ select coalesce(sum(x * y), 0)::real from unnest(a, b) as t(x, y) $$;

-- Inicio del día de hoy en hora de México
create or replace function public._hoy_mx()
returns timestamptz language sql stable
as $$
    select date_trunc('day', now() at time zone 'America/Mexico_City')
           at time zone 'America/Mexico_City'
$$;

-- 5. Ocupación actual ---------------------------------------------------
-- Cuenta a los clientes cuyo último movimiento de hoy es una entrada (1).
-- Cada entrada suma 1 y cada salida resta 1.
create or replace function public.ocupacion_actual()
returns integer
language sql stable security definer
set search_path = public
as $$
    select count(*)::int
    from (
        select distinct on (ra.cliente_id) ra.es_entrada
        from registros_acceso ra
        where ra.fecha_hora >= _hoy_mx()
        order by ra.cliente_id, ra.fecha_hora desc, ra.id desc
    ) ultimo
    where ultimo.es_entrada
$$;

-- 6. Movimiento (lógica común) -----------------------------------------
-- p_es_entrada = null  -> automático (alterna según el estado del cliente)
-- p_es_entrada = true  -> entrada forzada (botón Entrada)
-- p_es_entrada = false -> salida forzada  (botón Salida)
--
-- Resultados: ENTRADA, SALIDA, DENEGADO, REPETIDO, YA_DENTRO, SIN_ENTRADA, NO_RECONOCIDO
drop function if exists public._registrar_movimiento(bigint, boolean, text, real);
create function public._registrar_movimiento(
    p_cliente_id bigint, p_es_entrada boolean, p_metodo text, p_similitud real)
returns table (
    r_resultado text, r_cliente_id bigint, r_nombre text, r_membresia text,
    r_activa boolean, r_es_entrada boolean, r_fecha_hora timestamptz,
    r_entrada_desde timestamptz, r_dentro_total integer, r_similitud real)
language plpgsql security definer
set search_path = public
as $$
declare
    v_info   jsonb;
    v_nombre text;
    v_num    text;
    v_activa boolean;
    v_dentro boolean;
    v_ultimo timestamptz;
    v_desde  timestamptz;
    v_nuevo  boolean;
    v_ahora  timestamptz := now();
begin
    -- Bloquea al cliente para que dos lecturas simultáneas no dupliquen registros
    select c.dentro, c.ultimo_movimiento into v_dentro, v_ultimo
    from clientes c where c.id = p_cliente_id
    for update;

    if not found then
        return query select 'NO_RECONOCIDO'::text, null::bigint, null::text, null::text,
            null::boolean, null::boolean, null::timestamptz, null::timestamptz,
            ocupacion_actual(), p_similitud;
        return;
    end if;

    select to_jsonb(cl) into v_info from clientes_lista cl where cl.id = p_cliente_id;
    v_nombre := v_info ->> 'nombre';
    v_num    := v_info ->> 'numero_membresia';
    v_activa := coalesce((v_info ->> 'activa')::boolean, false)
                and coalesce(v_info ->> 'estado', 'ACTIVO') = 'ACTIVO';

    -- Una entrada de un día anterior sin salida ya no cuenta como "dentro"
    if v_dentro and (v_ultimo is null or v_ultimo < _hoy_mx()) then
        v_dentro := false;
        update clientes set dentro = false where id = p_cliente_id;
    end if;

    if v_dentro then
        select max(ra.fecha_hora) into v_desde
        from registros_acceso ra
        where ra.cliente_id = p_cliente_id and ra.es_entrada and ra.fecha_hora >= _hoy_mx();
    end if;

    if p_es_entrada is null then
        -- El cliente sigue frente a la cámara: no se registra otra vez
        if v_ultimo is not null and v_ahora - v_ultimo < interval '60 seconds' then
            return query select 'REPETIDO'::text, p_cliente_id, v_nombre, v_num, v_activa,
                v_dentro, v_ultimo, v_desde, ocupacion_actual(), p_similitud;
            return;
        end if;
        v_nuevo := not v_dentro;
    else
        v_nuevo := p_es_entrada;
        if v_nuevo and v_dentro then
            return query select 'YA_DENTRO'::text, p_cliente_id, v_nombre, v_num, v_activa,
                true, v_ultimo, v_desde, ocupacion_actual(), p_similitud;
            return;
        end if;
        if not v_nuevo and not v_dentro then
            return query select 'SIN_ENTRADA'::text, p_cliente_id, v_nombre, v_num, v_activa,
                false, v_ultimo, null::timestamptz, ocupacion_actual(), p_similitud;
            return;
        end if;
    end if;

    -- Para entrar se necesita membresía activa (para salir no)
    if v_nuevo and not v_activa then
        return query select 'DENEGADO'::text, p_cliente_id, v_nombre, v_num, false,
            false, null::timestamptz, null::timestamptz, ocupacion_actual(), p_similitud;
        return;
    end if;

    insert into registros_acceso (cliente_id, es_entrada, fecha_hora, metodo)
    values (p_cliente_id, v_nuevo, v_ahora, p_metodo);

    update clientes set dentro = v_nuevo, ultimo_movimiento = v_ahora
    where id = p_cliente_id;

    if v_nuevo then
        insert into asistencias (cliente_id, fecha_hora_ingreso, metodo)
        values (p_cliente_id, v_ahora, p_metodo);
    end if;

    return query select
        case when v_nuevo then 'ENTRADA' else 'SALIDA' end::text,
        p_cliente_id, v_nombre, v_num, v_activa, v_nuevo, v_ahora,
        case when v_nuevo then v_ahora else v_desde end,
        ocupacion_actual(), p_similitud;
end $$;

revoke all on function public._registrar_movimiento(bigint, boolean, text, real) from public, anon, authenticated;

-- 7. Acceso automático por rostro --------------------------------------
drop function if exists public.acceso_por_rostro(real[]);
create function public.acceso_por_rostro(p_embedding real[])
returns table (
    r_resultado text, r_cliente_id bigint, r_nombre text, r_membresia text,
    r_activa boolean, r_es_entrada boolean, r_fecha_hora timestamptz,
    r_entrada_desde timestamptz, r_dentro_total integer, r_similitud real)
language plpgsql security definer
set search_path = public
as $$
declare
    v_id  bigint;
    v_sim real;
begin
    if p_embedding is null or array_length(p_embedding, 1) <> 192 then
        raise exception 'Vector facial inválido';
    end if;

    select m.cliente_id, m.sim into v_id, v_sim
    from (
        select rc.cliente_id, _similitud(rc.embedding, p_embedding) as sim
        from rostros_clientes rc
    ) m
    order by m.sim desc
    limit 1;

    if v_id is null or v_sim < _umbral_rostro() then
        return query select 'NO_RECONOCIDO'::text, null::bigint, null::text, null::text,
            null::boolean, null::boolean, null::timestamptz, null::timestamptz,
            ocupacion_actual(), v_sim;
        return;
    end if;

    return query select * from _registrar_movimiento(v_id, null, 'FACIAL', v_sim);
end $$;

-- 8. Botones Entrada / Salida de Recepción ------------------------------
drop function if exists public.registrar_movimiento_manual(bigint, boolean);
create function public.registrar_movimiento_manual(p_cliente_id bigint, p_es_entrada boolean)
returns table (
    r_resultado text, r_cliente_id bigint, r_nombre text, r_membresia text,
    r_activa boolean, r_es_entrada boolean, r_fecha_hora timestamptz,
    r_entrada_desde timestamptz, r_dentro_total integer, r_similitud real)
language sql security definer
set search_path = public
as $$
    select * from _registrar_movimiento(p_cliente_id, p_es_entrada, 'MANUAL', null)
$$;

-- 9. Registro del rostro (Recepción) ------------------------------------
create or replace function public.registrar_rostro_cliente(p_cliente_id bigint, p_embedding real[])
returns void
language plpgsql security definer
set search_path = public
as $$
declare
    v_otro   bigint;
    v_sim    real;
    v_nombre text;
begin
    if p_embedding is null or array_length(p_embedding, 1) <> 192 then
        raise exception 'Vector facial inválido';
    end if;

    if not exists (select 1 from clientes where id = p_cliente_id) then
        raise exception 'Cliente no encontrado';
    end if;

    -- Evita que el mismo rostro quede registrado en dos clientes
    select m.cliente_id, m.sim into v_otro, v_sim
    from (
        select rc.cliente_id, _similitud(rc.embedding, p_embedding) as sim
        from rostros_clientes rc
        where rc.cliente_id <> p_cliente_id
    ) m
    order by m.sim desc
    limit 1;

    if v_otro is not null and v_sim >= _umbral_rostro() then
        select cl.nombre into v_nombre from clientes_lista cl where cl.id = v_otro;
        raise exception 'Este rostro ya está registrado para %', coalesce(v_nombre, 'otro cliente');
    end if;

    insert into rostros_clientes (cliente_id, embedding, actualizado)
    values (p_cliente_id, p_embedding, now())
    on conflict (cliente_id)
    do update set embedding = excluded.embedding, actualizado = now();
end $$;

-- 10. Clientes que ya tienen rostro -------------------------------------
create or replace function public.clientes_rostro_registrado()
returns table (cliente_id bigint)
language sql stable security definer
set search_path = public
as $$ select rc.cliente_id from rostros_clientes rc $$;

-- Permisos para la app
grant execute on function public.ocupacion_actual() to anon, authenticated;
grant execute on function public.acceso_por_rostro(real[]) to anon, authenticated;
grant execute on function public.registrar_movimiento_manual(bigint, boolean) to anon, authenticated;
grant execute on function public.registrar_rostro_cliente(bigint, real[]) to anon, authenticated;
grant execute on function public.clientes_rostro_registrado() to anon, authenticated;

notify pgrst, 'reload schema';

-- Comprobación: debe regresar un número (0 si nadie ha entrado hoy)
select ocupacion_actual() as personas_dentro;

-- =====================================================================
--  AXOLOTL FITNESS CLUB · Esquema completo (generado con sql/exportar_esquema.sql)
--  Exportado: 2026-11-10 18:24
--  Para una base vacía: SQL Editor → pegar → Run
-- =====================================================================
set check_function_bodies = off;

create sequence if not exists membresia_seq;

create table if not exists asistencias (
  id bigint generated always as identity not null,
  cliente_id bigint not null,
  fecha_hora_ingreso timestamp with time zone default now() not null,
  metodo text default 'QR'::text not null
);

create table if not exists clientes (
  id bigint generated always as identity not null,
  usuario_id bigint not null,
  codigo_qr text,
  numero_membresia text not null,
  dentro boolean default false not null,
  ultimo_movimiento timestamp with time zone,
  contacto_emergencia_nombre text,
  contacto_emergencia_telefono text,
  aviso_vencimiento_para date,
  aviso_eliminacion_en date
);

create table if not exists ejercicios_rutina (
  id bigint generated always as identity not null,
  rutina_id bigint not null,
  nombre_ejercicio text not null,
  series integer,
  repeticiones integer,
  descanso_seg integer
);

create table if not exists gastos (
  id bigint generated always as identity not null,
  concepto text not null,
  categoria text not null,
  monto numeric(10,2) not null,
  fecha date default CURRENT_DATE not null
);

create table if not exists instructores (
  id bigint generated always as identity not null,
  usuario_id bigint not null,
  especialidad text,
  horario text,
  costo_asesoria numeric(10,2) default 0,
  max_clientes integer default 10
);

create table if not exists membresias_cliente (
  id bigint generated always as identity not null,
  cliente_id bigint not null,
  tipo_membresia_id bigint not null,
  fecha_inicio date default CURRENT_DATE not null,
  fecha_fin date not null,
  estado text default 'ACTIVA'::text not null,
  cancelada_en timestamp with time zone,
  cancelada_por bigint,
  motivo_cancelacion text
);

create table if not exists pagos (
  id bigint generated always as identity not null,
  monto numeric(10,2) not null,
  metodo_pago text not null,
  membresia_cliente_id bigint,
  cliente_id bigint,
  registrado_por bigint,
  fecha_pago timestamp with time zone default now() not null,
  tipo text default 'PAGO'::text not null,
  estado text default 'ACEPTADO'::text not null,
  confirmado_por bigint,
  confirmado_en timestamp with time zone
);

create table if not exists registros_acceso (
  id bigint generated always as identity not null,
  cliente_id bigint not null,
  es_entrada boolean not null,
  fecha_hora timestamp with time zone default now() not null,
  metodo text default 'FACIAL'::text not null
);

create table if not exists roles (
  id bigint generated always as identity not null,
  nombre text not null
);

create table if not exists rostros_clientes (
  cliente_id bigint not null,
  embedding real[] not null,
  actualizado timestamp with time zone default now() not null
);

create table if not exists rutinas (
  id bigint generated always as identity not null,
  instructor_id bigint not null,
  cliente_id bigint not null,
  nombre_rutina text not null,
  fecha_asignacion date default CURRENT_DATE not null
);

create table if not exists servicios (
  id bigint generated always as identity not null,
  nombre text not null,
  duracion text not null,
  precio numeric(10,2) not null
);

create table if not exists solicitudes_asesoria (
  id bigint generated always as identity not null,
  cliente_id bigint not null,
  instructor_id bigint not null,
  costo numeric(10,2) not null,
  fecha_solicitud date default CURRENT_DATE not null,
  estado text default 'PENDIENTE'::text not null,
  dias text default ''::text not null,
  objetivo text default ''::text not null,
  cancelada_en timestamp with time zone,
  cancelada_por bigint,
  motivo_cancelacion text
);

create table if not exists tipos_membresia (
  id bigint generated always as identity not null,
  nombre text not null,
  precio numeric(10,2) not null,
  duracion_dias integer not null
);

create table if not exists usuario_roles (
  usuario_id bigint not null,
  rol_id bigint not null
);

create table if not exists usuarios (
  id bigint generated always as identity not null,
  nombre text not null,
  contrasena text not null,
  gmail text not null,
  telefono text default ''::text,
  estado estado_t default 'ACTIVO'::estado_t not null,
  fecha_registro date default CURRENT_DATE not null,
  intentos_fallidos integer default 0 not null,
  bloqueado_hasta timestamp with time zone,
  ultimo_acceso timestamp with time zone
);

alter table asistencias add constraint asistencias_pkey PRIMARY KEY (id);

alter table clientes add constraint clientes_codigo_qr_key UNIQUE (codigo_qr);

alter table clientes add constraint clientes_numero_membresia_formato CHECK ((numero_membresia ~ '^AX[0-9]{6}$'::text));

alter table clientes add constraint clientes_numero_membresia_key UNIQUE (numero_membresia);

alter table clientes add constraint clientes_pkey PRIMARY KEY (id);

alter table clientes add constraint clientes_usuario_id_key UNIQUE (usuario_id);

alter table ejercicios_rutina add constraint ejercicios_rutina_pkey PRIMARY KEY (id);

alter table gastos add constraint gastos_pkey PRIMARY KEY (id);

alter table instructores add constraint instructores_pkey PRIMARY KEY (id);

alter table instructores add constraint instructores_usuario_id_key UNIQUE (usuario_id);

alter table membresias_cliente add constraint membresias_cliente_pkey PRIMARY KEY (id);

alter table membresias_cliente add constraint membresias_estado_chk CHECK ((estado = ANY (ARRAY['ACTIVA'::text, 'PENDIENTE'::text, 'RECHAZADA'::text, 'CANCELADA'::text, 'VENCIDA'::text]))) NOT VALID;

alter table pagos add constraint pagos_estado_chk CHECK ((estado = ANY (ARRAY['PENDIENTE'::text, 'ACEPTADO'::text, 'RECHAZADO'::text]))) NOT VALID;

alter table pagos add constraint pagos_pkey PRIMARY KEY (id);

alter table pagos add constraint pagos_tipo_chk CHECK ((tipo = ANY (ARRAY['PAGO'::text, 'REEMBOLSO'::text]))) NOT VALID;

alter table registros_acceso add constraint registros_acceso_pkey PRIMARY KEY (id);

alter table roles add constraint roles_nombre_key UNIQUE (nombre);

alter table roles add constraint roles_pkey PRIMARY KEY (id);

alter table rostros_clientes add constraint rostros_clientes_embedding_check CHECK ((array_length(embedding, 1) = 192));

alter table rostros_clientes add constraint rostros_clientes_pkey PRIMARY KEY (cliente_id);

alter table rutinas add constraint rutinas_pkey PRIMARY KEY (id);

alter table servicios add constraint servicios_pkey PRIMARY KEY (id);

alter table solicitudes_asesoria add constraint solicitudes_asesoria_pkey PRIMARY KEY (id);

alter table solicitudes_asesoria add constraint solicitudes_estado_chk CHECK ((estado = ANY (ARRAY['PENDIENTE'::text, 'APROBADA'::text, 'RECHAZADA'::text, 'CANCELADA'::text]))) NOT VALID;

alter table tipos_membresia add constraint tipos_membresia_pkey PRIMARY KEY (id);

alter table usuario_roles add constraint usuario_roles_pkey PRIMARY KEY (usuario_id, rol_id);

alter table usuarios add constraint usuarios_gmail_key UNIQUE (gmail);

alter table usuarios add constraint usuarios_pkey PRIMARY KEY (id);

alter table asistencias add constraint asistencias_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE;

alter table clientes add constraint clientes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE;

alter table ejercicios_rutina add constraint ejercicios_rutina_rutina_id_fkey FOREIGN KEY (rutina_id) REFERENCES rutinas(id) ON DELETE CASCADE;

alter table instructores add constraint instructores_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE;

alter table membresias_cliente add constraint membresias_cliente_cancelada_por_fkey FOREIGN KEY (cancelada_por) REFERENCES usuarios(id) ON DELETE SET NULL;

alter table membresias_cliente add constraint membresias_cliente_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE;

alter table membresias_cliente add constraint membresias_cliente_tipo_membresia_id_fkey FOREIGN KEY (tipo_membresia_id) REFERENCES tipos_membresia(id);

alter table pagos add constraint pagos_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE;

alter table pagos add constraint pagos_confirmado_por_fkey FOREIGN KEY (confirmado_por) REFERENCES usuarios(id) ON DELETE SET NULL;

alter table pagos add constraint pagos_membresia_cliente_id_fkey FOREIGN KEY (membresia_cliente_id) REFERENCES membresias_cliente(id);

alter table pagos add constraint pagos_registrado_por_fkey FOREIGN KEY (registrado_por) REFERENCES usuarios(id);

alter table registros_acceso add constraint registros_acceso_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE;

alter table rostros_clientes add constraint rostros_clientes_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE;

alter table rutinas add constraint rutinas_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE;

alter table rutinas add constraint rutinas_instructor_id_fkey FOREIGN KEY (instructor_id) REFERENCES instructores(id);

alter table solicitudes_asesoria add constraint solicitudes_asesoria_cancelada_por_fkey FOREIGN KEY (cancelada_por) REFERENCES usuarios(id) ON DELETE SET NULL;

alter table solicitudes_asesoria add constraint solicitudes_asesoria_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE;

alter table solicitudes_asesoria add constraint solicitudes_asesoria_instructor_id_fkey FOREIGN KEY (instructor_id) REFERENCES instructores(id) ON DELETE CASCADE;

alter table usuario_roles add constraint usuario_roles_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE CASCADE;

alter table usuario_roles add constraint usuario_roles_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE;

CREATE INDEX registros_acceso_cliente_idx ON public.registros_acceso USING btree (cliente_id, fecha_hora DESC);

CREATE INDEX registros_acceso_fecha_idx ON public.registros_acceso USING btree (fecha_hora);

CREATE OR REPLACE FUNCTION public._activar_membresia(p_mem bigint)
 RETURNS date
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_cli bigint; v_dur int; v_ultima date; v_ini date; v_fin date;
  v_hoy date := _hoy_fecha_mx();
begin
  select mc.cliente_id, t.duracion_dias into v_cli, v_dur
    from membresias_cliente mc join tipos_membresia t on t.id = mc.tipo_membresia_id
   where mc.id = p_mem;

  select max(fecha_fin) into v_ultima from membresias_cliente
   where cliente_id = v_cli and estado = 'ACTIVA' and id <> p_mem;

  if v_ultima is not null and v_ultima >= v_hoy then
    v_ini := v_ultima + 1;
    v_fin := v_ultima + v_dur;
  else
    v_ini := v_hoy;
    v_fin := v_hoy + v_dur;
  end if;

  update membresias_cliente set estado = 'ACTIVA', fecha_inicio = v_ini, fecha_fin = v_fin
   where id = p_mem;
  return v_fin;
end $function$
;

CREATE OR REPLACE FUNCTION public._es_solo_cliente(p_usuario bigint)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  select _tiene_rol(p_usuario, array['CLIENTE'])
     and not _tiene_rol(p_usuario, array['RECEPCION', 'INSTRUCTOR', 'ENCARGADO'])
$function$
;

CREATE OR REPLACE FUNCTION public._hoy_fecha_mx()
 RETURNS date
 LANGUAGE sql
 STABLE
AS $function$ select (now() at time zone 'America/Mexico_City')::date $function$
;

CREATE OR REPLACE FUNCTION public._hoy_mx()
 RETURNS timestamp with time zone
 LANGUAGE sql
 STABLE
AS $function$
    select date_trunc('day', now() at time zone 'America/Mexico_City')
           at time zone 'America/Mexico_City'
$function$
;

CREATE OR REPLACE FUNCTION public._id_usuario(p_gmail text)
 RETURNS bigint
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$ select id from usuarios where lower(gmail) = lower(trim(coalesce(p_gmail, ''))) $function$
;

CREATE OR REPLACE FUNCTION public._monto_neto(p pagos)
 RETURNS numeric
 LANGUAGE sql
 IMMUTABLE
AS $function$ select case when p.estado <> 'ACEPTADO' then 0
                  when p.tipo = 'REEMBOLSO' then -p.monto else p.monto end $function$
;

CREATE OR REPLACE FUNCTION public._nueva_membresia(p_cli bigint, p_plan text, p_metodo text, p_quien bigint)
 RETURNS date
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare v_tipo tipos_membresia%rowtype; v_mem bigint; v_hoy date := _hoy_fecha_mx();
begin
  if p_metodo not in ('Efectivo', 'Tarjeta', 'Transferencia') then
    raise exception 'Método de pago no válido';
  end if;
  select * into v_tipo from tipos_membresia where nombre = p_plan;
  if not found then raise exception 'Plan no válido'; end if;

  insert into membresias_cliente (cliente_id, tipo_membresia_id, fecha_inicio, fecha_fin, estado)
  values (p_cli, v_tipo.id, v_hoy, v_hoy, 'PENDIENTE')
  returning id into v_mem;

  if p_metodo = 'Transferencia' then
    insert into pagos (monto, metodo_pago, membresia_cliente_id, cliente_id, registrado_por, estado)
    values (v_tipo.precio, p_metodo, v_mem, p_cli, p_quien, 'PENDIENTE');
    return null;
  end if;

  insert into pagos (monto, metodo_pago, membresia_cliente_id, cliente_id, registrado_por,
                     estado, confirmado_por, confirmado_en)
  values (v_tipo.precio, p_metodo, v_mem, p_cli, p_quien, 'ACEPTADO', p_quien, now());
  return _activar_membresia(v_mem);
end $function$
;

CREATE OR REPLACE FUNCTION public._registrar_movimiento(p_cliente_id bigint, p_es_entrada boolean, p_metodo text, p_similitud real)
 RETURNS TABLE(r_resultado text, r_cliente_id bigint, r_nombre text, r_membresia text, r_activa boolean, r_es_entrada boolean, r_fecha_hora timestamp with time zone, r_entrada_desde timestamp with time zone, r_dentro_total integer, r_similitud real)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
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
end $function$
;

CREATE OR REPLACE FUNCTION public._similitud(a real[], b real[])
 RETURNS real
 LANGUAGE sql
 IMMUTABLE
AS $function$ select coalesce(sum(x * y), 0)::real from unnest(a, b) as t(x, y) $function$
;

CREATE OR REPLACE FUNCTION public._tiene_rol(p_usuario bigint, p_roles text[])
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  select exists (select 1 from usuario_roles ur join roles r on r.id = ur.rol_id
                  where ur.usuario_id = p_usuario and r.nombre = any (p_roles))
$function$
;

CREATE OR REPLACE FUNCTION public._ultimo_uso(p_cliente_id bigint)
 RETURNS date
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  select greatest(
           u.fecha_registro,
           (u.ultimo_acceso at time zone 'America/Mexico_City')::date,
           (c.ultimo_movimiento at time zone 'America/Mexico_City')::date,
           (select max(mc.fecha_fin) from membresias_cliente mc
             where mc.cliente_id = c.id and mc.estado = 'ACTIVA'))
    from clientes c join usuarios u on u.id = c.usuario_id
   where c.id = p_cliente_id
$function$
;

CREATE OR REPLACE FUNCTION public._umbral_rostro()
 RETURNS real
 LANGUAGE sql
 IMMUTABLE
AS $function$ select 0.70::real $function$
;

CREATE OR REPLACE FUNCTION public._validar_contacto(p_nombre text, p_telefono text)
 RETURNS void
 LANGUAGE plpgsql
 IMMUTABLE
AS $function$
begin
  if coalesce(trim(p_nombre), '') = '' and coalesce(trim(p_telefono), '') = '' then return; end if;
  if length(trim(coalesce(p_nombre, ''))) < 3 then
    raise exception 'Escribe el nombre del contacto de emergencia';
  end if;
  if coalesce(p_telefono, '') !~ '^[0-9]{10}$' then
    raise exception 'El teléfono del contacto de emergencia debe tener 10 dígitos';
  end if;
end $function$
;

CREATE OR REPLACE FUNCTION public._verificar_contrasena(p_id bigint, p_password text)
 RETURNS boolean
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare u usuarios%rowtype;
begin
  select * into u from usuarios where id = p_id for update;
  if not found then return false; end if;

  if u.bloqueado_hasta is not null and u.bloqueado_hasta > now() then
    raise exception 'Cuenta bloqueada temporalmente. Intenta de nuevo en % min',
      greatest(1, ceil(extract(epoch from u.bloqueado_hasta - now()) / 60))::int;
  end if;

  -- Ya pasó el bloqueo: se empieza a contar de nuevo
  if u.bloqueado_hasta is not null then u.intentos_fallidos := 0; end if;

  if u.contrasena = crypt(coalesce(p_password, ''), u.contrasena) then
    update usuarios set intentos_fallidos = 0, bloqueado_hasta = null where id = p_id;
    return true;
  end if;

  update usuarios
     set intentos_fallidos = u.intentos_fallidos + 1,
         bloqueado_hasta = case when u.intentos_fallidos + 1 >= 5
                                then now() + interval '15 minutes' end
   where id = p_id;
  return false;
end $function$
;

CREATE OR REPLACE FUNCTION public.acceso_por_rostro(p_embedding real[])
 RETURNS TABLE(r_resultado text, r_cliente_id bigint, r_nombre text, r_membresia text, r_activa boolean, r_es_entrada boolean, r_fecha_hora timestamp with time zone, r_entrada_desde timestamp with time zone, r_dentro_total integer, r_similitud real)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
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
end $function$
;

CREATE OR REPLACE FUNCTION public.asignar_numero_membresia()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
begin
  new.numero_membresia := 'AX' || lpad(nextval('membresia_seq')::text, 6, '0');
  return new;
end $function$
;

CREATE OR REPLACE FUNCTION public.avisos_pendientes()
 RETURNS TABLE(r_tipo text, r_cliente_id bigint, r_nombre text, r_gmail text, r_plan text, r_fecha date)
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  -- Vencimiento: 3 días antes (planes de 7 días o más), una sola vez por vencimiento
  select 'VENCIMIENTO'::text, c.id, u.nombre, u.gmail, t.nombre, mc.fecha_fin
    from clientes c
    join usuarios u on u.id = c.usuario_id
    join lateral (select * from membresias_cliente x
                   where x.cliente_id = c.id and x.estado = 'ACTIVA'
                   order by x.fecha_fin desc limit 1) mc on true
    join tipos_membresia t on t.id = mc.tipo_membresia_id
   where u.estado = 'ACTIVO'
     and t.duracion_dias >= 7
     and mc.fecha_fin between _hoy_fecha_mx() and _hoy_fecha_mx() + 3
     and c.aviso_vencimiento_para is distinct from mc.fecha_fin
  union all
  -- Eliminación: 30 días antes de cumplir 365 días sin uso
  select 'ELIMINACION'::text, c.id, u.nombre, u.gmail, null::text,
         greatest(_ultimo_uso(c.id) + 365, _hoy_fecha_mx() + 30)
    from clientes c
    join usuarios u on u.id = c.usuario_id
   where _es_solo_cliente(u.id)
     and _ultimo_uso(c.id) <= _hoy_fecha_mx() - 335
     and (c.aviso_eliminacion_en is null or c.aviso_eliminacion_en < _ultimo_uso(c.id))
$function$
;

CREATE OR REPLACE FUNCTION public.bloquear_cambio_numero()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
begin
  if new.numero_membresia is distinct from old.numero_membresia then
    raise exception 'El número de membresía no se puede modificar';
  end if;
  return new;
end $function$
;

CREATE OR REPLACE FUNCTION public.cambiar_contrasena(p_gmail text, p_actual text, p_nueva text)
 RETURNS text
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare v_id bigint;
begin
  select id into v_id from usuarios
   where lower(gmail) = lower(trim(p_gmail)) and estado = 'ACTIVO';
  if v_id is null or not _verificar_contrasena(v_id, p_actual) then
    return 'Correo o contraseña actual incorrectos';
  end if;
  if p_nueva = p_actual then
    return 'La nueva contraseña debe ser diferente a la actual';
  end if;
  if p_nueva = 'Axolotl2026' then
    return 'Elige una contraseña distinta a la temporal';
  end if;

  perform validar_password(p_nueva);
  update usuarios set contrasena = p_nueva, intentos_fallidos = 0, bloqueado_hasta = null
   where id = v_id;
  return 'OK';
end $function$
;

CREATE OR REPLACE FUNCTION public.cancelar_asesoria(p_actor_gmail text, p_id bigint, p_motivo text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare v_actor bigint := _id_usuario(p_actor_gmail);
begin
  if not _tiene_rol(v_actor, array['RECEPCION', 'ENCARGADO']) then
    raise exception 'Solo Recepción o el Encargado pueden cancelar asesorías';
  end if;
  if length(trim(coalesce(p_motivo, ''))) < 3 then
    raise exception 'Escribe el motivo de la cancelación';
  end if;
  update solicitudes_asesoria
     set estado = 'CANCELADA', cancelada_en = now(), cancelada_por = v_actor,
         motivo_cancelacion = trim(p_motivo)
   where id = p_id and estado in ('PENDIENTE', 'APROBADA');
  if not found then
    raise exception 'La asesoría ya fue cancelada o resuelta';
  end if;
end $function$
;

CREATE OR REPLACE FUNCTION public.cancelar_membresia(p_actor_gmail text, p_password text, p_cliente_id bigint, p_motivo text, p_reembolso numeric DEFAULT 0)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare
  v_actor bigint; v_ids bigint[]; v_pagado numeric; v_metodo text;
  v_reemb numeric := coalesce(p_reembolso, 0);
begin
  v_actor := validar_encargado(p_actor_gmail, p_password);   -- lanza error si no es Encargado

  if length(trim(coalesce(p_motivo, ''))) < 3 then
    raise exception 'Escribe el motivo de la cancelación';
  end if;

  select array_agg(id) into v_ids from membresias_cliente
   where cliente_id = p_cliente_id and estado = 'ACTIVA' and fecha_fin >= _hoy_fecha_mx();
  if v_ids is null then
    raise exception 'El cliente no tiene una membresía vigente';
  end if;

  select coalesce(sum(_monto_neto(p)), 0) into v_pagado
    from pagos p where p.membresia_cliente_id = any (v_ids);
  if v_reemb < 0 or v_reemb > v_pagado then
    raise exception 'El reembolso debe estar entre 0 y %', v_pagado;
  end if;

  update membresias_cliente
     set estado = 'CANCELADA', cancelada_en = now(), cancelada_por = v_actor,
         motivo_cancelacion = trim(p_motivo)
   where id = any (v_ids);

  if v_reemb > 0 then
    select metodo_pago into v_metodo from pagos
     where membresia_cliente_id = any (v_ids) and tipo = 'PAGO' and estado = 'ACEPTADO'
     order by fecha_pago desc limit 1;
    insert into pagos (monto, metodo_pago, membresia_cliente_id, cliente_id, registrado_por,
                       tipo, estado, confirmado_por, confirmado_en)
    values (v_reemb, coalesce(v_metodo, 'Efectivo'), v_ids[1], p_cliente_id, v_actor,
            'REEMBOLSO', 'ACEPTADO', v_actor, now());
  end if;
end $function$
;

CREATE OR REPLACE FUNCTION public.cancelar_solicitud(p_gmail text, p_instructor_id bigint)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
  update solicitudes_asesoria s
     set estado = 'CANCELADA', cancelada_en = now(), cancelada_por = u.id,
         motivo_cancelacion = 'Cancelada por el cliente'
    from clientes c, usuarios u
   where s.cliente_id = c.id and u.id = c.usuario_id
     and lower(u.gmail) = lower(p_gmail)
     and s.instructor_id = p_instructor_id
     and s.estado = 'PENDIENTE';
end $function$
;

CREATE OR REPLACE FUNCTION public.clientes_dentro()
 RETURNS TABLE(cliente_id bigint, desde timestamp with time zone)
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
    select ultimo.cliente_id, ultimo.fecha_hora
    from (
        select distinct on (ra.cliente_id) ra.cliente_id, ra.es_entrada, ra.fecha_hora
        from registros_acceso ra
        where ra.fecha_hora >= _hoy_mx()
        order by ra.cliente_id, ra.fecha_hora desc, ra.id desc
    ) ultimo
    where ultimo.es_entrada
$function$
;

CREATE OR REPLACE FUNCTION public.clientes_rostro_registrado()
 RETURNS TABLE(cliente_id bigint)
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$ select rc.cliente_id from rostros_clientes rc $function$
;

CREATE OR REPLACE FUNCTION public.confirmar_pago(p_actor_gmail text, p_pago_id bigint, p_aceptar boolean)
 RETURNS date
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare v_actor bigint; p pagos%rowtype;
begin
  v_actor := _id_usuario(p_actor_gmail);
  select * into p from pagos where id = p_pago_id for update;
  if not found or p.estado <> 'PENDIENTE' then
    raise exception 'El pago ya fue resuelto o no existe';
  end if;
  if not _tiene_rol(v_actor, array['ENCARGADO'])
     and not (p.metodo_pago <> 'Transferencia' and _tiene_rol(v_actor, array['RECEPCION'])) then
    raise exception 'Solo el Encargado puede confirmar transferencias';
  end if;

  if not p_aceptar then
    update pagos set estado = 'RECHAZADO', confirmado_por = v_actor, confirmado_en = now() where id = p.id;
    update membresias_cliente set estado = 'RECHAZADA' where id = p.membresia_cliente_id;
    return null;
  end if;

  -- El ingreso cuenta el día en que se confirma
  update pagos set estado = 'ACEPTADO', confirmado_por = v_actor, confirmado_en = now(), fecha_pago = now()
   where id = p.id;
  return _activar_membresia(p.membresia_cliente_id);
end $function$
;

CREATE OR REPLACE FUNCTION public.contacto_emergencia_cliente(p_actor_gmail text, p_cliente_id bigint)
 RETURNS TABLE(r_nombre text, r_telefono text)
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['RECEPCION', 'ENCARGADO']) then
    raise exception 'Solo Recepción y el Encargado pueden ver el contacto de emergencia';
  end if;
  return query
    select coalesce(c.contacto_emergencia_nombre, '')::text, coalesce(c.contacto_emergencia_telefono, '')::text
      from clientes c where c.id = p_cliente_id;
end $function$
;

CREATE OR REPLACE FUNCTION public.crear_cliente(p_nombre text, p_telefono text, p_gmail text, p_plan text, p_registrado_por text, p_contacto_nombre text DEFAULT NULL::text, p_contacto_telefono text DEFAULT NULL::text, p_metodo text DEFAULT 'Efectivo'::text)
 RETURNS TABLE(r_id bigint, r_numero text, r_qr text, r_vence date)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare v_user bigint; v_cli bigint; v_fin date;
begin
  if exists (select 1 from usuarios where lower(gmail) = lower(trim(p_gmail))) then
    raise exception 'Ese correo ya está registrado';
  end if;
  perform _validar_contacto(p_contacto_nombre, p_contacto_telefono);

  insert into usuarios (nombre, contrasena, gmail, telefono)
  values (trim(p_nombre), 'Axolotl2026', lower(trim(p_gmail)), coalesce(p_telefono, ''))
  returning id into v_user;

  insert into usuario_roles (usuario_id, rol_id)
  select v_user, id from roles where nombre = 'CLIENTE';

  insert into clientes (usuario_id, contacto_emergencia_nombre, contacto_emergencia_telefono)
  values (v_user, nullif(trim(coalesce(p_contacto_nombre, '')), ''),
                  nullif(trim(coalesce(p_contacto_telefono, '')), ''))
  returning id into v_cli;

  if coalesce(p_plan, '') <> '' then
    v_fin := _nueva_membresia(v_cli, p_plan, coalesce(p_metodo, 'Efectivo'), _id_usuario(p_registrado_por));
  end if;

  return query select c.id, c.numero_membresia, c.codigo_qr, v_fin from clientes c where c.id = v_cli;
end $function$
;

CREATE OR REPLACE FUNCTION public.crear_personal(p_nombre text, p_telefono text, p_gmail text, p_rol text, p_costo numeric DEFAULT NULL::numeric, p_max_clientes integer DEFAULT NULL::integer)
 RETURNS bigint
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare v_id bigint;
begin
  if p_rol not in ('RECEPCION', 'INSTRUCTOR', 'ENCARGADO') then
    raise exception 'Rol no permitido';
  end if;
  if exists (select 1 from usuarios where lower(gmail) = lower(trim(p_gmail))) then
    raise exception 'Ese correo ya está registrado';
  end if;

  insert into usuarios (nombre, contrasena, gmail, telefono)
  values (trim(p_nombre), 'Axolotl2026', lower(trim(p_gmail)), coalesce(p_telefono, ''))
  returning id into v_id;

  insert into usuario_roles (usuario_id, rol_id)
  select v_id, id from roles where nombre = p_rol;

  if p_rol = 'INSTRUCTOR' then
    insert into instructores (usuario_id, costo_asesoria, max_clientes)
    values (v_id, coalesce(p_costo, 0), coalesce(p_max_clientes, 10));
  end if;
  return v_id;
end $function$
;

CREATE OR REPLACE FUNCTION public.detalle_personal(p_id bigint)
 RETURNS TABLE(r_telefono text, r_costo numeric, r_max_clientes integer)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
begin
  return query
  select coalesce(u.telefono, '')::text, i.costo_asesoria::numeric, i.max_clientes::int
    from usuarios u
    left join instructores i on i.usuario_id = u.id
   where u.id = p_id;
end $function$
;

CREATE OR REPLACE FUNCTION public.editar_cliente(p_id bigint, p_nombre text, p_telefono text, p_gmail text, p_estado text, p_contacto_nombre text DEFAULT NULL::text, p_contacto_telefono text DEFAULT NULL::text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare v_user bigint;
begin
  if p_estado not in ('ACTIVO', 'INACTIVO') then
    raise exception 'Estado no permitido';
  end if;
  select usuario_id into v_user from clientes where id = p_id;
  if v_user is null then raise exception 'Cliente no encontrado'; end if;
  if exists (select 1 from usuarios where lower(gmail) = lower(trim(p_gmail)) and id <> v_user) then
    raise exception 'Ese correo ya está registrado';
  end if;

  update usuarios
     set nombre = trim(p_nombre), telefono = coalesce(p_telefono, ''),
         gmail = lower(trim(p_gmail)), estado = p_estado::estado_t
   where id = v_user;

  if p_contacto_nombre is not null or p_contacto_telefono is not null then
    perform _validar_contacto(p_contacto_nombre, p_contacto_telefono);
    update clientes
       set contacto_emergencia_nombre = nullif(trim(coalesce(p_contacto_nombre, '')), ''),
           contacto_emergencia_telefono = nullif(trim(coalesce(p_contacto_telefono, '')), '')
     where id = p_id;
  end if;
end $function$
;

CREATE OR REPLACE FUNCTION public.editar_mi_perfil(p_gmail text, p_nombre text, p_telefono text, p_nuevo_gmail text)
 RETURNS text
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare v_id bigint; v_new text := lower(trim(p_nuevo_gmail));
begin
  select id into v_id from usuarios where lower(gmail) = lower(trim(p_gmail));
  if v_id is null then raise exception 'Usuario no encontrado'; end if;
  if v_new = '' or position('@' in v_new) = 0 then raise exception 'Correo no válido'; end if;
  if exists (select 1 from usuarios where lower(gmail) = v_new and id <> v_id) then
    raise exception 'Ese correo ya está registrado';
  end if;
  update usuarios
     set nombre = trim(p_nombre), telefono = coalesce(p_telefono, ''), gmail = v_new
   where id = v_id;
  return v_new;
end $function$
;

CREATE OR REPLACE FUNCTION public.editar_perfil_instructor(p_gmail_actual text, p_nombre text, p_telefono text, p_gmail text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare v_id bigint;
begin
  select u.id into v_id
    from usuarios u
    join instructores i on i.usuario_id = u.id
   where lower(u.gmail) = lower(trim(p_gmail_actual));

  if v_id is null then raise exception 'Instructor no encontrado'; end if;
  if exists (select 1 from usuarios where lower(gmail) = lower(trim(p_gmail)) and id <> v_id) then
    raise exception 'Ese correo ya está registrado';
  end if;

  update usuarios
     set nombre = trim(p_nombre), telefono = coalesce(p_telefono, ''),
         gmail = lower(trim(p_gmail))
   where id = v_id;
end $function$
;

CREATE OR REPLACE FUNCTION public.editar_personal(p_actor_gmail text, p_id bigint, p_nombre text, p_gmail text, p_rol text, p_estado text, p_telefono text DEFAULT NULL::text, p_costo numeric DEFAULT NULL::numeric, p_max_clientes integer DEFAULT NULL::integer)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare v_actor bigint;
begin
  select id into v_actor from usuarios where lower(gmail) = lower(p_actor_gmail);

  if p_rol not in ('RECEPCION', 'INSTRUCTOR', 'ENCARGADO') then
    raise exception 'Rol no permitido';
  end if;
  if p_estado not in ('ACTIVO', 'INACTIVO') then
    raise exception 'Estado no permitido';
  end if;
  if p_id = v_actor and (p_estado = 'INACTIVO' or p_rol <> 'ENCARGADO') then
    raise exception 'No puedes quitarte tu propio acceso de Encargado';
  end if;
  if not exists (
    select 1 from usuario_roles ur join roles r on r.id = ur.rol_id
    where ur.usuario_id = p_id and r.nombre in ('RECEPCION', 'INSTRUCTOR', 'ENCARGADO')
  ) then
    raise exception 'Solo se puede editar personal';
  end if;
  if exists (select 1 from usuarios where lower(gmail) = lower(trim(p_gmail)) and id <> p_id) then
    raise exception 'Ese correo ya está registrado';
  end if;
  if p_costo is not null and p_costo < 0 then
    raise exception 'El costo no puede ser negativo';
  end if;
  if p_max_clientes is not null and p_max_clientes < 1 then
    raise exception 'La capacidad debe ser al menos 1';
  end if;

  update usuarios
     set nombre = trim(p_nombre), gmail = lower(trim(p_gmail)), estado = p_estado::estado_t,
         telefono = coalesce(p_telefono, telefono)
   where id = p_id;

  delete from usuario_roles where usuario_id = p_id;
  insert into usuario_roles (usuario_id, rol_id)
  select p_id, id from roles where nombre = p_rol;

  if p_rol = 'INSTRUCTOR' then
    if not exists (select 1 from instructores where usuario_id = p_id) then
      insert into instructores (usuario_id) values (p_id);
    end if;
    update instructores
       set costo_asesoria = coalesce(p_costo, costo_asesoria),
           max_clientes = coalesce(p_max_clientes, max_clientes)
     where usuario_id = p_id;
  end if;
end $function$
;

CREATE OR REPLACE FUNCTION public.eliminar_clientes_inactivos()
 RETURNS integer
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare r record; n int := 0;
begin
  for r in
    select c.id as cli, c.usuario_id as usr
      from clientes c
     where _es_solo_cliente(c.usuario_id)
       and _ultimo_uso(c.id) <= _hoy_fecha_mx() - 365
       and c.aviso_eliminacion_en is not null
       and c.aviso_eliminacion_en >= _ultimo_uso(c.id)
       and c.aviso_eliminacion_en <= _hoy_fecha_mx() - 30
  loop
    update pagos set cliente_id = null, membresia_cliente_id = null where cliente_id = r.cli;
    update pagos set membresia_cliente_id = null
     where membresia_cliente_id in (select id from membresias_cliente where cliente_id = r.cli);
    delete from ejercicios_rutina where rutina_id in (select id from rutinas where cliente_id = r.cli);
    delete from rutinas where cliente_id = r.cli;
    delete from solicitudes_asesoria where cliente_id = r.cli;
    delete from asistencias where cliente_id = r.cli;
    delete from registros_acceso where cliente_id = r.cli;
    delete from rostros_clientes where cliente_id = r.cli;
    delete from membresias_cliente where cliente_id = r.cli;
    delete from clientes where id = r.cli;
    delete from usuario_roles where usuario_id = r.usr;
    delete from usuarios where id = r.usr;
    n := n + 1;
  end loop;
  return n;
end $function$
;

CREATE OR REPLACE FUNCTION public.eliminar_personal(p_actor_gmail text, p_id bigint)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare v_actor bigint;
begin
  select id into v_actor from usuarios where lower(gmail) = lower(p_actor_gmail);

  if p_id = v_actor then
    raise exception 'No puedes eliminar tu propia cuenta';
  end if;

  if not exists (
    select 1 from usuario_roles ur join roles r on r.id = ur.rol_id
    where ur.usuario_id = p_id and r.nombre in ('RECEPCION', 'INSTRUCTOR', 'ENCARGADO')
  ) then
    raise exception 'Solo se puede eliminar personal';
  end if;

  -- Los pagos que registró se conservan, pero sin responsable
  update pagos set registrado_por = null where registrado_por = p_id;

  delete from usuarios where id = p_id;   -- borra también su rol e instructor
exception
  when foreign_key_violation then
    raise exception 'No se puede eliminar: tiene rutinas o asesorías registradas';
end $function$
;

CREATE OR REPLACE FUNCTION public.es_alta_reciente(p_id bigint)
 RETURNS boolean
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  select exists (select 1 from usuarios where id = p_id and fecha_registro::date >= current_date - 1)
$function$
;

CREATE OR REPLACE FUNCTION public.generar_qr_cliente()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
begin
  if new.codigo_qr is null then
    new.codigo_qr := 'GYM-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 12));
  end if;
  return new;
end $function$
;

CREATE OR REPLACE FUNCTION public.guardar_asesoria(p_gmail text, p_id bigint, p_dias text, p_objetivo text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
  update solicitudes_asesoria s
     set dias = coalesce(p_dias, ''), objetivo = coalesce(p_objetivo, '')
   from instructores i, usuarios ui
   where s.id = p_id and s.estado = 'APROBADA'
     and i.id = s.instructor_id and ui.id = i.usuario_id
     and lower(ui.gmail) = lower(p_gmail);
  if not found then
    raise exception 'No puedes editar esta asesoría';
  end if;
end $function$
;

CREATE OR REPLACE FUNCTION public.hash_password_trigger()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
begin
  if new.contrasena !~ '^\$2[aby]\$' then
    if new.contrasena <> 'Axolotl2026' then
      perform validar_password(new.contrasena);
    end if;
    new.contrasena := crypt(new.contrasena, gen_salt('bf', 10));
  end if;
  return new;
end $function$
;

CREATE OR REPLACE FUNCTION public.login(p_gmail text, p_password text, p_rol text)
 RETURNS TABLE(id bigint, nombre text, gmail text, rol text, codigo_qr text)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare u usuarios%rowtype;
begin
  select * into u from usuarios x where lower(x.gmail) = lower(p_gmail);
  if not found or u.estado <> 'ACTIVO' then return; end if;
  if not _verificar_contrasena(u.id, p_password) then return; end if;

  update usuarios x set ultimo_acceso = now() where x.id = u.id;

  return query
    select u.id, u.nombre, u.gmail, r.nombre, c.codigo_qr
      from usuario_roles ur
      join roles r on r.id = ur.rol_id
      left join clientes c on c.usuario_id = u.id
     where ur.usuario_id = u.id and r.nombre = p_rol;
end $function$
;

CREATE OR REPLACE FUNCTION public.login_auto(p_gmail text, p_password text)
 RETURNS TABLE(id bigint, nombre text, gmail text, rol text, codigo_qr text)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare u usuarios%rowtype;
begin
  select * into u from usuarios x where lower(x.gmail) = lower(trim(p_gmail));
  if not found or u.estado <> 'ACTIVO' then return; end if;
  if not _verificar_contrasena(u.id, p_password) then return; end if;

  update usuarios x set ultimo_acceso = now() where x.id = u.id;

  return query
    select u.id, u.nombre, u.gmail, r.nombre, c.codigo_qr
      from usuario_roles ur
      join roles r on r.id = ur.rol_id
      left join clientes c on c.usuario_id = u.id
     where ur.usuario_id = u.id
     order by case r.nombre when 'ENCARGADO' then 1 when 'RECEPCION' then 2
                            when 'INSTRUCTOR' then 3 else 4 end
     limit 1;
end $function$
;

CREATE OR REPLACE FUNCTION public.marcar_aviso(p_cliente_id bigint, p_tipo text, p_fecha date)
 RETURNS void
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  update clientes
     set aviso_vencimiento_para = case when p_tipo = 'VENCIMIENTO' then p_fecha else aviso_vencimiento_para end,
         aviso_eliminacion_en   = case when p_tipo = 'ELIMINACION' then _hoy_fecha_mx() else aviso_eliminacion_en end
   where id = p_cliente_id
$function$
;

CREATE OR REPLACE FUNCTION public.mi_perfil(p_gmail text)
 RETURNS TABLE(r_nombre text, r_telefono text, r_gmail text, r_estado text, r_numero text, r_qr text, r_plan text, r_vence date, r_activa boolean, r_max_clientes integer, r_activos integer)
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  select u.nombre::text, coalesce(u.telefono, '')::text, u.gmail::text, u.estado::text,
         c.numero_membresia::text, c.codigo_qr::text, c.plan::text, c.fecha_fin::date,
         coalesce(c.activa, false),
         i.max_clientes::int, i.activos::int
  from usuarios u
  left join clientes_lista c on lower(c.gmail) = lower(u.gmail)
  left join instructores_lista i on lower(i.gmail) = lower(u.gmail)
  where lower(u.gmail) = lower(trim(p_gmail))
  limit 1
$function$
;

CREATE OR REPLACE FUNCTION public.mis_clientes_instructor(p_gmail text)
 RETURNS TABLE(r_id bigint, r_nombre text, r_dias text, r_objetivo text)
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  select s.id, uc.nombre, s.dias, s.objetivo
  from solicitudes_asesoria s
  join instructores i on i.id = s.instructor_id
  join usuarios ui on ui.id = i.usuario_id
  join clientes c on c.id = s.cliente_id
  join usuarios uc on uc.id = c.usuario_id
  where lower(ui.gmail) = lower(p_gmail) and s.estado = 'APROBADA'
  order by uc.nombre
$function$
;

CREATE OR REPLACE FUNCTION public.mis_solicitudes(p_gmail text)
 RETURNS TABLE(r_instructor_id bigint, r_estado text)
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  select s.instructor_id, s.estado
  from solicitudes_asesoria s
  join clientes c on c.id = s.cliente_id
  join usuarios u on u.id = c.usuario_id
  where lower(u.gmail) = lower(p_gmail)
    and s.estado in ('PENDIENTE', 'APROBADA')
$function$
;

CREATE OR REPLACE FUNCTION public.ocupacion_actual()
 RETURNS integer
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
    select count(*)::int
    from (
        select distinct on (ra.cliente_id) ra.es_entrada
        from registros_acceso ra
        where ra.fecha_hora >= _hoy_mx()
        order by ra.cliente_id, ra.fecha_hora desc, ra.id desc
    ) ultimo
    where ultimo.es_entrada
$function$
;

CREATE OR REPLACE FUNCTION public.perfil_instructor(p_gmail text)
 RETURNS TABLE(r_id bigint, r_nombre text, r_telefono text, r_gmail text, r_estado text, r_max_clientes integer, r_activos integer)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
begin
  return query
  select u.id, u.nombre::text, coalesce(u.telefono, '')::text, u.gmail::text,
         u.estado::text, coalesce(i.max_clientes, 10)::int,
         coalesce((select l.activos from instructores_lista l
                   where lower(l.gmail) = lower(u.gmail) limit 1), 0)::int
    from usuarios u
    join instructores i on i.usuario_id = u.id
   where lower(u.gmail) = lower(trim(p_gmail));
end $function$
;

CREATE OR REPLACE FUNCTION public.registrar_movimiento_manual(p_cliente_id bigint, p_es_entrada boolean)
 RETURNS TABLE(r_resultado text, r_cliente_id bigint, r_nombre text, r_membresia text, r_activa boolean, r_es_entrada boolean, r_fecha_hora timestamp with time zone, r_entrada_desde timestamp with time zone, r_dentro_total integer, r_similitud real)
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
    select * from _registrar_movimiento(p_cliente_id, p_es_entrada, 'MANUAL', null)
$function$
;

CREATE OR REPLACE FUNCTION public.registrar_rostro_cliente(p_cliente_id bigint, p_embedding real[])
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
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
end $function$
;

CREATE OR REPLACE FUNCTION public.renovar_membresia(p_cliente_id bigint, p_plan text, p_registrado_por text, p_metodo text DEFAULT 'Efectivo'::text)
 RETURNS date
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
begin
  if not exists (select 1 from clientes where id = p_cliente_id) then
    raise exception 'Cliente no encontrado';
  end if;
  if exists (select 1 from membresias_cliente where cliente_id = p_cliente_id and estado = 'PENDIENTE') then
    raise exception 'El cliente ya tiene un pago por transferencia pendiente de confirmar';
  end if;
  return _nueva_membresia(p_cliente_id, p_plan, coalesce(p_metodo, 'Efectivo'), _id_usuario(p_registrado_por));
end $function$
;

CREATE OR REPLACE FUNCTION public.resolver_solicitud(p_id bigint, p_estado text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare v_inst bigint; v_max int;
begin
  if p_estado not in ('APROBADA', 'RECHAZADA') then
    raise exception 'Estado no permitido';
  end if;

  select instructor_id into v_inst
  from solicitudes_asesoria where id = p_id and estado = 'PENDIENTE';
  if v_inst is null then
    raise exception 'La solicitud ya fue resuelta o no existe';
  end if;

  if p_estado = 'APROBADA' then
    select coalesce(max_clientes, 10) into v_max from instructores where id = v_inst;
    if (select count(*) from solicitudes_asesoria
        where instructor_id = v_inst and estado = 'APROBADA') >= v_max then
      raise exception 'El instructor no tiene cupo disponible';
    end if;
  end if;

  update solicitudes_asesoria set estado = p_estado where id = p_id;
end $function$
;

CREATE OR REPLACE FUNCTION public.solicitar_asesoria(p_gmail text, p_instructor_id bigint)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare v_cli bigint; v_costo numeric; v_max int;
begin
  select c.id into v_cli
  from clientes c join usuarios u on u.id = c.usuario_id
  where lower(u.gmail) = lower(p_gmail) and u.estado = 'ACTIVO';
  if v_cli is null then raise exception 'Solo los clientes pueden solicitar asesorías'; end if;

  select coalesce(costo_asesoria, 0), coalesce(max_clientes, 10) into v_costo, v_max
  from instructores where id = p_instructor_id;
  if not found then raise exception 'Instructor no encontrado'; end if;

  if exists (select 1 from solicitudes_asesoria
             where cliente_id = v_cli and instructor_id = p_instructor_id
               and estado in ('PENDIENTE', 'APROBADA')) then
    raise exception 'Ya tienes una solicitud con este instructor';
  end if;

  if (select count(*) from solicitudes_asesoria
      where instructor_id = p_instructor_id and estado = 'APROBADA') >= v_max then
    raise exception 'El instructor no tiene cupo disponible';
  end if;

  insert into solicitudes_asesoria (cliente_id, instructor_id, costo)
  values (v_cli, p_instructor_id, v_costo);
end $function$
;

CREATE OR REPLACE FUNCTION public.validar_encargado(p_gmail text, p_password text)
 RETURNS bigint
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare
  v_id bigint;
begin
  select u.id into v_id
  from usuarios u
  join usuario_roles ur on ur.usuario_id = u.id
  join roles r on r.id = ur.rol_id
  where lower(u.gmail) = lower(p_gmail)
    and u.estado = 'ACTIVO'
    and r.nombre = 'ENCARGADO'
    and u.contrasena = crypt(p_password, u.contrasena);

  if v_id is null then
    raise exception 'Tu contraseña de Encargado es incorrecta';
  end if;
  return v_id;
end $function$
;

CREATE OR REPLACE FUNCTION public.validar_password(p text)
 RETURNS void
 LANGUAGE plpgsql
AS $function$
begin
  if p is null or length(p) < 8 then
    raise exception 'La contraseña debe tener al menos 8 caracteres';
  end if;
  if p !~ '[A-Z]' then raise exception 'La contraseña debe incluir una mayúscula'; end if;
  if p !~ '[a-z]' then raise exception 'La contraseña debe incluir una minúscula'; end if;
  if p !~ '[0-9]' then raise exception 'La contraseña debe incluir un número'; end if;
  if p !~ '[^A-Za-z0-9]' then raise exception 'La contraseña debe incluir un símbolo'; end if;
end $function$
;

create or replace view ocupacion_por_hora as
 SELECT to_char(date_trunc('hour'::text, (fecha_hora_ingreso AT TIME ZONE 'America/Mexico_City'::text)), 'HH24:00'::text) AS hora,
    count(*) AS personas
   FROM asistencias
  WHERE (((fecha_hora_ingreso AT TIME ZONE 'America/Mexico_City'::text))::date = ((now() AT TIME ZONE 'America/Mexico_City'::text))::date)
  GROUP BY (to_char(date_trunc('hour'::text, (fecha_hora_ingreso AT TIME ZONE 'America/Mexico_City'::text)), 'HH24:00'::text))
  ORDER BY (to_char(date_trunc('hour'::text, (fecha_hora_ingreso AT TIME ZONE 'America/Mexico_City'::text)), 'HH24:00'::text));

create or replace view reporte_mensual as
 SELECT to_char(m, 'YYYY-MM'::text) AS periodo,
    COALESCE(( SELECT sum(_monto_neto(p.*)) AS sum
           FROM pagos p
          WHERE (date_trunc('month'::text, p.fecha_pago) = m.m)), (0)::numeric) AS servicios,
    COALESCE(( SELECT sum(s.costo) AS sum
           FROM solicitudes_asesoria s
          WHERE ((s.estado = 'APROBADA'::text) AND (date_trunc('month'::text, (s.fecha_solicitud)::timestamp with time zone) = m.m))), (0)::numeric) AS asesorias,
    COALESCE(( SELECT sum(g.monto) AS sum
           FROM gastos g
          WHERE (date_trunc('month'::text, (g.fecha)::timestamp with time zone) = m.m)), (0)::numeric) AS gastos
   FROM generate_series((date_trunc('month'::text, (CURRENT_DATE)::timestamp with time zone) - '5 mons'::interval), date_trunc('month'::text, (CURRENT_DATE)::timestamp with time zone), '1 mon'::interval) m(m);

create or replace view resumen_dashboard as
 SELECT ( SELECT count(*) AS count
           FROM (clientes c
             JOIN usuarios u ON ((u.id = c.usuario_id)))
          WHERE (u.estado = 'ACTIVO'::estado_t)) AS activos,
    ( SELECT count(*) AS count
           FROM (clientes c
             JOIN usuarios u ON ((u.id = c.usuario_id)))
          WHERE (u.estado = 'INACTIVO'::estado_t)) AS inactivos,
    COALESCE(( SELECT sum(_monto_neto(p.*)) AS sum
           FROM pagos p
          WHERE (date_trunc('month'::text, (p.fecha_pago AT TIME ZONE 'America/Mexico_City'::text)) = date_trunc('month'::text, (now() AT TIME ZONE 'America/Mexico_City'::text)))), (0)::numeric) AS ingresos_mes;

create or replace view personal_lista as
 SELECT u.id,
    u.nombre,
    u.gmail,
    (u.estado)::text AS estado,
    r.nombre AS rol
   FROM ((usuarios u
     JOIN usuario_roles ur ON ((ur.usuario_id = u.id)))
     JOIN roles r ON ((r.id = ur.rol_id)))
  WHERE (r.nombre = ANY (ARRAY['RECEPCION'::text, 'INSTRUCTOR'::text, 'ENCARGADO'::text]));

create or replace view clientes_lista as
 SELECT c.id,
    u.nombre,
    u.gmail,
    COALESCE(u.telefono, ''::text) AS telefono,
    u.fecha_registro,
    c.codigo_qr,
    m.plan,
    m.fecha_fin,
    COALESCE(m.activa, false) AS activa,
    c.numero_membresia,
    (u.estado)::text AS estado
   FROM ((clientes c
     JOIN usuarios u ON ((u.id = c.usuario_id)))
     LEFT JOIN LATERAL ( SELECT t.nombre AS plan,
            mc.fecha_fin,
            ((mc.estado = 'ACTIVA'::text) AND (mc.fecha_fin >= _hoy_fecha_mx())) AS activa
           FROM (membresias_cliente mc
             JOIN tipos_membresia t ON ((t.id = mc.tipo_membresia_id)))
          WHERE ((mc.cliente_id = c.id) AND (mc.estado <> ALL (ARRAY['PENDIENTE'::text, 'RECHAZADA'::text])))
          ORDER BY mc.fecha_fin DESC, mc.id DESC
         LIMIT 1) m ON (true));

create or replace view instructores_lista as
 SELECT i.id,
    u.nombre,
    u.gmail,
    COALESCE(i.costo_asesoria, (0)::numeric) AS costo,
    COALESCE(i.max_clientes, 10) AS max_clientes,
    (( SELECT count(*) AS count
           FROM solicitudes_asesoria s
          WHERE ((s.instructor_id = i.id) AND (s.estado = 'APROBADA'::text))))::integer AS activos
   FROM (instructores i
     JOIN usuarios u ON ((u.id = i.usuario_id)))
  WHERE (u.estado = 'ACTIVO'::estado_t);

create or replace view solicitudes_lista as
 SELECT s.id,
    s.estado,
    (s.costo)::double precision AS costo,
    s.fecha_solicitud,
    uc.nombre AS cliente,
    COALESCE(c.numero_membresia, ''::text) AS numero_membresia,
    ui.nombre AS instructor
   FROM ((((solicitudes_asesoria s
     JOIN clientes c ON ((c.id = s.cliente_id)))
     JOIN usuarios uc ON ((uc.id = c.usuario_id)))
     JOIN instructores i ON ((i.id = s.instructor_id)))
     JOIN usuarios ui ON ((ui.id = i.usuario_id)));

create or replace view clientes_por_mes as
 WITH h AS (
         SELECT ((now() AT TIME ZONE 'America/Mexico_City'::text))::date AS hoy
        ), meses AS (
         SELECT (m.m)::date AS inicio,
            LEAST((((m.m + '1 mon'::interval) - '1 day'::interval))::date, h.hoy) AS corte
           FROM h,
            LATERAL generate_series((date_trunc('month'::text, (h.hoy)::timestamp with time zone) - '11 mons'::interval), date_trunc('month'::text, (h.hoy)::timestamp with time zone), '1 mon'::interval) m(m)
        ), conteo AS (
         SELECT me_1.inicio,
            count(*) AS total,
            count(*) FILTER (WHERE (EXISTS ( SELECT 1
                   FROM (membresias_cliente mc
                     JOIN tipos_membresia t ON ((t.id = mc.tipo_membresia_id)))
                  WHERE ((mc.cliente_id = c.id) AND (mc.fecha_fin >= me_1.corte) AND ((mc.fecha_fin - t.duracion_dias) <= me_1.corte))))) AS activos
           FROM ((meses me_1
             JOIN clientes c ON (true))
             JOIN usuarios u ON (((u.id = c.usuario_id) AND (u.fecha_registro <= me_1.corte))))
          GROUP BY me_1.inicio
        )
 SELECT to_char((me.inicio)::timestamp with time zone, 'YYYY-MM'::text) AS periodo,
    (COALESCE(co.activos, (0)::bigint))::integer AS activos,
    (COALESCE((co.total - co.activos), (0)::bigint))::integer AS inactivos
   FROM (meses me
     LEFT JOIN conteo co ON ((co.inicio = me.inicio)))
  ORDER BY me.inicio;

create or replace view ingresos_semana as
 WITH h AS (
         SELECT _hoy_fecha_mx() AS hoy
        ), dias AS (
         SELECT (d_1.d)::date AS dia
           FROM h,
            LATERAL generate_series(date_trunc('week'::text, (h.hoy)::timestamp with time zone), (date_trunc('week'::text, (h.hoy)::timestamp with time zone) + '6 days'::interval), '1 day'::interval) d_1(d)
        )
 SELECT d.dia AS fecha,
    (EXTRACT(isodow FROM d.dia))::integer AS dia_semana,
    COALESCE(sum(_monto_neto(p.*)), (0)::numeric) AS total
   FROM (dias d
     LEFT JOIN pagos p ON ((((p.fecha_pago AT TIME ZONE 'America/Mexico_City'::text))::date = d.dia)))
  GROUP BY d.dia
  ORDER BY d.dia;

create or replace view pagos_pendientes as
 SELECT p.id,
    (p.monto)::double precision AS monto,
    p.metodo_pago,
    p.fecha_pago,
    c.id AS cliente_id,
    u.nombre AS cliente,
    c.numero_membresia,
    t.nombre AS plan
   FROM ((((pagos p
     JOIN clientes c ON ((c.id = p.cliente_id)))
     JOIN usuarios u ON ((u.id = c.usuario_id)))
     LEFT JOIN membresias_cliente mc ON ((mc.id = p.membresia_cliente_id)))
     LEFT JOIN tipos_membresia t ON ((t.id = mc.tipo_membresia_id)))
  WHERE ((p.estado = 'PENDIENTE'::text) AND (p.tipo = 'PAGO'::text));

create or replace view cancelaciones_lista as
 SELECT 'MEMBRESIA'::text AS tipo,
    mc.id,
    mc.cancelada_en,
    uc.nombre AS cliente,
    c.numero_membresia,
    t.nombre AS detalle,
    mc.motivo_cancelacion AS motivo,
    ua.nombre AS autorizo,
    (COALESCE(( SELECT sum(p.monto) AS sum
           FROM pagos p
          WHERE ((p.membresia_cliente_id = mc.id) AND (p.tipo = 'REEMBOLSO'::text))), (0)::numeric))::double precision AS reembolso
   FROM ((((membresias_cliente mc
     JOIN clientes c ON ((c.id = mc.cliente_id)))
     JOIN usuarios uc ON ((uc.id = c.usuario_id)))
     JOIN tipos_membresia t ON ((t.id = mc.tipo_membresia_id)))
     LEFT JOIN usuarios ua ON ((ua.id = mc.cancelada_por)))
  WHERE (mc.estado = 'CANCELADA'::text)
UNION ALL
 SELECT 'ASESORIA'::text AS tipo,
    s.id,
    s.cancelada_en,
    uc.nombre AS cliente,
    c.numero_membresia,
    ('Asesoría con '::text || ui.nombre) AS detalle,
    s.motivo_cancelacion AS motivo,
    ua.nombre AS autorizo,
    0 AS reembolso
   FROM (((((solicitudes_asesoria s
     JOIN clientes c ON ((c.id = s.cliente_id)))
     JOIN usuarios uc ON ((uc.id = c.usuario_id)))
     JOIN instructores i ON ((i.id = s.instructor_id)))
     JOIN usuarios ui ON ((ui.id = i.usuario_id)))
     LEFT JOIN usuarios ua ON ((ua.id = s.cancelada_por)))
  WHERE (s.estado = 'CANCELADA'::text);

CREATE TRIGGER clientes_generar_qr BEFORE INSERT ON public.clientes FOR EACH ROW EXECUTE FUNCTION generar_qr_cliente();

CREATE TRIGGER clientes_numero_fijo BEFORE UPDATE ON public.clientes FOR EACH ROW EXECUTE FUNCTION bloquear_cambio_numero();

CREATE TRIGGER clientes_numero_membresia BEFORE INSERT ON public.clientes FOR EACH ROW EXECUTE FUNCTION asignar_numero_membresia();

CREATE TRIGGER usuarios_hash_password BEFORE INSERT OR UPDATE OF contrasena ON public.usuarios FOR EACH ROW EXECUTE FUNCTION hash_password_trigger();

alter table asistencias enable row level security;

alter table clientes enable row level security;

alter table ejercicios_rutina enable row level security;

alter table instructores enable row level security;

alter table membresias_cliente enable row level security;

alter table pagos enable row level security;

alter table registros_acceso enable row level security;

alter table roles enable row level security;

alter table rostros_clientes enable row level security;

alter table rutinas enable row level security;

alter table solicitudes_asesoria enable row level security;

alter table usuario_roles enable row level security;

alter table usuarios enable row level security;

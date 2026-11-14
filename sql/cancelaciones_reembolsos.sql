-- =====================================================================
--  AXOLOTL FITNESS CLUB · Política de cancelación y reembolsos
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces,
--  también si ya se había corrido la versión anterior de este archivo)
--  Requiere haber corrido antes pendientes_noviembre.sql, comprobantes_pago.sql
--  y pagos_recepcion.sql
--
--  Reglas:
--   · Reembolso completo si se solicita dentro de las 48 h siguientes al pago y
--     el cliente aún no ha ingresado con esa membresía, o si hubo un error de cobro.
--   · En cualquier otro caso no hay reembolso: la membresía sigue activa hasta su
--     vencimiento y solo queda registrada la cancelación.
--   · El reembolso es por el mismo medio de pago, en máximo 10 días hábiles
--     contados desde la solicitud.
--
--  Flujo en dos pasos:
--   1. Recepción (o el Encargado) registra la SOLICITUD. La membresía sigue activa.
--   2. El Encargado, desde su sesión (Reportes), la AUTORIZA o la RECHAZA.
--      · Las 48 h se cuentan hasta la solicitud: si el Encargado tarda, el cliente
--        no pierde el reembolso.
--      · Si el cliente ingresa al gimnasio mientras la solicitud está pendiente,
--        ya no aplica el reembolso (regla INGRESO_TRAS_SOLICITUD).
--
--  Reportes: el reembolso queda PENDIENTE y no resta nada hasta que el Encargado
--  lo marca como entregado; ese día se descuenta en reporte_mensual,
--  resumen_dashboard e ingresos_semana (todas usan _monto_neto).
-- =====================================================================

alter table pagos add column if not exists fecha_limite date;                    -- solo reembolsos
alter table pagos add column if not exists reembolso_de bigint references pagos(id) on delete set null;
alter table membresias_cliente add column if not exists tipo_cancelacion text;   -- VOLUNTARIA | ERROR_COBRO

alter table membresias_cliente drop constraint if exists membresias_tipo_cancelacion_chk;
alter table membresias_cliente add constraint membresias_tipo_cancelacion_chk
  check (tipo_cancelacion is null or tipo_cancelacion in ('VOLUNTARIA', 'ERROR_COBRO')) not valid;

create table if not exists solicitudes_cancelacion (
  id             bigint generated always as identity primary key,
  cliente_id     bigint not null references clientes(id) on delete cascade,
  membresia_id   bigint not null references membresias_cliente(id) on delete cascade,
  tipo           text not null check (tipo in ('VOLUNTARIA', 'ERROR_COBRO')),
  motivo         text not null,
  estado         text not null default 'PENDIENTE'
                 check (estado in ('PENDIENTE', 'AUTORIZADA', 'RECHAZADA')),
  solicitada_por bigint references usuarios(id) on delete set null,
  solicitada_en  timestamptz not null default now(),
  resuelta_por   bigint references usuarios(id) on delete set null,
  resuelta_en    timestamptz,
  nota_encargado text,
  reembolso      numeric(10,2),      -- lo que se reembolsó al autorizar
  regla          text                -- regla que se aplicó al autorizar
);
-- Una sola solicitud pendiente por membresía
create unique index if not exists solicitudes_cancelacion_pendiente_uq
  on solicitudes_cancelacion (membresia_id) where estado = 'PENDIENTE';
alter table solicitudes_cancelacion enable row level security;

-- Suma días hábiles (lunes a viernes; no considera días festivos)
create or replace function _sumar_dias_habiles(p_desde date, p_dias int)
returns date language plpgsql immutable as $$
declare d date := p_desde; n int := 0;
begin
  while n < p_dias loop
    d := d + 1;
    if extract(isodow from d) < 6 then n := n + 1; end if;
  end loop;
  return d;
end $$;

-- ---------------------------------------------------------------------
-- Evaluación de UNA membresía. p_al = momento de la solicitud (las 48 h se
-- cuentan hasta ahí). Los ingresos se revisan hasta ahora mismo.
--   r_regla con reembolso: DENTRO_48H | ERROR_COBRO
--   r_regla sin reembolso: FUERA_48H | YA_INGRESO | INGRESO_TRAS_SOLICITUD | SIN_PAGO
-- ---------------------------------------------------------------------
-- La vista depende de _evaluar_membresia: se quita antes y se vuelve a crear abajo
drop view if exists solicitudes_cancelacion_pendientes;
drop function if exists _evaluar_cancelacion(bigint, text);
drop function if exists evaluar_cancelacion(text, bigint, text);
drop function if exists cancelar_membresia(text, text, bigint, text, numeric);
drop function if exists cancelar_membresia(text, text, bigint, text, text);
drop function if exists _evaluar_membresia(bigint, text, timestamptz);

create function _evaluar_membresia(p_mem bigint, p_tipo text, p_al timestamptz)
returns table (
  r_membresia_id bigint, r_plan text, r_fecha_fin date, r_pago_id bigint,
  r_fecha_pago timestamptz, r_horas double precision, r_ingreso boolean,
  r_reembolso double precision, r_metodo text, r_regla text, r_fecha_limite date
)
language plpgsql stable security definer set search_path = public as $$
declare
  v_mem membresias_cliente%rowtype; v_plan text; v_pago pagos%rowtype;
  v_neto numeric := 0; v_horas double precision; v_desde timestamptz;
  v_antes boolean := false; v_despues boolean := false;
  v_regla text; v_reemb numeric := 0;
begin
  if p_tipo not in ('VOLUNTARIA', 'ERROR_COBRO') then
    raise exception 'Tipo de cancelación no válido';
  end if;
  select * into v_mem from membresias_cliente where id = p_mem;
  if not found then raise exception 'La membresía no existe'; end if;
  select nombre into v_plan from tipos_membresia where id = v_mem.tipo_membresia_id;

  select * into v_pago from pagos
   where membresia_cliente_id = v_mem.id and tipo = 'PAGO' and estado = 'ACEPTADO'
   order by fecha_pago desc limit 1;

  if v_pago.id is null then
    v_regla := 'SIN_PAGO';
  else
    select coalesce(sum(_monto_neto(p)), 0) into v_neto
      from pagos p where p.membresia_cliente_id = v_mem.id;
    v_horas := extract(epoch from (p_al - v_pago.fecha_pago)) / 3600.0;

    -- Entradas con ESTA membresía: desde el pago y desde que empieza su vigencia
    -- (una renovación adelantada no se pierde por seguir usando la membresía actual)
    v_desde := greatest(v_pago.fecha_pago,
                        (v_mem.fecha_inicio::timestamp at time zone 'America/Mexico_City'));
    select coalesce(bool_or(ra.fecha_hora < p_al), false),
           coalesce(bool_or(ra.fecha_hora >= p_al), false)
      into v_antes, v_despues
      from registros_acceso ra
     where ra.cliente_id = v_mem.cliente_id and ra.es_entrada and ra.fecha_hora >= v_desde;

    if p_tipo = 'ERROR_COBRO' then
      v_regla := 'ERROR_COBRO';
    elsif v_horas > 48 then
      v_regla := 'FUERA_48H';
    elsif v_antes then
      v_regla := 'YA_INGRESO';
    elsif v_despues then
      v_regla := 'INGRESO_TRAS_SOLICITUD';
    else
      v_regla := 'DENTRO_48H';
    end if;

    if v_regla in ('ERROR_COBRO', 'DENTRO_48H') then
      v_reemb := greatest(v_neto, 0);
    end if;
  end if;

  return query select v_mem.id, v_plan, v_mem.fecha_fin, v_pago.id, v_pago.fecha_pago,
                      v_horas, (v_antes or v_despues), v_reemb::double precision,
                      v_pago.metodo_pago, v_regla,
                      case when v_reemb > 0 then
                        _sumar_dias_habiles((p_al at time zone 'America/Mexico_City')::date, 10) end;
end $$;

-- Membresía que se cancelaría: la vigente pagada más recientemente que no tenga
-- ya una cancelación registrada ni una solicitud pendiente.
create or replace function _membresia_a_cancelar(p_cliente_id bigint)
returns bigint language plpgsql stable security definer set search_path = public as $$
declare v_mem bigint;
begin
  select mc.id into v_mem
    from membresias_cliente mc
    left join lateral (select max(p.fecha_pago) as ult from pagos p
                        where p.membresia_cliente_id = mc.id and p.tipo = 'PAGO'
                          and p.estado = 'ACEPTADO') u on true
   where mc.cliente_id = p_cliente_id and mc.estado = 'ACTIVA'
     and mc.fecha_fin >= _hoy_fecha_mx() and mc.cancelada_en is null
     and not exists (select 1 from solicitudes_cancelacion s
                      where s.membresia_id = mc.id and s.estado = 'PENDIENTE')
   order by u.ult desc nulls last, mc.fecha_fin desc, mc.id desc
   limit 1;

  if v_mem is null then
    if exists (select 1 from solicitudes_cancelacion
                where cliente_id = p_cliente_id and estado = 'PENDIENTE') then
      raise exception 'El cliente ya tiene una solicitud de cancelación pendiente';
    end if;
    if exists (select 1 from membresias_cliente where cliente_id = p_cliente_id
                 and estado = 'ACTIVA' and fecha_fin >= _hoy_fecha_mx()) then
      raise exception 'La cancelación de este cliente ya fue registrada';
    end if;
    raise exception 'El cliente no tiene una membresía vigente';
  end if;
  return v_mem;
end $$;

-- Vista previa para Recepción o el Encargado (no cambia nada)
create function evaluar_cancelacion(p_actor_gmail text, p_cliente_id bigint, p_tipo text)
returns table (
  r_membresia_id bigint, r_plan text, r_fecha_fin date, r_pago_id bigint,
  r_fecha_pago timestamptz, r_horas double precision, r_ingreso boolean,
  r_reembolso double precision, r_metodo text, r_regla text, r_fecha_limite date
)
language plpgsql stable security definer set search_path = public as $$
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['ENCARGADO', 'RECEPCION']) then
    raise exception 'Solo Recepción o el Encargado pueden consultar cancelaciones';
  end if;
  return query select * from _evaluar_membresia(_membresia_a_cancelar(p_cliente_id), p_tipo, now());
end $$;

-- Paso 1: Recepción (o el Encargado) registra la solicitud. No cambia la membresía.
create or replace function solicitar_cancelacion(
  p_actor_gmail text, p_cliente_id bigint, p_tipo text, p_motivo text
)
returns table (
  r_membresia_id bigint, r_plan text, r_fecha_fin date, r_pago_id bigint,
  r_fecha_pago timestamptz, r_horas double precision, r_ingreso boolean,
  r_reembolso double precision, r_metodo text, r_regla text, r_fecha_limite date
)
language plpgsql security definer set search_path = public as $$
declare v_actor bigint; v_mem bigint;
begin
  v_actor := _id_usuario(p_actor_gmail);
  if not _tiene_rol(v_actor, array['ENCARGADO', 'RECEPCION']) then
    raise exception 'Solo Recepción o el Encargado pueden registrar cancelaciones';
  end if;
  if length(trim(coalesce(p_motivo, ''))) < 3 then
    raise exception 'Escribe el motivo de la cancelación';
  end if;
  if p_tipo not in ('VOLUNTARIA', 'ERROR_COBRO') then
    raise exception 'Tipo de cancelación no válido';
  end if;

  -- Evita que dos solicitudes simultáneas del mismo cliente se dupliquen
  perform 1 from clientes where id = p_cliente_id for update;
  v_mem := _membresia_a_cancelar(p_cliente_id);

  insert into solicitudes_cancelacion (cliente_id, membresia_id, tipo, motivo, solicitada_por)
  values (p_cliente_id, v_mem, p_tipo, trim(p_motivo), v_actor);

  return query select * from _evaluar_membresia(v_mem, p_tipo, now());
end $$;

-- Paso 2: el Encargado autoriza o rechaza desde su sesión.
-- Al autorizar se vuelve a evaluar con la hora de la solicitud:
--   con reembolso → la membresía se cancela y el reembolso queda PENDIENTE;
--   sin reembolso → la membresía sigue ACTIVA hasta su vencimiento.
create or replace function resolver_cancelacion(
  p_actor_gmail text, p_solicitud_id bigint, p_autorizar boolean, p_nota text default null
)
returns table (
  r_membresia_id bigint, r_plan text, r_fecha_fin date, r_pago_id bigint,
  r_fecha_pago timestamptz, r_horas double precision, r_ingreso boolean,
  r_reembolso double precision, r_metodo text, r_regla text, r_fecha_limite date
)
language plpgsql security definer set search_path = public as $$
declare v_actor bigint; s solicitudes_cancelacion%rowtype; e record; v_estado_mem text;
begin
  v_actor := _id_usuario(p_actor_gmail);
  if not _tiene_rol(v_actor, array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede autorizar cancelaciones';
  end if;

  select * into s from solicitudes_cancelacion where id = p_solicitud_id for update;
  if not found or s.estado <> 'PENDIENTE' then
    raise exception 'La solicitud ya fue resuelta o no existe';
  end if;

  select * into e from _evaluar_membresia(s.membresia_id, s.tipo, s.solicitada_en);

  if not p_autorizar then
    if length(trim(coalesce(p_nota, ''))) < 3 then
      raise exception 'Escribe el motivo del rechazo';
    end if;
    update solicitudes_cancelacion
       set estado = 'RECHAZADA', resuelta_por = v_actor, resuelta_en = now(),
           nota_encargado = trim(p_nota)
     where id = s.id;
    return query select e.r_membresia_id, e.r_plan, e.r_fecha_fin, e.r_pago_id, e.r_fecha_pago,
                        e.r_horas, e.r_ingreso, 0::double precision, e.r_metodo, e.r_regla,
                        null::date;
    return;
  end if;

  select estado into v_estado_mem from membresias_cliente where id = s.membresia_id for update;
  if v_estado_mem <> 'ACTIVA' then
    raise exception 'La membresía ya no está activa; rechaza la solicitud';
  end if;

  if e.r_reembolso > 0 then
    update membresias_cliente
       set estado = 'CANCELADA', cancelada_en = now(), cancelada_por = v_actor,
           motivo_cancelacion = s.motivo, tipo_cancelacion = s.tipo
     where id = s.membresia_id;

    insert into pagos (monto, metodo_pago, membresia_cliente_id, cliente_id, registrado_por,
                       tipo, estado, fecha_limite, reembolso_de)
    values (e.r_reembolso, e.r_metodo, s.membresia_id, s.cliente_id, v_actor,
            'REEMBOLSO', 'PENDIENTE', e.r_fecha_limite, e.r_pago_id);
  else
    update membresias_cliente
       set cancelada_en = now(), cancelada_por = v_actor,
           motivo_cancelacion = s.motivo, tipo_cancelacion = s.tipo
     where id = s.membresia_id;
  end if;

  update solicitudes_cancelacion
     set estado = 'AUTORIZADA', resuelta_por = v_actor, resuelta_en = now(),
         nota_encargado = nullif(trim(coalesce(p_nota, '')), ''),
         reembolso = e.r_reembolso, regla = e.r_regla
   where id = s.id;

  return query select e.r_membresia_id, e.r_plan, e.r_fecha_fin, e.r_pago_id, e.r_fecha_pago,
                      e.r_horas, e.r_ingreso, e.r_reembolso, e.r_metodo, e.r_regla,
                      e.r_fecha_limite;
end $$;

-- El Encargado marca el reembolso como entregado: a partir de ese momento resta
-- en los reportes (con la fecha de entrega).
create or replace function entregar_reembolso(p_actor_gmail text, p_pago_id bigint)
returns void language plpgsql security definer set search_path = public as $$
declare v_actor bigint;
begin
  v_actor := _id_usuario(p_actor_gmail);
  if not _tiene_rol(v_actor, array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede marcar reembolsos como entregados';
  end if;
  update pagos set estado = 'ACEPTADO', confirmado_por = v_actor,
                   confirmado_en = now(), fecha_pago = now()
   where id = p_pago_id and tipo = 'REEMBOLSO' and estado = 'PENDIENTE';
  if not found then
    raise exception 'El reembolso ya fue entregado o no existe';
  end if;
end $$;

-- confirmar_pago (y rechazar_pago, que lo usa) ya no aceptan reembolsos por error
create or replace function confirmar_pago(p_actor_gmail text, p_pago_id bigint, p_aceptar boolean)
returns date
language plpgsql security definer set search_path = public as $$
declare v_actor bigint; p pagos%rowtype;
begin
  v_actor := _id_usuario(p_actor_gmail);
  select * into p from pagos where id = p_pago_id for update;
  if not found or p.estado <> 'PENDIENTE' or p.tipo <> 'PAGO' then
    raise exception 'El pago ya fue resuelto o no existe';
  end if;
  if not _tiene_rol(v_actor, array['ENCARGADO', 'RECEPCION']) then
    raise exception 'Solo el Encargado o Recepción pueden validar pagos';
  end if;

  if not p_aceptar then
    update pagos set estado = 'RECHAZADO', confirmado_por = v_actor, confirmado_en = now() where id = p.id;
    update membresias_cliente set estado = 'RECHAZADA' where id = p.membresia_cliente_id;
    return null;
  end if;

  update pagos set estado = 'ACEPTADO', confirmado_por = v_actor, confirmado_en = now(), fecha_pago = now()
   where id = p.id;
  return _activar_membresia(p.membresia_cliente_id);
end $$;

-- ---------------------------------------------------------------------
-- Vistas
-- ---------------------------------------------------------------------

-- Solicitudes pendientes, con lo que aplicaría si el Encargado autoriza ahora
create view solicitudes_cancelacion_pendientes as
 select s.id, s.cliente_id, uc.nombre as cliente, c.numero_membresia,
        s.tipo, s.motivo, s.solicitada_en, us.nombre as solicito,
        e.r_plan as plan, e.r_fecha_fin as fecha_fin, e.r_horas as horas,
        e.r_reembolso as reembolso, e.r_metodo as metodo, e.r_regla as regla,
        e.r_fecha_limite as fecha_limite
   from solicitudes_cancelacion s
   join clientes c on c.id = s.cliente_id
   join usuarios uc on uc.id = c.usuario_id
   left join usuarios us on us.id = s.solicitada_por
   cross join lateral _evaluar_membresia(s.membresia_id, s.tipo, s.solicitada_en) e
  where s.estado = 'PENDIENTE';

-- Lista de clientes: si tiene una membresía vigente se muestra esa, aunque haya
-- otra posterior cancelada (antes una renovación adelantada cancelada hacía que
-- el cliente apareciera inactivo).
create or replace view clientes_lista as
 select c.id, u.nombre, u.gmail, coalesce(u.telefono, ''::text) as telefono, u.fecha_registro,
        c.codigo_qr, m.plan, m.fecha_fin, coalesce(m.activa, false) as activa,
        c.numero_membresia, (u.estado)::text as estado
   from clientes c
   join usuarios u on u.id = c.usuario_id
   left join lateral (
     select t.nombre as plan, mc.fecha_fin,
            (mc.estado = 'ACTIVA' and mc.fecha_fin >= _hoy_fecha_mx()) as activa
       from membresias_cliente mc join tipos_membresia t on t.id = mc.tipo_membresia_id
      where mc.cliente_id = c.id and mc.estado not in ('PENDIENTE', 'RECHAZADA')
      order by (mc.estado = 'ACTIVA' and mc.fecha_fin >= _hoy_fecha_mx()) desc,
               mc.fecha_fin desc, mc.id desc
      limit 1) m on true;

-- Cancelaciones autorizadas: incluye las que no tuvieron reembolso y el estado
-- del reembolso
drop view if exists cancelaciones_lista;
create view cancelaciones_lista as
 select 'MEMBRESIA'::text as tipo, mc.id, mc.cancelada_en,
        uc.nombre as cliente, c.numero_membresia, t.nombre as detalle,
        mc.motivo_cancelacion as motivo, ua.nombre as autorizo,
        coalesce(r.monto, 0)::double precision as reembolso,
        mc.tipo_cancelacion, (mc.estado = 'ACTIVA') as sigue_activa, mc.fecha_fin,
        r.id as reembolso_id, r.estado as estado_reembolso, r.metodo_pago as metodo_reembolso,
        r.fecha_limite, case when r.estado = 'ACEPTADO' then r.confirmado_en end as entregado_en,
        sol.solicito, sol.regla
   from membresias_cliente mc
   join clientes c on c.id = mc.cliente_id
   join usuarios uc on uc.id = c.usuario_id
   join tipos_membresia t on t.id = mc.tipo_membresia_id
   left join usuarios ua on ua.id = mc.cancelada_por
   left join lateral (select p.* from pagos p
                       where p.membresia_cliente_id = mc.id and p.tipo = 'REEMBOLSO'
                       order by p.id desc limit 1) r on true
   left join lateral (select us.nombre as solicito, s.regla
                        from solicitudes_cancelacion s
                        left join usuarios us on us.id = s.solicitada_por
                       where s.membresia_id = mc.id and s.estado = 'AUTORIZADA'
                       order by s.id desc limit 1) sol on true
  where mc.cancelada_en is not null
 union all
 select 'ASESORIA'::text, s.id, s.cancelada_en, uc.nombre, c.numero_membresia,
        'Asesoría con ' || ui.nombre, s.motivo_cancelacion, ua.nombre,
        0::double precision, null::text, false, null::date,
        null::bigint, null::text, null::text, null::date, null::timestamptz,
        null::text, null::text
   from solicitudes_asesoria s
   join clientes c on c.id = s.cliente_id
   join usuarios uc on uc.id = c.usuario_id
   join instructores i on i.id = s.instructor_id
   join usuarios ui on ui.id = i.usuario_id
   left join usuarios ua on ua.id = s.cancelada_por
  where s.estado = 'CANCELADA';

-- Gráfica semanal del Dashboard: solo pagos aceptados y los reembolsos restan
-- (dashboard_graficas.sql la dejaba sumando p.monto de todos los pagos)
create or replace view ingresos_semana as
with h as (select _hoy_fecha_mx() as hoy),
dias as (
  select d::date as dia
    from h, generate_series(date_trunc('week', h.hoy), date_trunc('week', h.hoy) + interval '6 days',
                            interval '1 day') d
)
select d.dia as fecha,
       extract(isodow from d.dia)::int as dia_semana,
       coalesce(sum(_monto_neto(p)), 0)::numeric as total
  from dias d
  left join pagos p on (p.fecha_pago at time zone 'America/Mexico_City')::date = d.dia
 group by d.dia
 order by d.dia;

grant execute on function evaluar_cancelacion(text, bigint, text) to anon, authenticated;
grant execute on function solicitar_cancelacion(text, bigint, text, text) to anon, authenticated;
grant execute on function resolver_cancelacion(text, bigint, boolean, text) to anon, authenticated;
grant execute on function entregar_reembolso(text, bigint) to anon, authenticated;
grant execute on function confirmar_pago(text, bigint, boolean) to anon, authenticated;
grant select on clientes_lista, cancelaciones_lista, ingresos_semana,
                solicitudes_cancelacion_pendientes to anon, authenticated;

notify pgrst, 'reload schema';

-- =====================================================================
--  AXOLOTL FITNESS CLUB · Visitas (pase de un día) cobradas en Recepción
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--  Requiere haber corrido antes pendientes_noviembre.sql y planes_recepcion.sql
--
--  · La visita NO es una membresía: no crea cliente ni aparece en la app del cliente.
--  · El precio es el del servicio marcado como "Visita" en "Servicios".
--  · Recepción o el Encargado la cobran en la sección "Visitas" y capturan
--    nombre, teléfono y contacto de emergencia del visitante.
--  · El cobro se guarda en pagos (tipo 'VISITA', ya aceptado), así que entra
--    solo en los ingresos del Dashboard y de Reportes.
-- =====================================================================

-- 1. Precio de la visita: el del servicio marcado como "Visita" en Servicios
--    (se crea, edita y elimina como cualquier servicio; ver planes_recepcion.sql)

-- Versión anterior: el precio vivía en configuracion_gym. Si había uno y aún no
-- existe el servicio, se crea el servicio "Visita" con ese precio.
do $$ begin
  if to_regclass('public.configuracion_gym') is not null then
    insert into servicios (nombre, duracion, precio, es_visita, visible_clientes, visible_recepcion)
    select 'Visita', '1 día', c.valor, true, false, true
      from configuracion_gym c
     where c.clave = 'precio_visita' and c.valor > 0
       and not exists (select 1 from servicios where es_visita);
    drop table configuracion_gym;
  end if;
end $$;
drop function if exists fijar_precio_visita(text, numeric);

create or replace function precio_visita()
returns double precision language sql stable security definer set search_path = public as $$
  select coalesce((select precio from servicios where es_visita limit 1), 0)::double precision
$$;

-- 2. Los pagos aceptan el tipo VISITA (sin cliente ni membresía)
alter table pagos drop constraint if exists pagos_tipo_chk;
alter table pagos add constraint pagos_tipo_chk
  check (tipo = any (array['PAGO', 'REEMBOLSO', 'VISITA'])) not valid;

-- 3. Datos del visitante
create table if not exists visitas (
  id bigint generated always as identity primary key,
  nombre text not null,
  telefono text not null,
  contacto_nombre text not null,
  contacto_telefono text not null,
  monto numeric(10,2) not null,
  metodo_pago text not null,
  pago_id bigint references pagos(id) on delete set null,
  registrado_por bigint references usuarios(id),
  fecha timestamptz not null default now()
);

alter table visitas enable row level security;

-- 4. Cobrar la visita: Recepción (o el Encargado)
create or replace function registrar_visita(
  p_actor_gmail text, p_nombre text, p_telefono text,
  p_contacto_nombre text, p_contacto_telefono text, p_metodo text
) returns bigint language plpgsql security definer set search_path = public as $$
declare
  v_actor bigint := _id_usuario(p_actor_gmail);
  v_precio numeric := precio_visita();
  v_pago bigint;
  v_visita bigint;
begin
  if not _tiene_rol(v_actor, array['RECEPCION', 'ENCARGADO']) then
    raise exception 'Solo Recepción o el Encargado pueden cobrar visitas';
  end if;
  -- El Encargado siempre puede; Recepción solo si la visita es visible para ella
  if not _tiene_rol(v_actor, array['ENCARGADO'])
     and not exists (select 1 from servicios where es_visita and visible_recepcion) then
    raise exception 'La visita no está disponible para Recepción';
  end if;
  if v_precio <= 0 then
    raise exception 'No hay un servicio de visita: el Encargado debe crearlo en Servicios';
  end if;
  if length(regexp_replace(coalesce(p_nombre, ''), '[^[:alpha:]]', '', 'g')) < 3 then
    raise exception 'Escribe el nombre del visitante';
  end if;
  if coalesce(p_telefono, '') !~ '^[0-9]{10}$' then
    raise exception 'El teléfono del visitante debe tener 10 dígitos';
  end if;
  if coalesce(trim(p_contacto_nombre), '') = '' or coalesce(trim(p_contacto_telefono), '') = '' then
    raise exception 'El contacto de emergencia es obligatorio';
  end if;
  perform _validar_contacto(p_contacto_nombre, p_contacto_telefono);
  if p_metodo not in ('Efectivo', 'Tarjeta') then
    raise exception 'La visita se cobra en efectivo o con tarjeta';
  end if;

  insert into pagos (monto, metodo_pago, registrado_por, tipo, estado, confirmado_por, confirmado_en)
  values (v_precio, p_metodo, v_actor, 'VISITA', 'ACEPTADO', v_actor, now())
  returning id into v_pago;

  insert into visitas (nombre, telefono, contacto_nombre, contacto_telefono, monto, metodo_pago, pago_id, registrado_por)
  values (trim(p_nombre), p_telefono, trim(p_contacto_nombre), p_contacto_telefono, v_precio, p_metodo, v_pago, v_actor)
  returning id into v_visita;

  return v_visita;
end $$;

-- 5. Visitas de hoy (para la lista de Recepción)
drop view if exists visitas_hoy;
create view visitas_hoy as
select v.id, v.nombre, v.telefono, v.monto::double precision as monto, v.metodo_pago,
       to_char(v.fecha at time zone 'America/Mexico_City', 'HH24:MI') as hora,
       u.nombre as registro
  from visitas v
  left join usuarios u on u.id = v.registrado_por
 where (v.fecha at time zone 'America/Mexico_City')::date = _hoy_fecha_mx()
 order by v.fecha desc;

grant execute on function precio_visita() to anon, authenticated;
grant execute on function registrar_visita(text, text, text, text, text, text) to anon, authenticated;
grant select on visitas_hoy to anon, authenticated;

notify pgrst, 'reload schema';

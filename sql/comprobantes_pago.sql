-- =====================================================================
--  AXOLOTL FITNESS CLUB · Comprobantes de pago del cliente
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  1. El cliente elige plan y método en "Pagos" y sube la foto de su comprobante.
--     Se crea un pago PENDIENTE (y su membresía PENDIENTE) con la imagen.
--  2. El Encargado lo ve en su pestaña "Pagos", abre el comprobante y lo
--     acepta o rechaza con confirmar_pago (ya existía).
--  3. Al aceptarlo, el pago pasa a ACEPTADO con fecha de hoy, así que entra
--     solo en reporte_mensual, resumen_dashboard e ingresos_semana
--     (Reportes / Dashboard). Mientras está pendiente o si se rechaza, no suma.
--  4. Al rechazar, el Encargado escribe el motivo y el cliente lo ve en rojo.
--  5. reporte_mensual agrupa por mes en hora de México (antes usaba UTC).
-- =====================================================================

-- Imagen del comprobante (JPEG en base64, igual que las fotos de perfil)
alter table pagos add column if not exists comprobante text;
-- Lo escribe el Encargado al rechazar
alter table pagos add column if not exists motivo_rechazo text;

-- El cliente que inició sesión envía su comprobante
create or replace function enviar_comprobante(
  p_gmail text, p_plan text, p_metodo text, p_comprobante text
) returns bigint
language plpgsql security definer set search_path = public as $$
declare v_cli bigint; v_usr bigint; v_tipo tipos_membresia%rowtype; v_mem bigint; v_pago bigint;
begin
  select c.id, u.id into v_cli, v_usr
    from clientes c join usuarios u on u.id = c.usuario_id
   where lower(u.gmail) = lower(trim(coalesce(p_gmail, '')));
  if v_cli is null then raise exception 'Cliente no encontrado'; end if;

  if p_metodo not in ('Transferencia', 'Depósito', 'Efectivo') then
    raise exception 'Método de pago no válido';
  end if;
  if coalesce(length(p_comprobante), 0) < 100 then
    raise exception 'Falta la imagen del comprobante';
  end if;

  select * into v_tipo from tipos_membresia where nombre = p_plan;
  if not found then raise exception 'Plan no válido'; end if;

  if exists (select 1 from membresias_cliente where cliente_id = v_cli and estado = 'PENDIENTE') then
    raise exception 'Ya tienes un pago en revisión. Espera a que el Encargado lo valide.';
  end if;

  insert into membresias_cliente (cliente_id, tipo_membresia_id, fecha_inicio, fecha_fin, estado)
  values (v_cli, v_tipo.id, _hoy_fecha_mx(), _hoy_fecha_mx(), 'PENDIENTE')
  returning id into v_mem;

  insert into pagos (monto, metodo_pago, membresia_cliente_id, cliente_id, registrado_por, estado, comprobante)
  values (v_tipo.precio, p_metodo, v_mem, v_cli, v_usr, 'PENDIENTE', p_comprobante)
  returning id into v_pago;

  return v_pago;
end $$;

-- Último comprobante del cliente (para mostrarle si está en revisión, aceptado o rechazado)
drop function if exists mi_ultimo_comprobante(text);
create function mi_ultimo_comprobante(p_gmail text)
returns table (r_id bigint, r_plan text, r_monto double precision, r_metodo text,
               r_estado text, r_enviado timestamptz, r_resuelto timestamptz, r_motivo text)
language sql stable security definer set search_path = public as $$
  select p.id, t.nombre, p.monto::double precision, p.metodo_pago, p.estado,
         coalesce(p.confirmado_en, p.fecha_pago), p.confirmado_en, p.motivo_rechazo
    from pagos p
    join clientes c on c.id = p.cliente_id
    join usuarios u on u.id = c.usuario_id
    left join membresias_cliente mc on mc.id = p.membresia_cliente_id
    left join tipos_membresia t on t.id = mc.tipo_membresia_id
   where lower(u.gmail) = lower(trim(p_gmail))
     and p.comprobante is not null and p.tipo = 'PAGO'
   order by p.id desc
   limit 1
$$;

-- La lista de pendientes indica si trae comprobante (la imagen se pide aparte, pesa)
create or replace view pagos_pendientes as
 select p.id,
    p.monto::double precision as monto,
    p.metodo_pago,
    p.fecha_pago,
    c.id as cliente_id,
    u.nombre as cliente,
    c.numero_membresia,
    t.nombre as plan,
    (p.comprobante is not null) as tiene_comprobante
   from pagos p
     join clientes c on c.id = p.cliente_id
     join usuarios u on u.id = c.usuario_id
     left join membresias_cliente mc on mc.id = p.membresia_cliente_id
     left join tipos_membresia t on t.id = mc.tipo_membresia_id
  where p.estado = 'PENDIENTE' and p.tipo = 'PAGO';

-- Imagen del comprobante: solo el Encargado
create or replace function comprobante_pago(p_actor_gmail text, p_pago_id bigint)
returns text language plpgsql stable security definer set search_path = public as $$
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede ver los comprobantes';
  end if;
  return (select comprobante from pagos where id = p_pago_id);
end $$;

-- Rechazo con motivo (obligatorio). Usa confirmar_pago para no repetir sus reglas.
create or replace function rechazar_pago(p_actor_gmail text, p_pago_id bigint, p_motivo text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if length(trim(coalesce(p_motivo, ''))) < 3 then
    raise exception 'Escribe el motivo del rechazo';
  end if;
  perform confirmar_pago(p_actor_gmail, p_pago_id, false);
  update pagos set motivo_rechazo = trim(p_motivo) where id = p_pago_id;
end $$;

-- Reporte mensual: el mes se calcula en hora de México
create or replace view reporte_mensual as
 select to_char(m, 'YYYY-MM') as periodo,
    coalesce((select sum(_monto_neto(p.*)) from pagos p
               where date_trunc('month', p.fecha_pago at time zone 'America/Mexico_City') = m.m), 0) as servicios,
    coalesce((select sum(s.costo) from solicitudes_asesoria s
               where s.estado = 'APROBADA'
                 and date_trunc('month', s.fecha_solicitud::timestamp) = m.m), 0) as asesorias,
    coalesce((select sum(g.monto) from gastos g
               where date_trunc('month', g.fecha::timestamp) = m.m), 0) as gastos
   from generate_series(date_trunc('month', _hoy_fecha_mx()::timestamp) - interval '5 mons',
                        date_trunc('month', _hoy_fecha_mx()::timestamp), interval '1 mon') m(m);

grant execute on function rechazar_pago(text, bigint, text) to anon, authenticated;
grant select on reporte_mensual to anon, authenticated;
grant execute on function enviar_comprobante(text, text, text, text) to anon, authenticated;
grant execute on function mi_ultimo_comprobante(text) to anon, authenticated;
grant execute on function comprobante_pago(text, bigint) to anon, authenticated;
grant select on pagos_pendientes to anon, authenticated;

notify pgrst, 'reload schema';

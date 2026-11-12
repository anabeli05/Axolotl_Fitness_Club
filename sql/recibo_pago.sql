-- =====================================================================
--  AXOLOTL FITNESS CLUB · Recibo de pago por correo
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  Cuando Recepción (o el Encargado) renueva un plan en efectivo, la app llama a la
--  Edge Function "enviar-recibo" con el id del cliente. La función toma de aquí el
--  pago recién registrado y manda el recibo al correo que el cliente tiene registrado.
--  Cada pago se envía una sola vez (recibo_enviado_en).
--  Estas funciones solo las puede usar la Edge Function (service_role), no la app.
-- =====================================================================

alter table pagos add column if not exists recibo_enviado_en timestamptz;

-- Último pago aceptado del cliente, de los últimos 15 minutos y aún sin recibo
drop function if exists recibo_por_enviar(bigint);
create function recibo_por_enviar(p_cliente_id bigint)
returns table (
  r_pago_id bigint, r_nombre text, r_gmail text, r_numero_membresia text,
  r_plan text, r_monto double precision, r_metodo text, r_fecha_pago timestamptz,
  r_vigencia_inicio date, r_vigencia_fin date, r_atendio text
)
language sql stable security definer set search_path = public as $$
  select p.id, u.nombre, u.gmail, c.numero_membresia,
         t.nombre, p.monto::double precision, p.metodo_pago, p.fecha_pago,
         mc.fecha_inicio, mc.fecha_fin, ua.nombre
    from pagos p
    join clientes c on c.id = p.cliente_id
    join usuarios u on u.id = c.usuario_id
    left join membresias_cliente mc on mc.id = p.membresia_cliente_id
    left join tipos_membresia t on t.id = mc.tipo_membresia_id
    left join usuarios ua on ua.id = p.registrado_por
   where p.cliente_id = p_cliente_id
     and p.tipo = 'PAGO' and p.estado = 'ACEPTADO'
     and p.recibo_enviado_en is null
     and p.fecha_pago > now() - interval '15 minutes'
   order by p.id desc
   limit 1
$$;

create or replace function marcar_recibo_enviado(p_pago_id bigint)
returns void language sql security definer set search_path = public as $$
  update pagos set recibo_enviado_en = now() where id = p_pago_id
$$;

revoke all on function recibo_por_enviar(bigint) from public, anon, authenticated;
revoke all on function marcar_recibo_enviado(bigint) from public, anon, authenticated;
grant execute on function recibo_por_enviar(bigint) to service_role;
grant execute on function marcar_recibo_enviado(bigint) to service_role;

notify pgrst, 'reload schema';

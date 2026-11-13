-- =====================================================================
--  AXOLOTL FITNESS CLUB · Recepción también valida pagos
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--  Requiere haber corrido antes pendientes_noviembre.sql y comprobantes_pago.sql
--
--  Antes solo el Encargado podía ver los comprobantes y confirmar transferencias.
--  Ahora Recepción tiene la misma pestaña "Pagos": ve el comprobante y acepta
--  o rechaza (con motivo) igual que el Encargado. Queda registrado quién lo hizo
--  en pagos.confirmado_por.
-- =====================================================================

create or replace function confirmar_pago(p_actor_gmail text, p_pago_id bigint, p_aceptar boolean)
returns date
language plpgsql security definer set search_path = public as $$
declare v_actor bigint; p pagos%rowtype;
begin
  v_actor := _id_usuario(p_actor_gmail);
  select * into p from pagos where id = p_pago_id for update;
  if not found or p.estado <> 'PENDIENTE' then
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

  -- El ingreso cuenta el día en que se confirma
  update pagos set estado = 'ACEPTADO', confirmado_por = v_actor, confirmado_en = now(), fecha_pago = now()
   where id = p.id;
  return _activar_membresia(p.membresia_cliente_id);
end $$;

-- Imagen del comprobante: Encargado y Recepción
create or replace function comprobante_pago(p_actor_gmail text, p_pago_id bigint)
returns text language plpgsql stable security definer set search_path = public as $$
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['ENCARGADO', 'RECEPCION']) then
    raise exception 'Solo el Encargado o Recepción pueden ver los comprobantes';
  end if;
  return (select comprobante from pagos where id = p_pago_id);
end $$;

grant execute on function confirmar_pago(text, bigint, boolean) to anon, authenticated;
grant execute on function comprobante_pago(text, bigint) to anon, authenticated;

notify pgrst, 'reload schema';

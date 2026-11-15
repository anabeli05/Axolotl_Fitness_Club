-- =====================================================================
--  AXOLOTL FITNESS CLUB · Eliminar cliente (solo Encargado)
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--  Requiere pendientes_noviembre.sql ya corrido.
--
--  · Dar de baja (cambiar_estado_cliente, sql/estado_cuenta_cliente.sql): la cuenta
--    queda INACTIVA y se puede reactivar.
--  · Eliminar (este archivo): borra al cliente y sus datos personales (rostro,
--    accesos, membresías, rutinas, asesorías). No se puede deshacer.
--    Los pagos se conservan sin cliente para no alterar los reportes de ingresos.
--
--  Corrige además la eliminación automática de clientes inactivos: fallaba si el
--  cliente había enviado un comprobante desde la app, porque ese pago quedaba
--  registrado a su nombre (pagos.registrado_por).
-- =====================================================================

-- Borra un cliente y su usuario. Uso interno.
create or replace function _eliminar_cliente(p_cliente_id bigint)
returns void language plpgsql security definer set search_path = public as $$
declare v_usr bigint;
begin
  select usuario_id into v_usr from clientes where id = p_cliente_id;
  if v_usr is null then raise exception 'Cliente no encontrado'; end if;

  -- Pagos: se conservan para los reportes, sin datos del cliente
  update pagos set cliente_id = null, membresia_cliente_id = null where cliente_id = p_cliente_id;
  update pagos set membresia_cliente_id = null
   where membresia_cliente_id in (select id from membresias_cliente where cliente_id = p_cliente_id);
  update pagos set registrado_por = null where registrado_por = v_usr;

  delete from ejercicios_rutina where rutina_id in (select id from rutinas where cliente_id = p_cliente_id);
  delete from rutinas where cliente_id = p_cliente_id;
  delete from solicitudes_asesoria where cliente_id = p_cliente_id;
  delete from asistencias where cliente_id = p_cliente_id;
  delete from registros_acceso where cliente_id = p_cliente_id;
  delete from rostros_clientes where cliente_id = p_cliente_id;
  delete from membresias_cliente where cliente_id = p_cliente_id;  -- y sus solicitudes de cancelación
  delete from clientes where id = p_cliente_id;
  delete from usuario_roles where usuario_id = v_usr;
  delete from usuarios where id = v_usr;
end $$;

-- El Encargado elimina a un cliente desde la app
create or replace function eliminar_cliente(p_actor_gmail text, p_cliente_id bigint)
returns void language plpgsql security definer set search_path = public as $$
declare v_usr bigint;
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede eliminar clientes';
  end if;
  select usuario_id into v_usr from clientes where id = p_cliente_id for update;
  if v_usr is null then raise exception 'Cliente no encontrado'; end if;
  -- Si también es del personal, se elimina desde Personal, no desde Clientes
  if not _es_solo_cliente(v_usr) then
    raise exception 'Este usuario también es parte del personal; elimínalo desde Personal';
  end if;
  perform _eliminar_cliente(p_cliente_id);
end $$;

-- Eliminación automática (365 días sin uso, avisado hace 30 días o más)
create or replace function eliminar_clientes_inactivos()
returns integer
language plpgsql security definer set search_path = public as $$
declare r record; n int := 0;
begin
  for r in
    select c.id as cli
      from clientes c
     where _es_solo_cliente(c.usuario_id)
       and _ultimo_uso(c.id) <= _hoy_fecha_mx() - 365
       and c.aviso_eliminacion_en is not null
       and c.aviso_eliminacion_en >= _ultimo_uso(c.id)
       and c.aviso_eliminacion_en <= _hoy_fecha_mx() - 30
  loop
    perform _eliminar_cliente(r.cli);
    n := n + 1;
  end loop;
  return n;
end $$;

revoke all on function _eliminar_cliente(bigint) from public, anon, authenticated;
revoke all on function eliminar_clientes_inactivos() from public, anon, authenticated;
grant execute on function eliminar_clientes_inactivos() to service_role;
grant execute on function eliminar_cliente(text, bigint) to anon, authenticated;

notify pgrst, 'reload schema';

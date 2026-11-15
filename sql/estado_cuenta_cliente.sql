-- =====================================================================
--  AXOLOTL FITNESS CLUB · Activar / dar de baja la cuenta de un cliente
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  Solo el Encargado cambia el estado de la cuenta (ACTIVO / INACTIVO), desde
--  el interruptor del formulario "Editar cliente". Una cuenta INACTIVA no puede
--  entrar al gimnasio aunque tenga la membresía pagada.
--
--  editar_cliente ya no cambia el estado (antes cualquiera que lo llamara podía
--  dar de baja a un cliente); conserva el parámetro p_estado para que la app
--  siga funcionando igual.
-- =====================================================================

create or replace function cambiar_estado_cliente(p_actor_gmail text, p_cliente_id bigint, p_activo boolean)
returns void language plpgsql security definer set search_path = public as $$
declare v_user bigint;
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede activar o dar de baja a un cliente';
  end if;
  select usuario_id into v_user from clientes where id = p_cliente_id;
  if v_user is null then raise exception 'Cliente no encontrado'; end if;

  update usuarios
     set estado = (case when p_activo then 'ACTIVO' else 'INACTIVO' end)::estado_t
   where id = v_user;
end $$;

create or replace function editar_cliente(
  p_id bigint, p_nombre text, p_telefono text, p_gmail text, p_estado text,
  p_contacto_nombre text default null, p_contacto_telefono text default null
)
returns void language plpgsql security definer set search_path = public, extensions as $$
declare v_user bigint;
begin
  select usuario_id into v_user from clientes where id = p_id;
  if v_user is null then raise exception 'Cliente no encontrado'; end if;
  if exists (select 1 from usuarios where lower(gmail) = lower(trim(p_gmail)) and id <> v_user) then
    raise exception 'Ese correo ya está registrado';
  end if;

  -- El estado de la cuenta se cambia solo con cambiar_estado_cliente (Encargado)
  update usuarios
     set nombre = trim(p_nombre), telefono = coalesce(p_telefono, ''),
         gmail = lower(trim(p_gmail))
   where id = v_user;

  if p_contacto_nombre is not null or p_contacto_telefono is not null then
    perform _validar_contacto(p_contacto_nombre, p_contacto_telefono);
    update clientes
       set contacto_emergencia_nombre = nullif(trim(coalesce(p_contacto_nombre, '')), ''),
           contacto_emergencia_telefono = nullif(trim(coalesce(p_contacto_telefono, '')), '')
     where id = p_id;
  end if;
end $$;

grant execute on function cambiar_estado_cliente(text, bigint, boolean) to anon, authenticated;

notify pgrst, 'reload schema';

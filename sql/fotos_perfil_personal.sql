-- =====================================================================
--  AXOLOTL FITNESS CLUB · Fotos de perfil: actualizar la del cliente y foto del personal
--  Requiere sql/foto_y_contacto_cliente.sql
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  1. Cliente: al ACTUALIZAR el rostro en Recepción, la foto se reemplaza
--     (antes solo se guardaba la primera vez). El cliente sigue sin poder cambiarla.
--  2. Personal (Recepción, Instructor, Encargado): el Encargado les toma la foto
--     al darlos de alta y puede actualizarla desde Usuarios.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Cliente: la foto ya no queda bloqueada
-- ---------------------------------------------------------------------
drop trigger if exists clientes_foto_fija on clientes;
drop function if exists bloquear_cambio_foto();

create or replace function registrar_rostro_cliente(
  p_cliente_id bigint, p_embedding real[], p_foto text default null
)
returns void language plpgsql security definer set search_path = public as $$
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

    -- La foto nueva reemplaza a la anterior (máx. ~400 KB en base64)
    if p_foto is not null and length(p_foto) between 100 and 400000 then
        update clientes set foto_perfil = p_foto where id = p_cliente_id;
    end if;
end $$;

grant execute on function registrar_rostro_cliente(bigint, real[], text) to anon, authenticated;

-- ---------------------------------------------------------------------
-- 2. Foto del personal
-- ---------------------------------------------------------------------
alter table usuarios add column if not exists foto_perfil text;  -- JPEG en base64 (solo personal)

-- Solo el Encargado toma o actualiza la foto del personal
create or replace function guardar_foto_personal(p_actor_gmail text, p_usuario_id bigint, p_foto text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede tomar la foto del personal';
  end if;
  if not exists (
    select 1 from usuario_roles ur join roles r on r.id = ur.rol_id
     where ur.usuario_id = p_usuario_id and r.nombre in ('RECEPCION', 'INSTRUCTOR', 'ENCARGADO')
  ) then
    raise exception 'El usuario no es parte del personal';
  end if;
  if p_foto is null or length(p_foto) not between 100 and 400000 then
    raise exception 'Foto inválida';
  end if;

  update usuarios set foto_perfil = p_foto where id = p_usuario_id;
end $$;

-- Foto de un miembro del personal por id (lista de Usuarios)
create or replace function foto_usuario(p_usuario_id bigint)
returns text language sql stable security definer set search_path = public as $$
  select foto_perfil from usuarios where id = p_usuario_id
$$;

-- Foto del miembro del personal que inició sesión (su perfil)
create or replace function foto_personal(p_gmail text)
returns text language sql stable security definer set search_path = public as $$
  select foto_perfil from usuarios where lower(gmail) = lower(trim(p_gmail))
$$;

grant execute on function guardar_foto_personal(text, bigint, text) to anon, authenticated;
grant execute on function foto_usuario(bigint) to anon, authenticated;
grant execute on function foto_personal(text) to anon, authenticated;

notify pgrst, 'reload schema';

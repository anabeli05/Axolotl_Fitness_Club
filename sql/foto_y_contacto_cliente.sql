-- =====================================================================
--  AXOLOTL FITNESS CLUB · Foto de perfil fija y contacto de emergencia del cliente
--  Requiere sql/reconocimiento_facial.sql, sql/pendientes_noviembre.sql
--  y sql/contacto_obligatorio.sql
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  1. La foto de perfil del cliente es la que se toma en recepción al registrar
--     su rostro (JPEG pequeño en base64). Se guarda solo la primera vez y después
--     ya no se puede cambiar (lo impide un trigger).
--  2. El cliente puede ver y editar su propio contacto de emergencia.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Foto de perfil
-- ---------------------------------------------------------------------
alter table clientes add column if not exists foto_perfil text;  -- JPEG en base64

-- Una vez guardada, la foto ya no se cambia ni se borra
create or replace function bloquear_cambio_foto()
returns trigger language plpgsql as $$
begin
  if old.foto_perfil is not null and new.foto_perfil is distinct from old.foto_perfil then
    raise exception 'La foto de perfil del cliente no se puede cambiar';
  end if;
  return new;
end $$;

drop trigger if exists clientes_foto_fija on clientes;
create trigger clientes_foto_fija before update of foto_perfil on clientes
  for each row execute function bloquear_cambio_foto();

-- registrar_rostro_cliente ahora recibe también la foto (opcional).
-- Se borra la versión anterior para que PostgREST no tenga dos funciones con el mismo nombre.
drop function if exists registrar_rostro_cliente(bigint, real[]);

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

    -- La foto solo se guarda si el cliente todavía no tiene (máx. ~300 KB)
    if p_foto is not null and length(p_foto) between 100 and 300000 then
        update clientes set foto_perfil = p_foto
         where id = p_cliente_id and foto_perfil is null;
    end if;
end $$;

grant execute on function registrar_rostro_cliente(bigint, real[], text) to anon, authenticated;

-- Foto del cliente que inició sesión (null si aún no tiene)
create or replace function foto_cliente(p_gmail text)
returns text language sql stable security definer set search_path = public as $$
  select c.foto_perfil
    from clientes c join usuarios u on u.id = c.usuario_id
   where lower(u.gmail) = lower(trim(p_gmail))
$$;

grant execute on function foto_cliente(text) to anon, authenticated;

-- ---------------------------------------------------------------------
-- 2. Contacto de emergencia del propio cliente
-- ---------------------------------------------------------------------
create or replace function mi_contacto_emergencia(p_gmail text)
returns table(r_nombre text, r_telefono text)
language sql stable security definer set search_path = public as $$
  select coalesce(c.contacto_emergencia_nombre, '')::text,
         coalesce(c.contacto_emergencia_telefono, '')::text
    from clientes c join usuarios u on u.id = c.usuario_id
   where lower(u.gmail) = lower(trim(p_gmail))
$$;

create or replace function editar_mi_contacto(p_gmail text, p_nombre text, p_telefono text)
returns void language plpgsql security definer set search_path = public as $$
declare v_cli bigint;
begin
  select c.id into v_cli
    from clientes c join usuarios u on u.id = c.usuario_id
   where lower(u.gmail) = lower(trim(p_gmail)) and u.estado = 'ACTIVO';
  if v_cli is null then
    raise exception 'Solo el cliente puede editar su contacto de emergencia';
  end if;

  perform _validar_contacto(p_nombre, p_telefono);

  update clientes
     set contacto_emergencia_nombre = trim(p_nombre),
         contacto_emergencia_telefono = p_telefono
   where id = v_cli;
end $$;

grant execute on function mi_contacto_emergencia(text) to anon, authenticated;
grant execute on function editar_mi_contacto(text, text, text) to anon, authenticated;

-- PostgREST recarga las funciones
notify pgrst, 'reload schema';

-- ---------------------------------------------------------------------
-- 3. Foto en el acceso facial de Recepción (se pide después de identificar)
-- ---------------------------------------------------------------------
create or replace function foto_cliente_por_id(p_cliente_id bigint)
returns text language sql stable security definer set search_path = public as $$
  select foto_perfil from clientes where id = p_cliente_id
$$;

grant execute on function foto_cliente_por_id(bigint) to anon, authenticated;

notify pgrst, 'reload schema';

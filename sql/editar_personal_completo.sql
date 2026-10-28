-- =====================================================================
-- GYM CONTROL · Editar personal con teléfono, costo y capacidad
-- Pegar completo en Supabase > SQL Editor > Run
-- =====================================================================

-- 1) Datos que no vienen en personal_lista (para llenar el formulario de edición)
drop function if exists detalle_personal(bigint);
create or replace function detalle_personal(p_id bigint)
returns table (r_telefono text, r_costo numeric, r_max_clientes int)
language plpgsql security definer set search_path = public, extensions as $$
begin
  return query
  select coalesce(u.telefono, '')::text, i.costo_asesoria::numeric, i.max_clientes::int
    from usuarios u
    left join instructores i on i.usuario_id = u.id
   where u.id = p_id;
end $$;

-- 2) editar_personal ahora también recibe teléfono, costo y capacidad.
--    Los tres son opcionales: si llegan en null se conserva lo que ya había.
drop function if exists editar_personal(text, bigint, text, text, text, text);
drop function if exists editar_personal(text, bigint, text, text, text, text, text, numeric, int);

create or replace function editar_personal(
  p_actor_gmail text, p_id bigint, p_nombre text, p_gmail text, p_rol text, p_estado text,
  p_telefono text default null, p_costo numeric default null, p_max_clientes int default null
)
returns void
language plpgsql security definer set search_path = public, extensions as $$
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
end $$;

revoke execute on function detalle_personal(bigint) from public;
revoke execute on function editar_personal(text, bigint, text, text, text, text, text, numeric, int) from public;
grant execute on function detalle_personal(bigint) to anon, authenticated;
grant execute on function editar_personal(text, bigint, text, text, text, text, text, numeric, int) to anon, authenticated;

notify pgrst, 'reload schema';

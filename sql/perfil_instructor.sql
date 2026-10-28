-- =====================================================================
-- GYM CONTROL · Perfil del instructor (leer y editar sus propios datos)
-- Pegar completo en Supabase > SQL Editor > Run
-- =====================================================================

-- 1) Datos del instructor que inició sesión
drop function if exists perfil_instructor(text);
create or replace function perfil_instructor(p_gmail text)
returns table (
  r_id bigint, r_nombre text, r_telefono text, r_gmail text,
  r_estado text, r_max_clientes int, r_activos int
)
language plpgsql security definer set search_path = public, extensions as $$
begin
  return query
  select u.id, u.nombre::text, coalesce(u.telefono, '')::text, u.gmail::text,
         u.estado::text, coalesce(i.max_clientes, 10)::int,
         coalesce((select l.activos from instructores_lista l
                   where lower(l.gmail) = lower(u.gmail) limit 1), 0)::int
    from usuarios u
    join instructores i on i.usuario_id = u.id
   where lower(u.gmail) = lower(trim(p_gmail));
end $$;

-- 2) El instructor edita su nombre, teléfono y correo
drop function if exists editar_perfil_instructor(text, text, text, text);
create or replace function editar_perfil_instructor(
  p_gmail_actual text, p_nombre text, p_telefono text, p_gmail text
)
returns void
language plpgsql security definer set search_path = public, extensions as $$
declare v_id bigint;
begin
  select u.id into v_id
    from usuarios u
    join instructores i on i.usuario_id = u.id
   where lower(u.gmail) = lower(trim(p_gmail_actual));

  if v_id is null then raise exception 'Instructor no encontrado'; end if;
  if exists (select 1 from usuarios where lower(gmail) = lower(trim(p_gmail)) and id <> v_id) then
    raise exception 'Ese correo ya está registrado';
  end if;

  update usuarios
     set nombre = trim(p_nombre), telefono = coalesce(p_telefono, ''),
         gmail = lower(trim(p_gmail))
   where id = v_id;
end $$;

revoke execute on function perfil_instructor(text) from public;
revoke execute on function editar_perfil_instructor(text, text, text, text) from public;
grant execute on function perfil_instructor(text) to anon, authenticated;
grant execute on function editar_perfil_instructor(text, text, text, text) to anon, authenticated;

notify pgrst, 'reload schema';

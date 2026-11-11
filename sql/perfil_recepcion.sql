-- =====================================================================
--  AXOLOTL FITNESS CLUB · Perfil de Recepción
--  Requiere sql/fotos_perfil_personal.sql (foto del personal)
--  Supabase → SQL Editor → pegar → Run (se puede correr varias veces)
--
--  Recepción ve sus datos y su foto, y puede cambiar su teléfono.
--  Nombre, correo, rol y estado los sigue administrando el Encargado.
-- =====================================================================

-- Datos del miembro del personal que inició sesión
create or replace function perfil_personal(p_gmail text)
returns table(r_id bigint, r_nombre text, r_gmail text, r_telefono text,
              r_estado text, r_fecha_registro date)
language sql stable security definer set search_path = public as $$
  select u.id, u.nombre, u.gmail, coalesce(u.telefono, ''), u.estado::text, u.fecha_registro
    from usuarios u
   where lower(u.gmail) = lower(trim(p_gmail))
     and exists (select 1 from usuario_roles ur join roles r on r.id = ur.rol_id
                  where ur.usuario_id = u.id
                    and r.nombre in ('RECEPCION', 'INSTRUCTOR', 'ENCARGADO'))
$$;

-- Recepción cambia su propio teléfono (10 dígitos)
create or replace function editar_mi_telefono(p_gmail text, p_telefono text)
returns void language plpgsql security definer set search_path = public as $$
declare v_id bigint;
begin
  select u.id into v_id
    from usuarios u
    join usuario_roles ur on ur.usuario_id = u.id
    join roles r on r.id = ur.rol_id
   where lower(u.gmail) = lower(trim(p_gmail)) and r.nombre = 'RECEPCION' and u.estado = 'ACTIVO';
  if v_id is null then
    raise exception 'Solo Recepción puede cambiar su teléfono desde aquí';
  end if;
  if coalesce(p_telefono, '') !~ '^[0-9]{10}$' then
    raise exception 'El teléfono debe tener 10 dígitos';
  end if;

  update usuarios set telefono = p_telefono where id = v_id;
end $$;

grant execute on function perfil_personal(text) to anon, authenticated;
grant execute on function editar_mi_telefono(text, text) to anon, authenticated;

notify pgrst, 'reload schema';

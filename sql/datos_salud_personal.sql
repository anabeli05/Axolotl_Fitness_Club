-- =====================================================================
--  AXOLOTL FITNESS CLUB · Fecha de nacimiento y datos de salud del PERSONAL
--  Requiere sql/pendientes_noviembre.sql (_tiene_rol, _id_usuario, _hoy_fecha_mx)
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  Mismos datos que los clientes (sql/datos_salud_cliente.sql):
--    fecha de nacimiento, tipo de sangre, alergias, padecimientos/lesiones
--    y medicamentos.
--  Van en una tabla aparte (no en usuarios) para que ninguna vista los exponga.
--  Solo el Encargado los lee y guarda.
-- =====================================================================

create table if not exists salud_personal (
  usuario_id       bigint primary key references usuarios(id) on delete cascade,
  fecha_nacimiento date,
  tipo_sangre      text check (tipo_sangre is null or tipo_sangre in ('A+','A-','B+','B-','AB+','AB-','O+','O-')),
  alergias         text,
  padecimientos    text,
  medicamentos     text,
  actualizado      timestamp with time zone default now() not null
);

-- Sin acceso directo: solo con las funciones de abajo
alter table salud_personal enable row level security;
revoke all on table salud_personal from anon, authenticated;

-- ---------------------------------------------------------------------
-- Leer (solo Encargado)
-- ---------------------------------------------------------------------
drop function if exists datos_salud_personal(text, bigint);
create function datos_salud_personal(p_actor_gmail text, p_usuario_id bigint)
returns table (r_fecha_nacimiento date, r_tipo_sangre text, r_alergias text,
               r_padecimientos text, r_medicamentos text)
language plpgsql stable security definer set search_path = public as $$
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede ver los datos de salud del personal';
  end if;
  return query
    select s.fecha_nacimiento, s.tipo_sangre,
           coalesce(s.alergias, '')::text, coalesce(s.padecimientos, '')::text,
           coalesce(s.medicamentos, '')::text
      from salud_personal s where s.usuario_id = p_usuario_id;
end $$;

-- ---------------------------------------------------------------------
-- Guardar (solo Encargado). Textos vacíos se guardan como null.
-- ---------------------------------------------------------------------
drop function if exists guardar_datos_salud_personal(text, bigint, date, text, text, text, text);
create function guardar_datos_salud_personal(
  p_actor_gmail text, p_usuario_id bigint, p_fecha_nacimiento date,
  p_tipo_sangre text default null, p_alergias text default null,
  p_padecimientos text default null, p_medicamentos text default null
)
returns void
language plpgsql security definer set search_path = public as $$
declare v_tipo text := nullif(upper(trim(coalesce(p_tipo_sangre, ''))), '');
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede guardar los datos de salud del personal';
  end if;
  if not exists (
    select 1 from usuario_roles ur join roles r on r.id = ur.rol_id
     where ur.usuario_id = p_usuario_id and r.nombre in ('RECEPCION', 'INSTRUCTOR', 'ENCARGADO')
  ) then
    raise exception 'Solo se pueden guardar datos del personal';
  end if;
  if p_fecha_nacimiento is not null and
     (p_fecha_nacimiento >= _hoy_fecha_mx() or p_fecha_nacimiento < _hoy_fecha_mx() - interval '100 years') then
    raise exception 'Fecha de nacimiento inválida';
  end if;
  if v_tipo is not null and v_tipo not in ('A+','A-','B+','B-','AB+','AB-','O+','O-') then
    raise exception 'Tipo de sangre inválido';
  end if;

  insert into salud_personal (usuario_id, fecha_nacimiento, tipo_sangre, alergias,
                              padecimientos, medicamentos, actualizado)
  values (p_usuario_id, p_fecha_nacimiento, v_tipo,
          nullif(left(trim(coalesce(p_alergias, '')), 300), ''),
          nullif(left(trim(coalesce(p_padecimientos, '')), 300), ''),
          nullif(left(trim(coalesce(p_medicamentos, '')), 300), ''),
          now())
  on conflict (usuario_id) do update
     set fecha_nacimiento = excluded.fecha_nacimiento,
         tipo_sangre      = excluded.tipo_sangre,
         alergias         = excluded.alergias,
         padecimientos    = excluded.padecimientos,
         medicamentos     = excluded.medicamentos,
         actualizado      = now();
end $$;

grant execute on function datos_salud_personal(text, bigint) to anon, authenticated;
grant execute on function guardar_datos_salud_personal(text, bigint, date, text, text, text, text) to anon, authenticated;

notify pgrst, 'reload schema';

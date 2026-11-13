-- =====================================================================
--  AXOLOTL FITNESS CLUB · Fecha de nacimiento y datos de salud del cliente
--  Requiere sql/pendientes_noviembre.sql (_tiene_rol, _id_usuario, _hoy_fecha_mx)
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  Datos que se capturan al registrar o editar a un cliente:
--    fecha de nacimiento, tipo de sangre, alergias, padecimientos/lesiones
--    y medicamentos.
--  Son datos sensibles: NO están en clientes_lista. Solo Recepción y el
--  Encargado los leen y guardan, con las funciones de abajo (como el
--  contacto de emergencia).
-- =====================================================================

alter table clientes add column if not exists fecha_nacimiento date;
alter table clientes add column if not exists tipo_sangre      text;
alter table clientes add column if not exists alergias         text;
alter table clientes add column if not exists padecimientos    text;
alter table clientes add column if not exists medicamentos     text;

alter table clientes drop constraint if exists clientes_tipo_sangre_valido;
alter table clientes add constraint clientes_tipo_sangre_valido
  check (tipo_sangre is null or tipo_sangre in ('A+','A-','B+','B-','AB+','AB-','O+','O-'));

-- ---------------------------------------------------------------------
-- Leer (Recepción y Encargado)
-- ---------------------------------------------------------------------
drop function if exists datos_salud_cliente(text, bigint);
create function datos_salud_cliente(p_actor_gmail text, p_cliente_id bigint)
returns table (r_fecha_nacimiento date, r_tipo_sangre text, r_alergias text,
               r_padecimientos text, r_medicamentos text)
language plpgsql stable security definer set search_path = public as $$
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['RECEPCION', 'ENCARGADO']) then
    raise exception 'Solo Recepción y el Encargado pueden ver los datos de salud';
  end if;
  return query
    select c.fecha_nacimiento, c.tipo_sangre,
           coalesce(c.alergias, '')::text, coalesce(c.padecimientos, '')::text,
           coalesce(c.medicamentos, '')::text
      from clientes c where c.id = p_cliente_id;
end $$;

-- ---------------------------------------------------------------------
-- Guardar (Recepción y Encargado). Textos vacíos se guardan como null.
-- ---------------------------------------------------------------------
drop function if exists guardar_datos_salud(text, bigint, date, text, text, text, text);
create function guardar_datos_salud(
  p_actor_gmail text, p_cliente_id bigint, p_fecha_nacimiento date,
  p_tipo_sangre text default null, p_alergias text default null,
  p_padecimientos text default null, p_medicamentos text default null
)
returns void
language plpgsql security definer set search_path = public as $$
declare v_tipo text := nullif(upper(trim(coalesce(p_tipo_sangre, ''))), '');
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['RECEPCION', 'ENCARGADO']) then
    raise exception 'Solo Recepción y el Encargado pueden guardar los datos de salud';
  end if;
  if not exists (select 1 from clientes where id = p_cliente_id) then
    raise exception 'Cliente no encontrado';
  end if;
  if p_fecha_nacimiento is not null and
     (p_fecha_nacimiento >= _hoy_fecha_mx() or p_fecha_nacimiento < _hoy_fecha_mx() - interval '100 years') then
    raise exception 'Fecha de nacimiento inválida';
  end if;
  if v_tipo is not null and v_tipo not in ('A+','A-','B+','B-','AB+','AB-','O+','O-') then
    raise exception 'Tipo de sangre inválido';
  end if;

  update clientes
     set fecha_nacimiento = p_fecha_nacimiento,
         tipo_sangre      = v_tipo,
         alergias         = nullif(left(trim(coalesce(p_alergias, '')), 300), ''),
         padecimientos    = nullif(left(trim(coalesce(p_padecimientos, '')), 300), ''),
         medicamentos     = nullif(left(trim(coalesce(p_medicamentos, '')), 300), '')
   where id = p_cliente_id;
end $$;

grant execute on function datos_salud_cliente(text, bigint) to anon, authenticated;
grant execute on function guardar_datos_salud(text, bigint, date, text, text, text, text) to anon, authenticated;

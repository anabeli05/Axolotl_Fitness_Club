-- =====================================================================
--  AXOLOTL FITNESS CLUB · Auditoría: clientes eliminados y movimientos por rol
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--  Requiere eliminar_cliente.sql ya corrido.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Clientes eliminados
-- ---------------------------------------------------------------------
create table if not exists clientes_eliminados (
  id_auditoria bigint generated always as identity not null,
  -- Copia exacta de clientes
  id bigint not null,
  usuario_id bigint not null,
  codigo_qr text,
  numero_membresia text not null,
  dentro boolean default false not null,
  ultimo_movimiento timestamp with time zone,
  contacto_emergencia_nombre text,
  contacto_emergencia_telefono text,
  aviso_vencimiento_para date,
  aviso_eliminacion_en date,
  -- Datos del usuario al momento de eliminar (el usuario también se borra)
  nombre text,
  gmail text,
  telefono text,
  fecha_registro date,
  -- Auditoría
  eliminado_en timestamp with time zone default now() not null,
  eliminado_por bigint,           -- usuarios.id, sin FK para que el historial nunca se borre
  eliminado_por_nombre text,
  motivo_eliminacion text
);

do $$ begin
  alter table clientes_eliminados add constraint clientes_eliminados_pkey PRIMARY KEY (id_auditoria);
exception when duplicate_table or invalid_table_definition then null; end $$;

create index if not exists clientes_eliminados_fecha_idx on clientes_eliminados (eliminado_en desc);
create index if not exists clientes_eliminados_numero_idx on clientes_eliminados (numero_membresia);

-- ---------------------------------------------------------------------
-- 2) Movimientos por rol
-- ---------------------------------------------------------------------
create table if not exists movimientos_rol (
  id bigint generated always as identity not null,
  rol_id bigint,
  rol_nombre text,                -- copia del nombre por si el rol se elimina
  usuario_id bigint,
  usuario_nombre text,            -- copia del nombre por si el usuario se elimina
  tipo_accion text not null,
  modulo text not null,
  descripcion text,
  valores_anteriores jsonb,
  valores_nuevos jsonb,
  fecha_hora timestamp with time zone default now() not null
);

do $$ begin
  alter table movimientos_rol add constraint movimientos_rol_pkey PRIMARY KEY (id);
exception when duplicate_table or invalid_table_definition then null; end $$;

do $$ begin
  alter table movimientos_rol add constraint movimientos_rol_tipo_chk CHECK ((tipo_accion = ANY (ARRAY['INSERT'::text, 'UPDATE'::text, 'DELETE'::text, 'CAMBIO_PERMISO'::text, 'LOGIN'::text, 'CANCELACION'::text, 'PAGO'::text, 'OTRO'::text])));
exception when duplicate_object then null; end $$;

do $$ begin
  alter table movimientos_rol add constraint movimientos_rol_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE SET NULL;
exception when duplicate_object then null; end $$;

do $$ begin
  alter table movimientos_rol add constraint movimientos_rol_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL;
exception when duplicate_object then null; end $$;

create index if not exists movimientos_rol_fecha_idx on movimientos_rol (fecha_hora desc);
create index if not exists movimientos_rol_rol_idx on movimientos_rol (rol_nombre, fecha_hora desc);
create index if not exists movimientos_rol_usuario_idx on movimientos_rol (usuario_id, fecha_hora desc);

-- Rol principal de un usuario (si tiene varios). Uso interno.
create or replace function _rol_principal(p_usuario bigint)
returns bigint language sql stable security definer set search_path = public as $$
  select r.id
    from usuario_roles ur join roles r on r.id = ur.rol_id
   where ur.usuario_id = p_usuario
   order by coalesce(array_position(array['ENCARGADO','RECEPCION','INSTRUCTOR','CLIENTE'], r.nombre), 99), r.id
   limit 1
$$;

-- Registra un movimiento. Uso interno desde otras funciones.
create or replace function _registrar_movimiento_rol(
  p_usuario_id bigint,
  p_tipo_accion text,
  p_modulo text,
  p_descripcion text,
  p_valores_anteriores jsonb default null,
  p_valores_nuevos jsonb default null)
returns bigint language plpgsql security definer set search_path = public as $$
declare v_rol bigint; v_id bigint;
begin
  v_rol := _rol_principal(p_usuario_id);
  insert into movimientos_rol (rol_id, rol_nombre, usuario_id, usuario_nombre, tipo_accion, modulo,
                               descripcion, valores_anteriores, valores_nuevos)
  values (v_rol,
          (select nombre from roles where id = v_rol),
          p_usuario_id,
          (select nombre from usuarios where id = p_usuario_id),
          upper(p_tipo_accion), p_modulo, p_descripcion, p_valores_anteriores, p_valores_nuevos)
  returning id into v_id;
  return v_id;
end $$;

-- ---------------------------------------------------------------------
-- Trigger: respaldo automático al borrar de clientes
--   Quién y por qué se leen de la transacción:
--     set_config('app.actor_id', '<usuarios.id>', true)
--     set_config('app.motivo_eliminacion', '<texto>', true)
-- ---------------------------------------------------------------------
create or replace function respaldar_cliente_eliminado()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  v_actor  bigint := nullif(current_setting('app.actor_id', true), '')::bigint;
  v_motivo text   := nullif(current_setting('app.motivo_eliminacion', true), '');
  u usuarios%rowtype;
begin
  select * into u from usuarios where id = OLD.usuario_id;

  insert into clientes_eliminados (
    id, usuario_id, codigo_qr, numero_membresia, dentro, ultimo_movimiento,
    contacto_emergencia_nombre, contacto_emergencia_telefono,
    aviso_vencimiento_para, aviso_eliminacion_en,
    nombre, gmail, telefono, fecha_registro,
    eliminado_por, eliminado_por_nombre, motivo_eliminacion)
  values (
    OLD.id, OLD.usuario_id, OLD.codigo_qr, OLD.numero_membresia, OLD.dentro, OLD.ultimo_movimiento,
    OLD.contacto_emergencia_nombre, OLD.contacto_emergencia_telefono,
    OLD.aviso_vencimiento_para, OLD.aviso_eliminacion_en,
    u.nombre, u.gmail, u.telefono, u.fecha_registro,
    v_actor, (select nombre from usuarios where id = v_actor), v_motivo);

  perform _registrar_movimiento_rol(
    v_actor, 'DELETE', 'clientes',
    'Eliminó al cliente ' || coalesce(u.nombre, '') || ' (' || OLD.numero_membresia || ')'
      || coalesce(' · Motivo: ' || v_motivo, ''),
    to_jsonb(OLD) || jsonb_build_object('nombre', u.nombre, 'gmail', u.gmail, 'telefono', u.telefono),
    null);

  return OLD;
end $$;

drop trigger if exists clientes_respaldar_eliminado on clientes;
CREATE TRIGGER clientes_respaldar_eliminado BEFORE DELETE ON public.clientes FOR EACH ROW EXECUTE FUNCTION respaldar_cliente_eliminado();

-- ---------------------------------------------------------------------
-- eliminar_cliente con motivo (compatible con la app: p_motivo es opcional)
-- ---------------------------------------------------------------------
drop function if exists eliminar_cliente(text, bigint);

create or replace function eliminar_cliente(p_actor_gmail text, p_cliente_id bigint, p_motivo text default null)
returns void language plpgsql security definer set search_path = public as $$
declare v_usr bigint; v_actor bigint;
begin
  v_actor := _id_usuario(p_actor_gmail);
  if not _tiene_rol(v_actor, array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede eliminar clientes';
  end if;
  select usuario_id into v_usr from clientes where id = p_cliente_id for update;
  if v_usr is null then raise exception 'Cliente no encontrado'; end if;
  if not _es_solo_cliente(v_usr) then
    raise exception 'Este usuario también es parte del personal; elimínalo desde Personal';
  end if;
  perform set_config('app.actor_id', v_actor::text, true);
  perform set_config('app.motivo_eliminacion', coalesce(nullif(trim(p_motivo), ''), 'Eliminado por el Encargado'), true);
  perform _eliminar_cliente(p_cliente_id);
end $$;

create or replace function eliminar_clientes_inactivos()
returns integer
language plpgsql security definer set search_path = public as $$
declare r record; n int := 0;
begin
  perform set_config('app.actor_id', '', true);
  perform set_config('app.motivo_eliminacion', 'Eliminación automática: 365 días sin uso', true);
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

-- ---------------------------------------------------------------------
-- Vistas
-- ---------------------------------------------------------------------
create or replace view movimientos_por_rol as
 SELECT m.id,
    COALESCE(r.nombre, m.rol_nombre, 'SIN ROL'::text) AS rol,
    m.usuario_id,
    COALESCE(u.nombre, m.usuario_nombre, 'Sistema'::text) AS usuario,
    u.gmail,
    m.tipo_accion,
    m.modulo,
    m.descripcion,
    m.valores_anteriores,
    m.valores_nuevos,
    m.fecha_hora,
    (m.fecha_hora AT TIME ZONE 'America/Mexico_City'::text) AS fecha_hora_mx
   FROM ((movimientos_rol m
     LEFT JOIN roles r ON ((r.id = m.rol_id)))
     LEFT JOIN usuarios u ON ((u.id = m.usuario_id)));

create or replace view resumen_movimientos_rol as
 SELECT rol,
    tipo_accion,
    modulo,
    count(*) AS total,
    max(fecha_hora) AS ultimo_movimiento
   FROM movimientos_por_rol
  GROUP BY rol, tipo_accion, modulo
  ORDER BY rol, total DESC;

-- Consulta para la app: movimientos filtrados por rol (null = todos), búsqueda por
-- nombre / correo / descripción (null = sin filtro) y por bloques
drop function if exists movimientos_de_rol(text, text, integer);
drop function if exists movimientos_de_rol(text, text, integer, integer);

create or replace function movimientos_de_rol(
  p_actor_gmail text,
  p_rol text default null,
  p_limite integer default 50,
  p_offset integer default 0,
  p_buscar text default null)
returns setof movimientos_por_rol
language plpgsql stable security definer set search_path = public as $$
declare v_q text := nullif(trim(coalesce(p_buscar, '')), '');
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede ver los movimientos';
  end if;
  return query
    select * from movimientos_por_rol m
     where (p_rol is null or m.rol = upper(p_rol))
       and (v_q is null
            or m.usuario ilike '%' || v_q || '%'
            or m.gmail ilike '%' || v_q || '%'
            or m.descripcion ilike '%' || v_q || '%')
     order by m.fecha_hora desc, m.id desc
     limit least(greatest(coalesce(p_limite, 50), 1), 500)
    offset greatest(coalesce(p_offset, 0), 0);
end $$;

-- ---------------------------------------------------------------------
-- Seguridad (la app solo accede por funciones)
-- ---------------------------------------------------------------------
alter table clientes_eliminados enable row level security;
alter table movimientos_rol enable row level security;

revoke all on clientes_eliminados, movimientos_rol from anon, authenticated;
revoke all on movimientos_por_rol, resumen_movimientos_rol from anon, authenticated;

revoke all on function _rol_principal(bigint) from public, anon, authenticated;
revoke all on function _registrar_movimiento_rol(bigint, text, text, text, jsonb, jsonb) from public, anon, authenticated;
revoke all on function respaldar_cliente_eliminado() from public, anon, authenticated;
revoke all on function eliminar_clientes_inactivos() from public, anon, authenticated;
grant execute on function eliminar_clientes_inactivos() to service_role;
grant execute on function eliminar_cliente(text, bigint, text) to anon, authenticated;
grant execute on function movimientos_de_rol(text, text, integer, integer, text) to anon, authenticated;

notify pgrst, 'reload schema';

-- Ejemplos:
-- select * from movimientos_por_rol where rol = 'RECEPCION' order by fecha_hora desc;
-- select * from resumen_movimientos_rol where rol = 'ENCARGADO';
-- select * from clientes_eliminados order by eliminado_en desc;

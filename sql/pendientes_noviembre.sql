-- =====================================================================
--  AXOLOTL FITNESS CLUB · Pendientes de noviembre 2026
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  1. Bloqueo de 15 min tras 5 intentos (corrige el contador y cambiar contraseña)
--  4. Cancelaciones y reembolsos de membresías (solo Encargado) y cancelación de asesorías
--  5/10. Contacto de emergencia: solo nombre y teléfono (Recepción y Encargado)
--  6. Se quitan clientes.fecha_nacimiento y clientes.condiciones_medicas
--  12. La vigencia cuenta desde la confirmación del pago
--  2/3. Datos para los avisos por correo y la eliminación tras 365 días sin uso
--       (el envío lo hace la Edge Function avisos-diarios; ver sql/programar_avisos.sql)
--
--  No crea tablas nuevas: solo columnas, vistas y funciones.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. Columnas nuevas
-- ---------------------------------------------------------------------
alter table usuarios add column if not exists ultimo_acceso timestamptz;

alter table clientes add column if not exists contacto_emergencia_nombre   text;
alter table clientes add column if not exists contacto_emergencia_telefono text;
alter table clientes add column if not exists aviso_vencimiento_para date;  -- fecha_fin ya avisada
alter table clientes add column if not exists aviso_eliminacion_en  date;   -- día que se avisó

alter table pagos add column if not exists tipo   text not null default 'PAGO';      -- PAGO | REEMBOLSO
alter table pagos add column if not exists estado text not null default 'ACEPTADO';  -- PENDIENTE | ACEPTADO | RECHAZADO
alter table pagos add column if not exists confirmado_por bigint references usuarios(id) on delete set null;
alter table pagos add column if not exists confirmado_en  timestamptz;
-- Para conservar los ingresos cuando se elimina a un cliente
alter table pagos alter column cliente_id drop not null;

alter table membresias_cliente add column if not exists cancelada_en  timestamptz;
alter table membresias_cliente add column if not exists cancelada_por bigint references usuarios(id) on delete set null;
alter table membresias_cliente add column if not exists motivo_cancelacion text;

alter table solicitudes_asesoria add column if not exists cancelada_en  timestamptz;
alter table solicitudes_asesoria add column if not exists cancelada_por bigint references usuarios(id) on delete set null;
alter table solicitudes_asesoria add column if not exists motivo_cancelacion text;

-- Quita reglas CHECK viejas de "estado" para permitir los estados nuevos
do $$
declare c record;
begin
  for c in
    select conrelid::regclass as tabla, conname from pg_constraint
     where conrelid in ('public.membresias_cliente'::regclass, 'public.solicitudes_asesoria'::regclass,
                        'public.pagos'::regclass)
       and contype = 'c'
       and (pg_get_constraintdef(oid) ilike '%estado%' or pg_get_constraintdef(oid) ilike '%tipo%')
  loop
    execute format('alter table %s drop constraint %I', c.tabla, c.conname);
  end loop;
end $$;

alter table membresias_cliente add constraint membresias_estado_chk
  check (estado in ('ACTIVA', 'PENDIENTE', 'RECHAZADA', 'CANCELADA', 'VENCIDA')) not valid;
alter table solicitudes_asesoria add constraint solicitudes_estado_chk
  check (estado in ('PENDIENTE', 'APROBADA', 'RECHAZADA', 'CANCELADA')) not valid;
alter table pagos add constraint pagos_tipo_chk   check (tipo in ('PAGO', 'REEMBOLSO')) not valid;
alter table pagos add constraint pagos_estado_chk check (estado in ('PENDIENTE', 'ACEPTADO', 'RECHAZADO')) not valid;

-- ---------------------------------------------------------------------
-- 6 / 5 / 10. Datos del cliente
-- El texto libre que hubiera en contacto_emergencia pasa al nombre.
-- ---------------------------------------------------------------------
do $$
begin
  if exists (select 1 from information_schema.columns
              where table_schema = 'public' and table_name = 'clientes'
                and column_name = 'contacto_emergencia') then
    update clientes set contacto_emergencia_nombre = left(trim(contacto_emergencia), 60)
     where contacto_emergencia_nombre is null and coalesce(trim(contacto_emergencia), '') <> '';
  end if;
end $$;

alter table clientes drop column if exists contacto_emergencia;
alter table clientes drop column if exists fecha_nacimiento;
alter table clientes drop column if exists condiciones_medicas;

-- ---------------------------------------------------------------------
-- 1. Bloqueo de la cuenta
--  · Al terminar el bloqueo el contador vuelve a 0 (antes, 1 error volvía a bloquear).
--  · Cambiar contraseña también cuenta los intentos.
--  · Un RAISE deshace los cambios, por eso los errores de contraseña NO usan raise.
-- ---------------------------------------------------------------------
create or replace function _verificar_contrasena(p_id bigint, p_password text)
returns boolean
language plpgsql security definer set search_path = public, extensions as $$
declare u usuarios%rowtype;
begin
  select * into u from usuarios where id = p_id for update;
  if not found then return false; end if;

  if u.bloqueado_hasta is not null and u.bloqueado_hasta > now() then
    raise exception 'Cuenta bloqueada temporalmente. Intenta de nuevo en % min',
      greatest(1, ceil(extract(epoch from u.bloqueado_hasta - now()) / 60))::int;
  end if;

  -- Ya pasó el bloqueo: se empieza a contar de nuevo
  if u.bloqueado_hasta is not null then u.intentos_fallidos := 0; end if;

  if u.contrasena = crypt(coalesce(p_password, ''), u.contrasena) then
    update usuarios set intentos_fallidos = 0, bloqueado_hasta = null where id = p_id;
    return true;
  end if;

  update usuarios
     set intentos_fallidos = u.intentos_fallidos + 1,
         bloqueado_hasta = case when u.intentos_fallidos + 1 >= 5
                                then now() + interval '15 minutes' end
   where id = p_id;
  return false;
end $$;
revoke all on function _verificar_contrasena(bigint, text) from public, anon, authenticated;

create or replace function login_auto(p_gmail text, p_password text)
returns table (id bigint, nombre text, gmail text, rol text, codigo_qr text)
language plpgsql security definer set search_path = public, extensions as $$
declare u usuarios%rowtype;
begin
  select * into u from usuarios x where lower(x.gmail) = lower(trim(p_gmail));
  if not found or u.estado <> 'ACTIVO' then return; end if;
  if not _verificar_contrasena(u.id, p_password) then return; end if;

  update usuarios x set ultimo_acceso = now() where x.id = u.id;

  return query
    select u.id, u.nombre, u.gmail, r.nombre, c.codigo_qr
      from usuario_roles ur
      join roles r on r.id = ur.rol_id
      left join clientes c on c.usuario_id = u.id
     where ur.usuario_id = u.id
     order by case r.nombre when 'ENCARGADO' then 1 when 'RECEPCION' then 2
                            when 'INSTRUCTOR' then 3 else 4 end
     limit 1;
end $$;

create or replace function login(p_gmail text, p_password text, p_rol text)
returns table (id bigint, nombre text, gmail text, rol text, codigo_qr text)
language plpgsql security definer set search_path = public, extensions as $$
declare u usuarios%rowtype;
begin
  select * into u from usuarios x where lower(x.gmail) = lower(p_gmail);
  if not found or u.estado <> 'ACTIVO' then return; end if;
  if not _verificar_contrasena(u.id, p_password) then return; end if;

  update usuarios x set ultimo_acceso = now() where x.id = u.id;

  return query
    select u.id, u.nombre, u.gmail, r.nombre, c.codigo_qr
      from usuario_roles ur
      join roles r on r.id = ur.rol_id
      left join clientes c on c.usuario_id = u.id
     where ur.usuario_id = u.id and r.nombre = p_rol;
end $$;

-- Regresa 'OK' o el mensaje de error (sin raise, para que el intento fallido se guarde)
drop function if exists cambiar_contrasena(text, text, text);
create function cambiar_contrasena(p_gmail text, p_actual text, p_nueva text)
returns text
language plpgsql security definer set search_path = public, extensions as $$
declare v_id bigint;
begin
  select id into v_id from usuarios
   where lower(gmail) = lower(trim(p_gmail)) and estado = 'ACTIVO';
  if v_id is null or not _verificar_contrasena(v_id, p_actual) then
    return 'Correo o contraseña actual incorrectos';
  end if;
  if p_nueva = p_actual then
    return 'La nueva contraseña debe ser diferente a la actual';
  end if;
  if p_nueva = 'Axolotl2026' then
    return 'Elige una contraseña distinta a la temporal';
  end if;

  perform validar_password(p_nueva);
  update usuarios set contrasena = p_nueva, intentos_fallidos = 0, bloqueado_hasta = null
   where id = v_id;
  return 'OK';
end $$;

-- ---------------------------------------------------------------------
-- Utilidades
-- ---------------------------------------------------------------------
create or replace function _hoy_fecha_mx()
returns date language sql stable
as $$ select (now() at time zone 'America/Mexico_City')::date $$;

create or replace function _id_usuario(p_gmail text)
returns bigint language sql stable security definer set search_path = public
as $$ select id from usuarios where lower(gmail) = lower(trim(coalesce(p_gmail, ''))) $$;

create or replace function _tiene_rol(p_usuario bigint, p_roles text[])
returns boolean language sql stable security definer set search_path = public
as $$
  select exists (select 1 from usuario_roles ur join roles r on r.id = ur.rol_id
                  where ur.usuario_id = p_usuario and r.nombre = any (p_roles))
$$;

create or replace function _validar_contacto(p_nombre text, p_telefono text)
returns void language plpgsql immutable as $$
begin
  if coalesce(trim(p_nombre), '') = '' and coalesce(trim(p_telefono), '') = '' then return; end if;
  if length(trim(coalesce(p_nombre, ''))) < 3 then
    raise exception 'Escribe el nombre del contacto de emergencia';
  end if;
  if coalesce(p_telefono, '') !~ '^[0-9]{10}$' then
    raise exception 'El teléfono del contacto de emergencia debe tener 10 dígitos';
  end if;
end $$;

-- ---------------------------------------------------------------------
-- 12. Vigencia desde la confirmación del pago
--  · Efectivo / Tarjeta: quien registra confirma en ese momento → la vigencia empieza ya.
--  · Transferencia: el pago queda PENDIENTE y la membresía no está activa
--    hasta que el Encargado la confirma; la vigencia empieza ese día.
--  · Renovar antes de vencer: empieza al día siguiente del vencimiento actual
--    (antes se sumaba un día de más).
-- ---------------------------------------------------------------------
create or replace function _activar_membresia(p_mem bigint)
returns date
language plpgsql security definer set search_path = public as $$
declare
  v_cli bigint; v_dur int; v_ultima date; v_ini date; v_fin date;
  v_hoy date := _hoy_fecha_mx();
begin
  select mc.cliente_id, t.duracion_dias into v_cli, v_dur
    from membresias_cliente mc join tipos_membresia t on t.id = mc.tipo_membresia_id
   where mc.id = p_mem;

  select max(fecha_fin) into v_ultima from membresias_cliente
   where cliente_id = v_cli and estado = 'ACTIVA' and id <> p_mem;

  if v_ultima is not null and v_ultima >= v_hoy then
    v_ini := v_ultima + 1;
    v_fin := v_ultima + v_dur;
  else
    v_ini := v_hoy;
    v_fin := v_hoy + v_dur;
  end if;

  update membresias_cliente set estado = 'ACTIVA', fecha_inicio = v_ini, fecha_fin = v_fin
   where id = p_mem;
  return v_fin;
end $$;
revoke all on function _activar_membresia(bigint) from public, anon, authenticated;

-- Crea la membresía con su pago. Regresa la fecha de vencimiento o null si queda pendiente.
create or replace function _nueva_membresia(p_cli bigint, p_plan text, p_metodo text, p_quien bigint)
returns date
language plpgsql security definer set search_path = public as $$
declare v_tipo tipos_membresia%rowtype; v_mem bigint; v_hoy date := _hoy_fecha_mx();
begin
  if p_metodo not in ('Efectivo', 'Tarjeta', 'Transferencia') then
    raise exception 'Método de pago no válido';
  end if;
  select * into v_tipo from tipos_membresia where nombre = p_plan;
  if not found then raise exception 'Plan no válido'; end if;

  insert into membresias_cliente (cliente_id, tipo_membresia_id, fecha_inicio, fecha_fin, estado)
  values (p_cli, v_tipo.id, v_hoy, v_hoy, 'PENDIENTE')
  returning id into v_mem;

  if p_metodo = 'Transferencia' then
    insert into pagos (monto, metodo_pago, membresia_cliente_id, cliente_id, registrado_por, estado)
    values (v_tipo.precio, p_metodo, v_mem, p_cli, p_quien, 'PENDIENTE');
    return null;
  end if;

  insert into pagos (monto, metodo_pago, membresia_cliente_id, cliente_id, registrado_por,
                     estado, confirmado_por, confirmado_en)
  values (v_tipo.precio, p_metodo, v_mem, p_cli, p_quien, 'ACEPTADO', p_quien, now());
  return _activar_membresia(v_mem);
end $$;
revoke all on function _nueva_membresia(bigint, text, text, bigint) from public, anon, authenticated;

drop function if exists crear_cliente(text, text, text, text, text);
drop function if exists crear_cliente(text, text, text, text, text, text, text, text);
create function crear_cliente(
  p_nombre text, p_telefono text, p_gmail text, p_plan text, p_registrado_por text,
  p_contacto_nombre text default null, p_contacto_telefono text default null,
  p_metodo text default 'Efectivo'
)
returns table (r_id bigint, r_numero text, r_qr text, r_vence date)
language plpgsql security definer set search_path = public, extensions as $$
declare v_user bigint; v_cli bigint; v_fin date;
begin
  if exists (select 1 from usuarios where lower(gmail) = lower(trim(p_gmail))) then
    raise exception 'Ese correo ya está registrado';
  end if;
  perform _validar_contacto(p_contacto_nombre, p_contacto_telefono);

  insert into usuarios (nombre, contrasena, gmail, telefono)
  values (trim(p_nombre), 'Axolotl2026', lower(trim(p_gmail)), coalesce(p_telefono, ''))
  returning id into v_user;

  insert into usuario_roles (usuario_id, rol_id)
  select v_user, id from roles where nombre = 'CLIENTE';

  insert into clientes (usuario_id, contacto_emergencia_nombre, contacto_emergencia_telefono)
  values (v_user, nullif(trim(coalesce(p_contacto_nombre, '')), ''),
                  nullif(trim(coalesce(p_contacto_telefono, '')), ''))
  returning id into v_cli;

  if coalesce(p_plan, '') <> '' then
    v_fin := _nueva_membresia(v_cli, p_plan, coalesce(p_metodo, 'Efectivo'), _id_usuario(p_registrado_por));
  end if;

  return query select c.id, c.numero_membresia, c.codigo_qr, v_fin from clientes c where c.id = v_cli;
end $$;

drop function if exists renovar_membresia(bigint, text, text);
drop function if exists renovar_membresia(bigint, text, text, text);
create function renovar_membresia(
  p_cliente_id bigint, p_plan text, p_registrado_por text, p_metodo text default 'Efectivo'
)
returns date
language plpgsql security definer set search_path = public, extensions as $$
begin
  if not exists (select 1 from clientes where id = p_cliente_id) then
    raise exception 'Cliente no encontrado';
  end if;
  if exists (select 1 from membresias_cliente where cliente_id = p_cliente_id and estado = 'PENDIENTE') then
    raise exception 'El cliente ya tiene un pago por transferencia pendiente de confirmar';
  end if;
  return _nueva_membresia(p_cliente_id, p_plan, coalesce(p_metodo, 'Efectivo'), _id_usuario(p_registrado_por));
end $$;

-- Confirmar o rechazar un pago pendiente.
-- Transferencias: solo Encargado. Efectivo / Tarjeta: Recepción o Encargado.
drop function if exists confirmar_pago(text, bigint, boolean);
create function confirmar_pago(p_actor_gmail text, p_pago_id bigint, p_aceptar boolean)
returns date
language plpgsql security definer set search_path = public as $$
declare v_actor bigint; p pagos%rowtype;
begin
  v_actor := _id_usuario(p_actor_gmail);
  select * into p from pagos where id = p_pago_id for update;
  if not found or p.estado <> 'PENDIENTE' then
    raise exception 'El pago ya fue resuelto o no existe';
  end if;
  if not _tiene_rol(v_actor, array['ENCARGADO'])
     and not (p.metodo_pago <> 'Transferencia' and _tiene_rol(v_actor, array['RECEPCION'])) then
    raise exception 'Solo el Encargado puede confirmar transferencias';
  end if;

  if not p_aceptar then
    update pagos set estado = 'RECHAZADO', confirmado_por = v_actor, confirmado_en = now() where id = p.id;
    update membresias_cliente set estado = 'RECHAZADA' where id = p.membresia_cliente_id;
    return null;
  end if;

  -- El ingreso cuenta el día en que se confirma
  update pagos set estado = 'ACEPTADO', confirmado_por = v_actor, confirmado_en = now(), fecha_pago = now()
   where id = p.id;
  return _activar_membresia(p.membresia_cliente_id);
end $$;

drop view if exists pagos_pendientes;
create view pagos_pendientes as
select p.id, p.monto::double precision as monto, p.metodo_pago, p.fecha_pago,
       c.id as cliente_id, u.nombre as cliente, c.numero_membresia, t.nombre as plan
  from pagos p
  join clientes c on c.id = p.cliente_id
  join usuarios u on u.id = c.usuario_id
  left join membresias_cliente mc on mc.id = p.membresia_cliente_id
  left join tipos_membresia t on t.id = mc.tipo_membresia_id
 where p.estado = 'PENDIENTE' and p.tipo = 'PAGO';

-- La membresía que se muestra ignora las pendientes y rechazadas
create or replace view clientes_lista as
 select c.id, u.nombre, u.gmail, coalesce(u.telefono, ''::text) as telefono, u.fecha_registro,
        c.codigo_qr, m.plan, m.fecha_fin, coalesce(m.activa, false) as activa,
        c.numero_membresia, (u.estado)::text as estado
   from clientes c
   join usuarios u on u.id = c.usuario_id
   left join lateral (
     select t.nombre as plan, mc.fecha_fin,
            (mc.estado = 'ACTIVA' and mc.fecha_fin >= _hoy_fecha_mx()) as activa
       from membresias_cliente mc join tipos_membresia t on t.id = mc.tipo_membresia_id
      where mc.cliente_id = c.id and mc.estado not in ('PENDIENTE', 'RECHAZADA')
      order by mc.fecha_fin desc, mc.id desc
      limit 1) m on true;

-- Ingresos: solo pagos aceptados; los reembolsos restan
create or replace function _monto_neto(p pagos)
returns numeric language sql immutable
as $$ select case when p.estado <> 'ACEPTADO' then 0
                  when p.tipo = 'REEMBOLSO' then -p.monto else p.monto end $$;

create or replace view resumen_dashboard as
select (select count(*) from clientes c join usuarios u on u.id = c.usuario_id
         where u.estado = 'ACTIVO') as activos,
       (select count(*) from clientes c join usuarios u on u.id = c.usuario_id
         where u.estado = 'INACTIVO') as inactivos,
       coalesce((select sum(_monto_neto(p)) from pagos p
                  where date_trunc('month', p.fecha_pago at time zone 'America/Mexico_City')
                      = date_trunc('month', now() at time zone 'America/Mexico_City')), 0) as ingresos_mes;

create or replace view ingresos_semana as
with h as (select _hoy_fecha_mx() as hoy),
dias as (
  select d::date as dia from h,
         generate_series(date_trunc('week', h.hoy), date_trunc('week', h.hoy) + interval '6 days',
                         interval '1 day') d
)
select d.dia as fecha, extract(isodow from d.dia)::int as dia_semana,
       coalesce(sum(_monto_neto(p)), 0)::numeric as total
  from dias d
  left join pagos p on (p.fecha_pago at time zone 'America/Mexico_City')::date = d.dia
 group by d.dia
 order by d.dia;

create or replace view reporte_mensual as
select to_char(m, 'YYYY-MM') as periodo,
       coalesce((select sum(_monto_neto(p)) from pagos p
                  where date_trunc('month', p.fecha_pago) = m.m), 0) as servicios,
       coalesce((select sum(s.costo) from solicitudes_asesoria s
                  where s.estado = 'APROBADA'
                    and date_trunc('month', s.fecha_solicitud::timestamptz) = m.m), 0) as asesorias,
       coalesce((select sum(g.monto) from gastos g
                  where date_trunc('month', g.fecha::timestamptz) = m.m), 0) as gastos
  from generate_series(date_trunc('month', current_date::timestamptz) - interval '5 mons',
                       date_trunc('month', current_date::timestamptz), interval '1 mon') m(m);

-- ---------------------------------------------------------------------
-- 5 / 10. Contacto de emergencia (solo nombre y teléfono)
-- No está en clientes_lista: solo lo leen Recepción y Encargado con esta función.
-- ---------------------------------------------------------------------
drop function if exists contacto_emergencia_cliente(text, bigint);
create function contacto_emergencia_cliente(p_actor_gmail text, p_cliente_id bigint)
returns table (r_nombre text, r_telefono text)
language plpgsql stable security definer set search_path = public as $$
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['RECEPCION', 'ENCARGADO']) then
    raise exception 'Solo Recepción y el Encargado pueden ver el contacto de emergencia';
  end if;
  return query
    select coalesce(c.contacto_emergencia_nombre, '')::text, coalesce(c.contacto_emergencia_telefono, '')::text
      from clientes c where c.id = p_cliente_id;
end $$;

-- p_contacto_* en null = se conserva lo que había (así lo usa el perfil del cliente)
drop function if exists editar_cliente(bigint, text, text, text, text);
drop function if exists editar_cliente(bigint, text, text, text, text, text, text);
create function editar_cliente(
  p_id bigint, p_nombre text, p_telefono text, p_gmail text, p_estado text,
  p_contacto_nombre text default null, p_contacto_telefono text default null
)
returns void
language plpgsql security definer set search_path = public, extensions as $$
declare v_user bigint;
begin
  if p_estado not in ('ACTIVO', 'INACTIVO') then
    raise exception 'Estado no permitido';
  end if;
  select usuario_id into v_user from clientes where id = p_id;
  if v_user is null then raise exception 'Cliente no encontrado'; end if;
  if exists (select 1 from usuarios where lower(gmail) = lower(trim(p_gmail)) and id <> v_user) then
    raise exception 'Ese correo ya está registrado';
  end if;

  update usuarios
     set nombre = trim(p_nombre), telefono = coalesce(p_telefono, ''),
         gmail = lower(trim(p_gmail)), estado = p_estado::estado_t
   where id = v_user;

  if p_contacto_nombre is not null or p_contacto_telefono is not null then
    perform _validar_contacto(p_contacto_nombre, p_contacto_telefono);
    update clientes
       set contacto_emergencia_nombre = nullif(trim(coalesce(p_contacto_nombre, '')), ''),
           contacto_emergencia_telefono = nullif(trim(coalesce(p_contacto_telefono, '')), '')
     where id = p_id;
  end if;
end $$;

-- ---------------------------------------------------------------------
-- 4. Cancelaciones y reembolsos
-- ---------------------------------------------------------------------
-- Membresía: solo el Encargado, con su contraseña. Cancela la vigente (y las ya pagadas
-- por adelantado). El reembolso no puede ser mayor a lo pagado por esas membresías.
drop function if exists cancelar_membresia(text, text, bigint, text, numeric);
create function cancelar_membresia(
  p_actor_gmail text, p_password text, p_cliente_id bigint, p_motivo text, p_reembolso numeric default 0
)
returns void
language plpgsql security definer set search_path = public, extensions as $$
declare
  v_actor bigint; v_ids bigint[]; v_pagado numeric; v_metodo text;
  v_reemb numeric := coalesce(p_reembolso, 0);
begin
  v_actor := validar_encargado(p_actor_gmail, p_password);   -- lanza error si no es Encargado

  if length(trim(coalesce(p_motivo, ''))) < 3 then
    raise exception 'Escribe el motivo de la cancelación';
  end if;

  select array_agg(id) into v_ids from membresias_cliente
   where cliente_id = p_cliente_id and estado = 'ACTIVA' and fecha_fin >= _hoy_fecha_mx();
  if v_ids is null then
    raise exception 'El cliente no tiene una membresía vigente';
  end if;

  select coalesce(sum(_monto_neto(p)), 0) into v_pagado
    from pagos p where p.membresia_cliente_id = any (v_ids);
  if v_reemb < 0 or v_reemb > v_pagado then
    raise exception 'El reembolso debe estar entre 0 y %', v_pagado;
  end if;

  update membresias_cliente
     set estado = 'CANCELADA', cancelada_en = now(), cancelada_por = v_actor,
         motivo_cancelacion = trim(p_motivo)
   where id = any (v_ids);

  if v_reemb > 0 then
    select metodo_pago into v_metodo from pagos
     where membresia_cliente_id = any (v_ids) and tipo = 'PAGO' and estado = 'ACEPTADO'
     order by fecha_pago desc limit 1;
    insert into pagos (monto, metodo_pago, membresia_cliente_id, cliente_id, registrado_por,
                       tipo, estado, confirmado_por, confirmado_en)
    values (v_reemb, coalesce(v_metodo, 'Efectivo'), v_ids[1], p_cliente_id, v_actor,
            'REEMBOLSO', 'ACEPTADO', v_actor, now());
  end if;
end $$;

-- Asesorías: el cliente cancela su solicitud pendiente (ya no se borra, queda registrada)
create or replace function cancelar_solicitud(p_gmail text, p_instructor_id bigint)
returns void
language plpgsql security definer set search_path = public as $$
begin
  update solicitudes_asesoria s
     set estado = 'CANCELADA', cancelada_en = now(), cancelada_por = u.id,
         motivo_cancelacion = 'Cancelada por el cliente'
    from clientes c, usuarios u
   where s.cliente_id = c.id and u.id = c.usuario_id
     and lower(u.gmail) = lower(p_gmail)
     and s.instructor_id = p_instructor_id
     and s.estado = 'PENDIENTE';
end $$;

-- Recepción o Encargado cancelan una asesoría pendiente o aprobada
drop function if exists cancelar_asesoria(text, bigint, text);
create function cancelar_asesoria(p_actor_gmail text, p_id bigint, p_motivo text)
returns void
language plpgsql security definer set search_path = public as $$
declare v_actor bigint := _id_usuario(p_actor_gmail);
begin
  if not _tiene_rol(v_actor, array['RECEPCION', 'ENCARGADO']) then
    raise exception 'Solo Recepción o el Encargado pueden cancelar asesorías';
  end if;
  if length(trim(coalesce(p_motivo, ''))) < 3 then
    raise exception 'Escribe el motivo de la cancelación';
  end if;
  update solicitudes_asesoria
     set estado = 'CANCELADA', cancelada_en = now(), cancelada_por = v_actor,
         motivo_cancelacion = trim(p_motivo)
   where id = p_id and estado in ('PENDIENTE', 'APROBADA');
  if not found then
    raise exception 'La asesoría ya fue cancelada o resuelta';
  end if;
end $$;

-- Registro de cancelaciones (membresías y asesorías)
drop view if exists cancelaciones_lista;
create view cancelaciones_lista as
select 'MEMBRESIA'::text as tipo, mc.id, mc.cancelada_en, uc.nombre as cliente, c.numero_membresia,
       t.nombre as detalle, mc.motivo_cancelacion as motivo, ua.nombre as autorizo,
       coalesce((select sum(p.monto) from pagos p
                  where p.membresia_cliente_id = mc.id and p.tipo = 'REEMBOLSO'), 0)::double precision as reembolso
  from membresias_cliente mc
  join clientes c on c.id = mc.cliente_id
  join usuarios uc on uc.id = c.usuario_id
  join tipos_membresia t on t.id = mc.tipo_membresia_id
  left join usuarios ua on ua.id = mc.cancelada_por
 where mc.estado = 'CANCELADA'
union all
select 'ASESORIA', s.id, s.cancelada_en, uc.nombre, c.numero_membresia,
       'Asesoría con ' || ui.nombre, s.motivo_cancelacion, ua.nombre, 0
  from solicitudes_asesoria s
  join clientes c on c.id = s.cliente_id
  join usuarios uc on uc.id = c.usuario_id
  join instructores i on i.id = s.instructor_id
  join usuarios ui on ui.id = i.usuario_id
  left join usuarios ua on ua.id = s.cancelada_por
 where s.estado = 'CANCELADA';

-- ---------------------------------------------------------------------
-- 2 / 3. Avisos por correo y eliminación tras 365 días sin uso
-- "Uso" = último inicio de sesión, último acceso al gimnasio, último día con
-- membresía vigente o fecha de registro (lo más reciente).
-- Estas funciones solo las llama la Edge Function avisos-diarios (service_role).
-- ---------------------------------------------------------------------
create or replace function _ultimo_uso(p_cliente_id bigint)
returns date language sql stable security definer set search_path = public as $$
  select greatest(
           u.fecha_registro,
           (u.ultimo_acceso at time zone 'America/Mexico_City')::date,
           (c.ultimo_movimiento at time zone 'America/Mexico_City')::date,
           (select max(mc.fecha_fin) from membresias_cliente mc
             where mc.cliente_id = c.id and mc.estado = 'ACTIVA'))
    from clientes c join usuarios u on u.id = c.usuario_id
   where c.id = p_cliente_id
$$;

-- Solo clientes (nunca personal)
create or replace function _es_solo_cliente(p_usuario bigint)
returns boolean language sql stable security definer set search_path = public as $$
  select _tiene_rol(p_usuario, array['CLIENTE'])
     and not _tiene_rol(p_usuario, array['RECEPCION', 'INSTRUCTOR', 'ENCARGADO'])
$$;

drop function if exists avisos_pendientes();
create function avisos_pendientes()
returns table (r_tipo text, r_cliente_id bigint, r_nombre text, r_gmail text, r_plan text, r_fecha date)
language sql stable security definer set search_path = public as $$
  -- Vencimiento: 3 días antes (planes de 7 días o más), una sola vez por vencimiento
  select 'VENCIMIENTO'::text, c.id, u.nombre, u.gmail, t.nombre, mc.fecha_fin
    from clientes c
    join usuarios u on u.id = c.usuario_id
    join lateral (select * from membresias_cliente x
                   where x.cliente_id = c.id and x.estado = 'ACTIVA'
                   order by x.fecha_fin desc limit 1) mc on true
    join tipos_membresia t on t.id = mc.tipo_membresia_id
   where u.estado = 'ACTIVO'
     and t.duracion_dias >= 7
     and mc.fecha_fin between _hoy_fecha_mx() and _hoy_fecha_mx() + 3
     and c.aviso_vencimiento_para is distinct from mc.fecha_fin
  union all
  -- Eliminación: 30 días antes de cumplir 365 días sin uso
  select 'ELIMINACION'::text, c.id, u.nombre, u.gmail, null::text,
         greatest(_ultimo_uso(c.id) + 365, _hoy_fecha_mx() + 30)
    from clientes c
    join usuarios u on u.id = c.usuario_id
   where _es_solo_cliente(u.id)
     and _ultimo_uso(c.id) <= _hoy_fecha_mx() - 335
     and (c.aviso_eliminacion_en is null or c.aviso_eliminacion_en < _ultimo_uso(c.id))
$$;

drop function if exists marcar_aviso(bigint, text, date);
create function marcar_aviso(p_cliente_id bigint, p_tipo text, p_fecha date)
returns void language sql security definer set search_path = public as $$
  update clientes
     set aviso_vencimiento_para = case when p_tipo = 'VENCIMIENTO' then p_fecha else aviso_vencimiento_para end,
         aviso_eliminacion_en   = case when p_tipo = 'ELIMINACION' then _hoy_fecha_mx() else aviso_eliminacion_en end
   where id = p_cliente_id
$$;

-- Elimina a los clientes con 365 días sin uso a los que se avisó hace 30 días o más.
-- Los pagos se conservan (sin cliente) para no alterar los reportes de ingresos.
drop function if exists eliminar_clientes_inactivos();
create function eliminar_clientes_inactivos()
returns integer
language plpgsql security definer set search_path = public as $$
declare r record; n int := 0;
begin
  for r in
    select c.id as cli, c.usuario_id as usr
      from clientes c
     where _es_solo_cliente(c.usuario_id)
       and _ultimo_uso(c.id) <= _hoy_fecha_mx() - 365
       and c.aviso_eliminacion_en is not null
       and c.aviso_eliminacion_en >= _ultimo_uso(c.id)
       and c.aviso_eliminacion_en <= _hoy_fecha_mx() - 30
  loop
    update pagos set cliente_id = null, membresia_cliente_id = null where cliente_id = r.cli;
    update pagos set membresia_cliente_id = null
     where membresia_cliente_id in (select id from membresias_cliente where cliente_id = r.cli);
    delete from ejercicios_rutina where rutina_id in (select id from rutinas where cliente_id = r.cli);
    delete from rutinas where cliente_id = r.cli;
    delete from solicitudes_asesoria where cliente_id = r.cli;
    delete from asistencias where cliente_id = r.cli;
    delete from registros_acceso where cliente_id = r.cli;
    delete from rostros_clientes where cliente_id = r.cli;
    delete from membresias_cliente where cliente_id = r.cli;
    delete from clientes where id = r.cli;
    delete from usuario_roles where usuario_id = r.usr;
    delete from usuarios where id = r.usr;
    n := n + 1;
  end loop;
  return n;
end $$;

-- ---------------------------------------------------------------------
-- Permisos
-- ---------------------------------------------------------------------
revoke all on function _tiene_rol(bigint, text[]) from public, anon, authenticated;
revoke all on function _id_usuario(text) from public, anon, authenticated;
revoke all on function _ultimo_uso(bigint) from public, anon, authenticated;
revoke all on function _es_solo_cliente(bigint) from public, anon, authenticated;
revoke all on function avisos_pendientes() from public, anon, authenticated;
revoke all on function marcar_aviso(bigint, text, date) from public, anon, authenticated;
revoke all on function eliminar_clientes_inactivos() from public, anon, authenticated;
grant execute on function avisos_pendientes() to service_role;
grant execute on function marcar_aviso(bigint, text, date) to service_role;
grant execute on function eliminar_clientes_inactivos() to service_role;

grant execute on function login_auto(text, text) to anon, authenticated;
grant execute on function login(text, text, text) to anon, authenticated;
grant execute on function cambiar_contrasena(text, text, text) to anon, authenticated;
grant execute on function crear_cliente(text, text, text, text, text, text, text, text) to anon, authenticated;
grant execute on function renovar_membresia(bigint, text, text, text) to anon, authenticated;
grant execute on function confirmar_pago(text, bigint, boolean) to anon, authenticated;
grant execute on function contacto_emergencia_cliente(text, bigint) to anon, authenticated;
grant execute on function editar_cliente(bigint, text, text, text, text, text, text) to anon, authenticated;
grant execute on function cancelar_membresia(text, text, bigint, text, numeric) to anon, authenticated;
grant execute on function cancelar_solicitud(text, bigint) to anon, authenticated;
grant execute on function cancelar_asesoria(text, bigint, text) to anon, authenticated;

grant select on clientes_lista, pagos_pendientes, cancelaciones_lista,
                resumen_dashboard, ingresos_semana, reporte_mensual to anon, authenticated;

notify pgrst, 'reload schema';

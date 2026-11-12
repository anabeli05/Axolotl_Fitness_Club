-- =====================================================================
--  AXOLOTL FITNESS CLUB · Servicios conectados con los planes que se cobran
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  Antes había dos tablas sin relación:
--    servicios        → lo que el Encargado edita en "Servicios"
--    tipos_membresia  → lo que realmente se cobra (Recepción y pagos del cliente)
--  Ahora "Servicios" manda: al crear o editar un servicio, su plan en
--  tipos_membresia toma el mismo nombre, precio y duración. El cliente ve en
--  "Pagos" exactamente los servicios con su precio actual.
--
--  Al eliminar un servicio, su plan se conserva (hay membresías que lo usan),
--  pero deja de aparecer para el cliente.
--  "Visita" no es un servicio: sigue disponible solo en Recepción.
-- =====================================================================

alter table tipos_membresia add column if not exists servicio_id bigint;

do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'tipos_membresia_servicio_fkey') then
    alter table tipos_membresia add constraint tipos_membresia_servicio_fkey
      foreign key (servicio_id) references servicios(id) on delete set null;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'tipos_membresia_servicio_uk') then
    alter table tipos_membresia add constraint tipos_membresia_servicio_uk unique (servicio_id);
  end if;
end $$;

-- Enlace inicial de los planes que ya existían con su servicio
update tipos_membresia t set servicio_id = s.id
  from servicios s
 where t.servicio_id is null
   and not exists (select 1 from tipos_membresia x where x.servicio_id = s.id)
   and (lower(t.nombre) = lower(s.nombre)
        or (lower(t.nombre) = 'mensual' and lower(s.nombre) = 'mensualidad'));

-- "30 días" -> 30
create or replace function _dias_servicio(p_duracion text)
returns integer language sql immutable as $$
  select greatest(coalesce(nullif(regexp_replace(coalesce(p_duracion, ''), '\D', '', 'g'), '')::int, 1), 1)
$$;

-- Copia nombre, precio y duración del servicio a su plan
create or replace function _sincronizar_servicio()
returns trigger language plpgsql security definer set search_path = public as $$
declare v_tipo bigint;
begin
  select id into v_tipo from tipos_membresia where servicio_id = new.id;
  if v_tipo is null then
    -- Un plan con el mismo nombre que aún no tiene servicio se reutiliza
    select id into v_tipo from tipos_membresia
     where servicio_id is null and lower(nombre) = lower(new.nombre) limit 1;
  end if;

  if v_tipo is null then
    insert into tipos_membresia (nombre, precio, duracion_dias, servicio_id)
    values (new.nombre, new.precio, _dias_servicio(new.duracion), new.id);
  else
    update tipos_membresia
       set nombre = new.nombre, precio = new.precio,
           duracion_dias = _dias_servicio(new.duracion), servicio_id = new.id
     where id = v_tipo;
  end if;
  return new;
end $$;

drop trigger if exists servicios_sincronizar on servicios;
create trigger servicios_sincronizar
  after insert or update on servicios
  for each row execute function _sincronizar_servicio();

-- Sincroniza los servicios que ya existen (aquí la Mensualidad toma el precio de Servicios)
update servicios set precio = precio;

-- Lo que el cliente puede pagar: los servicios, con el nombre del plan que se cobra
create or replace view planes_pago as
 select s.id as servicio_id,
    t.nombre,
    t.duracion_dias,
    s.precio::double precision as precio
   from servicios s
   join tipos_membresia t on t.servicio_id = s.id
  order by t.duracion_dias, s.precio;

grant select on planes_pago to anon, authenticated;

notify pgrst, 'reload schema';

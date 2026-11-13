-- =====================================================================
--  AXOLOTL FITNESS CLUB · Para quién se ven los servicios
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--  Requiere haber corrido antes servicios_precios.sql
--
--  1. Recepción leía tipos_membresia completo, así que los servicios que el
--     Encargado eliminaba seguían apareciendo (su plan se conserva porque hay
--     membresías que lo usan, solo queda con servicio_id = null).
--  2. El Encargado elige si cada servicio se ve para Clientes, Recepción o ambos.
--     El Encargado siempre ve todos los servicios.
--  3. Recepción solo ve lo que el Encargado tiene en "Servicios".
--  4. Un servicio puede marcarse como "Visita" (pase de un día). Se crea, edita
--     y elimina como cualquier otro, y también se elige para quién se ve:
--       · Clientes: lo pueden pagar en la app como cualquier plan.
--       · Recepción: lo cobra en la sección "Visitas" (ver visitas.sql).
--     Solo puede haber uno.
-- =====================================================================

alter table servicios add column if not exists visible_clientes  boolean not null default true;
alter table servicios add column if not exists visible_recepcion boolean not null default true;
alter table servicios add column if not exists es_visita         boolean not null default false;

-- Si ya había un servicio llamado "Visita", queda marcado como tal
update servicios set es_visita = true
 where not exists (select 1 from servicios where es_visita)
   and id = (select min(id) from servicios where lower(nombre) = 'visita');

create unique index if not exists servicios_una_visita on servicios (es_visita) where es_visita;

-- Solo el Encargado configura para quién se ve y si es la visita
drop function if exists visibilidad_servicio(text, bigint, boolean, boolean);
create or replace function configurar_servicio(
  p_actor_gmail text, p_id bigint, p_clientes boolean, p_recepcion boolean, p_es_visita boolean
) returns void language plpgsql security definer set search_path = public as $$
begin
  if not _tiene_rol(_id_usuario(p_actor_gmail), array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede configurar los servicios';
  end if;
  -- Solo una visita: si este pasa a serlo, el anterior deja de serlo
  if coalesce(p_es_visita, false) then
    update servicios set es_visita = false where es_visita and id <> p_id;
  end if;
  update servicios
     set visible_clientes = coalesce(p_clientes, true),
         visible_recepcion = coalesce(p_recepcion, true),
         es_visita = coalesce(p_es_visita, false)
   where id = p_id;
end $$;

grant execute on function configurar_servicio(text, bigint, boolean, boolean, boolean) to anon, authenticated;

-- Lo que el cliente puede pagar: servicios visibles para clientes (la visita incluida)
create or replace view planes_pago as
 select s.id as servicio_id,
    t.nombre,
    t.duracion_dias,
    s.precio::double precision as precio
   from servicios s
   join tipos_membresia t on t.servicio_id = s.id
  where s.visible_clientes
  order by t.duracion_dias, s.precio;

grant select on planes_pago to anon, authenticated;

-- Lo que Recepción cobra como membresía: servicios visibles para Recepción
-- (la visita no: Recepción la cobra en la sección "Visitas")
drop view if exists planes_recepcion;
create view planes_recepcion as
 select t.id, t.nombre, t.precio, t.duracion_dias, t.servicio_id
   from tipos_membresia t
   join servicios s on s.id = t.servicio_id
  where s.visible_recepcion and not s.es_visita;

grant select on planes_recepcion to anon, authenticated;

-- El Encargado siempre ve todos los servicios vigentes (la visita se cobra en "Visitas")
drop view if exists planes_encargado;
create view planes_encargado as
 select t.id, t.nombre, t.precio, t.duracion_dias, t.servicio_id
   from tipos_membresia t
   join servicios s on s.id = t.servicio_id
  where not s.es_visita;

grant select on planes_encargado to anon, authenticated;

notify pgrst, 'reload schema';

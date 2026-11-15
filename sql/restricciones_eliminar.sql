-- =====================================================================
--  AXOLOTL FITNESS CLUB · Restricciones para eliminar clientes
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--  Requiere eliminar_cliente.sql ya corrido.
--
--  Al borrar a un cliente (desde la app, o desde el Table Editor de Supabase
--  borrando su fila en usuarios o en clientes) la base borra sola todo lo suyo:
--    · ON DELETE CASCADE (ya estaban así): membresías, accesos, asistencias, rostro,
--      rutinas, asesorías, solicitudes de cancelación, roles.
--    · ON DELETE SET NULL (cambian aquí): los PAGOS se conservan sin cliente para
--      no alterar los reportes de ingresos.
--
--  Antes:
--    · pagos.cliente_id era CASCADE: borrar a un cliente directo en la base BORRABA
--      sus pagos y cambiaba los reportes.
--    · pagos.membresia_cliente_id y pagos.registrado_por no tenían regla: impedían
--      borrar al cliente (error de llave foránea).
--    · visitas.registrado_por tampoco: impedía borrar al personal que cobró visitas.
-- =====================================================================

-- Cambia la regla de la llave foránea de una columna (busca la restricción por columna,
-- así funciona aunque el nombre sea distinto en tu base).
create or replace function _cambiar_fk(p_tabla text, p_columna text, p_ref text, p_regla text)
returns void language plpgsql as $$
declare r record;
begin
  for r in
    select con.conname
      from pg_constraint con
      join pg_attribute a on a.attrelid = con.conrelid and a.attnum = any (con.conkey)
     where con.contype = 'f'
       and con.conrelid = format('public.%I', p_tabla)::regclass
       and array_length(con.conkey, 1) = 1
       and a.attname = p_columna
  loop
    execute format('alter table public.%I drop constraint %I', p_tabla, r.conname);
  end loop;
  execute format('alter table public.%I alter column %I drop not null', p_tabla, p_columna);
  execute format('alter table public.%I add constraint %I foreign key (%I) references public.%s on delete %s',
                 p_tabla, p_tabla || '_' || p_columna || '_fkey', p_columna, p_ref, p_regla);
end $$;

select _cambiar_fk('pagos', 'cliente_id',           'clientes(id)',           'set null');
select _cambiar_fk('pagos', 'membresia_cliente_id', 'membresias_cliente(id)', 'set null');
select _cambiar_fk('pagos', 'registrado_por',       'usuarios(id)',           'set null');
select _cambiar_fk('visitas', 'registrado_por',     'usuarios(id)',           'set null');

drop function _cambiar_fk(text, text, text, text);

-- Con las reglas de arriba basta con borrar el usuario: todo lo demás lo hace la base
create or replace function _eliminar_cliente(p_cliente_id bigint)
returns void language plpgsql security definer set search_path = public as $$
declare v_usr bigint;
begin
  select usuario_id into v_usr from clientes where id = p_cliente_id;
  if v_usr is null then raise exception 'Cliente no encontrado'; end if;
  delete from usuarios where id = v_usr;   -- clientes y todo lo suyo: CASCADE; pagos: SET NULL
end $$;

revoke all on function _eliminar_cliente(bigint) from public, anon, authenticated;

notify pgrst, 'reload schema';

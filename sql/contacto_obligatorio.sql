-- =====================================================================
--  AXOLOTL FITNESS CLUB · Contacto de emergencia obligatorio
--  Requiere sql/pendientes_noviembre.sql
--  Supabase → SQL Editor → pegar → Run (se puede correr varias veces)
--
--  Antes el contacto era opcional: si llegaba vacío no se validaba.
--  Ahora crear_cliente exige nombre (mínimo 3 letras) y teléfono de 10 dígitos.
--  En editar_cliente se valida cuando se mandan los datos del contacto
--  (el perfil del cliente solo cambia su teléfono y no manda el contacto).
--
--  No se pone NOT NULL en la tabla porque hay clientes viejos sin contacto;
--  se les pedirá la próxima vez que Recepción o el Encargado los editen.
-- =====================================================================
create or replace function _validar_contacto(p_nombre text, p_telefono text)
returns void language plpgsql immutable as $$
begin
  if length(regexp_replace(coalesce(p_nombre, ''), '[^[:alpha:]]', '', 'g')) < 3 then
    raise exception 'Escribe el nombre del contacto de emergencia';
  end if;
  if coalesce(p_telefono, '') !~ '^[0-9]{10}$' then
    raise exception 'El teléfono del contacto de emergencia debe tener 10 dígitos';
  end if;
end $$;

-- Clientes que todavía no tienen contacto de emergencia (solo para revisar)
select u.nombre, c.numero_membresia
  from clientes c join usuarios u on u.id = c.usuario_id
 where coalesce(trim(c.contacto_emergencia_nombre), '') = ''
    or coalesce(c.contacto_emergencia_telefono, '') !~ '^[0-9]{10}$'
 order by u.nombre;

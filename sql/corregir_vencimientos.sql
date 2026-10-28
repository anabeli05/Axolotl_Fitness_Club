-- =====================================================================
-- GYM CONTROL · Corregir fechas de vencimiento que no siguen los días del plan
-- Ejecutar en Supabase > SQL Editor, EN DOS PASOS:
--   PASO 1: seleccionar solo la consulta de vista previa y dar Run.
--   PASO 2: si la lista es correcta, seleccionar el UPDATE y dar Run.
--
-- Solo corrige clientes que tienen UNA sola membresía (nunca renovaron),
-- porque en ellos el vencimiento debe ser exactamente:
--     fecha de registro + duración del plan
-- Los clientes con renovaciones no se tocan.
-- =====================================================================

-- PASO 1 · Vista previa: qué clientes se corregirían y a qué fecha
select c.numero_membresia, u.nombre, u.fecha_registro, t.nombre as plan,
       t.duracion_dias, mc.fecha_fin as vence_actual,
       (u.fecha_registro + t.duracion_dias) as vence_correcto,
       (mc.fecha_fin - u.fecha_registro) - t.duracion_dias as dias_de_mas
  from membresias_cliente mc
  join clientes c on c.id = mc.cliente_id
  join usuarios u on u.id = c.usuario_id
  join tipos_membresia t on t.id = mc.tipo_membresia_id
 where (select count(*) from membresias_cliente x where x.cliente_id = mc.cliente_id) = 1
   and mc.fecha_fin <> u.fecha_registro + t.duracion_dias
 order by c.numero_membresia;


-- PASO 2 · Corrección (ejecutar solo después de revisar el PASO 1)
update membresias_cliente mc
   set fecha_fin = u.fecha_registro + t.duracion_dias
  from clientes c, usuarios u, tipos_membresia t
 where c.id = mc.cliente_id
   and u.id = c.usuario_id
   and t.id = mc.tipo_membresia_id
   and (select count(*) from membresias_cliente x where x.cliente_id = mc.cliente_id) = 1
   and mc.fecha_fin <> u.fecha_registro + t.duracion_dias;

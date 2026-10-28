-- =====================================================================
-- GYM CONTROL · Corregir el vencimiento de AX000003
-- Registro 08/10/2026 + Mensual (30 días) = vence 07/11/2026
-- Ejecutar en Supabase > SQL Editor, EN DOS PASOS.
-- =====================================================================

-- PASO 1 · Ver sus membresías antes de cambiar nada
select mc.id, mc.fecha_fin, t.nombre as plan, t.duracion_dias,
       u.fecha_registro, (u.fecha_registro + t.duracion_dias) as vence_correcto
  from membresias_cliente mc
  join clientes c on c.id = mc.cliente_id
  join usuarios u on u.id = c.usuario_id
  join tipos_membresia t on t.id = mc.tipo_membresia_id
 where c.numero_membresia = 'AX000003'
 order by mc.fecha_fin desc;


-- PASO 2 · Ajustar la membresía más reciente (la que muestra la app)
update membresias_cliente mc
   set fecha_fin = u.fecha_registro + t.duracion_dias
  from clientes c, usuarios u, tipos_membresia t
 where c.id = mc.cliente_id
   and u.id = c.usuario_id
   and t.id = mc.tipo_membresia_id
   and c.numero_membresia = 'AX000003'
   and mc.id = (select x.id from membresias_cliente x
                 where x.cliente_id = c.id
                 order by x.fecha_fin desc, x.id desc
                 limit 1);

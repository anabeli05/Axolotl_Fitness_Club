-- =====================================================================
-- GYM CONTROL · Gráficas del Dashboard con datos reales
-- Pegar completo en Supabase > SQL Editor > Run
-- =====================================================================

-- 1) Clientes activos / inactivos de los últimos 12 meses.
--    Un cliente cuenta como ACTIVO en un mes si al cierre de ese mes (o hoy, si es
--    el mes actual) tenía una membresía vigente: fecha_fin - duracion_dias <= corte <= fecha_fin.
--    INACTIVO = ya estaba registrado para ese corte pero sin membresía vigente.
drop view if exists clientes_por_mes;
create view clientes_por_mes as
with h as (
  select (now() at time zone 'America/Mexico_City')::date as hoy
),
meses as (
  select m::date as inicio,
         least((m + interval '1 month' - interval '1 day')::date, h.hoy) as corte
    from h, generate_series(date_trunc('month', h.hoy) - interval '11 months',
                            date_trunc('month', h.hoy), interval '1 month') m
),
conteo as (
  select me.inicio,
         count(*) as total,
         count(*) filter (where exists (
           select 1
             from membresias_cliente mc
             join tipos_membresia t on t.id = mc.tipo_membresia_id
            where mc.cliente_id = c.id
              and mc.fecha_fin >= me.corte
              and mc.fecha_fin - t.duracion_dias <= me.corte
         )) as activos
    from meses me
    join clientes c on true
    join usuarios u on u.id = c.usuario_id and u.fecha_registro <= me.corte
   group by me.inicio
)
select to_char(me.inicio, 'YYYY-MM') as periodo,
       coalesce(co.activos, 0)::int as activos,
       coalesce(co.total - co.activos, 0)::int as inactivos
  from meses me
  left join conteo co on co.inicio = me.inicio
 order by me.inicio;

-- 2) Ingresos (pagos) por día de la semana actual, de lunes a domingo
drop view if exists ingresos_semana;
create view ingresos_semana as
with h as (
  select (now() at time zone 'America/Mexico_City')::date as hoy
),
dias as (
  select d::date as dia
    from h, generate_series(date_trunc('week', h.hoy), date_trunc('week', h.hoy) + interval '6 days',
                            interval '1 day') d
)
select d.dia as fecha,
       extract(isodow from d.dia)::int as dia_semana,   -- 1 = lunes … 7 = domingo
       coalesce(sum(p.monto), 0)::numeric as total
  from dias d
  left join pagos p on (p.fecha_pago at time zone 'America/Mexico_City')::date = d.dia
 group by d.dia
 order by d.dia;

grant select on clientes_por_mes to anon, authenticated;
grant select on ingresos_semana to anon, authenticated;

notify pgrst, 'reload schema';

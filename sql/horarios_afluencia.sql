-- =====================================================================
--  AXOLOTL FITNESS CLUB · Horarios de menor afluencia con datos reales
--  Requiere sql/reconocimiento_facial.sql (tabla registros_acceso)
--  Supabase → SQL Editor → pegar todo → Run
--
--  afluencia_semanal: promedio de ENTRADAS por día de la semana y hora,
--  con las últimas 4 semanas completas + la actual (28 días hacia atrás).
--  Los días sin entradas cuentan como 0, así el promedio es real.
--    dia_semana: 1 = lunes … 7 = domingo
--    hora:       6 … 23  (horario del gimnasio 06:00 - 00:00)
-- =====================================================================

drop view if exists afluencia_semanal;
create view afluencia_semanal as
with h as (
  select (now() at time zone 'America/Mexico_City')::date as hoy
),
dias as (
  select d::date as dia
    from h, generate_series(h.hoy - 27, h.hoy, interval '1 day') d
),
horas as (
  select generate_series(6, 23) as hora
),
entradas as (
  select (ra.fecha_hora at time zone 'America/Mexico_City')::date as dia,
         extract(hour from ra.fecha_hora at time zone 'America/Mexico_City')::int as hora,
         count(*) as n
    from registros_acceso ra, h
   where ra.es_entrada
     and ra.fecha_hora >= ((h.hoy - 27)::timestamp at time zone 'America/Mexico_City')
   group by 1, 2
)
select extract(isodow from d.dia)::int as dia_semana,
       ho.hora,
       round(avg(coalesce(e.n, 0)), 1)::numeric as promedio
  from dias d
 cross join horas ho
  left join entradas e on e.dia = d.dia and e.hora = ho.hora
 group by 1, 2
 order by 1, 2;

grant select on afluencia_semanal to anon, authenticated;

notify pgrst, 'reload schema';

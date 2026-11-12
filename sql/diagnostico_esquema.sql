-- =====================================================================
--  GYM CONTROL · Diagnóstico del esquema (SOLO LECTURA, no cambia nada)
--  Supabase → SQL Editor → pegar → Run → copiar el resultado completo
-- =====================================================================
select string_agg(bloque, E'\n\n' order by orden, nombre) as esquema
from (
  -- Tablas y columnas
  select 1 as orden, t.table_name as nombre,
         '-- TABLA ' || t.table_name || E'\n' ||
         string_agg('   ' || c.column_name || ' ' || c.data_type ||
                    coalesce(' default ' || c.column_default, '') ||
                    case when c.is_nullable = 'NO' then ' not null' else '' end,
                    E'\n' order by c.ordinal_position) as bloque
    from information_schema.tables t
    join information_schema.columns c
      on c.table_schema = t.table_schema and c.table_name = t.table_name
   where t.table_schema = 'public' and t.table_type = 'BASE TABLE'
   group by t.table_name
  union all
  -- Vistas
  select 2, v.viewname, '-- VISTA ' || v.viewname || E'\n' || v.definition
    from pg_views v where v.schemaname = 'public'
  union all
  -- Funciones
  select 3, p.proname, pg_get_functiondef(p.oid)
    from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.prokind = 'f'
  union all
  -- Triggers
  select 4, tg.tgname, '-- TRIGGER ' || tg.tgname || ' ON ' || tg.tgrelid::regclass
    from pg_trigger tg join pg_class cl on cl.oid = tg.tgrelid
    join pg_namespace n on n.oid = cl.relnamespace
   where n.nspname = 'public' and not tg.tgisinternal
  union all
  -- Extensiones (pg_cron, pg_net) y tareas programadas
  select 5, e.extname, '-- EXTENSION ' || e.extname || ' ' || e.extversion
    from pg_extension e
) x;

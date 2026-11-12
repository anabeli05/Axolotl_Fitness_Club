-- =====================================================================
--  AXOLOTL FITNESS CLUB · Reconocimiento facial con vectores de 512
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  La app cambió de MobileFaceNet (192 números) a FaceNet-512 (512 números).
--  Los vectores de un modelo no sirven con el otro, así que:
--    · se BORRAN los rostros registrados (no las fotos de perfil ni los clientes)
--    · Recepción debe volver a registrar el rostro de cada cliente
--  Correr esto junto con instalar la nueva versión de la app: con la app
--  anterior (192) el escáner y el registro dejarán de funcionar.
-- =====================================================================

-- 1. Rostros viejos fuera (eran de 192) y nueva validación de tamaño
delete from rostros_clientes where array_length(embedding, 1) <> 512;

alter table rostros_clientes drop constraint if exists rostros_clientes_embedding_check;
alter table rostros_clientes add constraint rostros_clientes_embedding_check
  check (array_length(embedding, 1) = 512);

-- 2. Funciones que validan el tamaño del vector: 192 -> 512
--    (se toma la versión que ya está en la base, solo cambia el número)
do $$
declare f record; v_def text;
begin
  for f in
    select p.oid
      from pg_proc p join pg_namespace n on n.oid = p.pronamespace
     where n.nspname = 'public'
       and p.prosrc ~ 'array_length\(\s*p_embedding\s*,\s*1\s*\)\s*<>\s*192'
  loop
    v_def := regexp_replace(pg_get_functiondef(f.oid),
               'array_length\(\s*p_embedding\s*,\s*1\s*\)\s*<>\s*192',
               'array_length(p_embedding, 1) <> 512', 'g');
    execute v_def;
    raise notice 'Actualizada: %', f.oid::regprocedure;
  end loop;
end $$;

-- 3. Umbral para FaceNet-512 (similitud coseno, 0 a 1)
--    Si confunde a dos personas súbelo (0.65); si no reconoce, bájalo (0.55).
create or replace function public._umbral_rostro()
returns real language sql immutable
as $$ select 0.60::real $$;

-- Comprobación: debe salir 0 filas (ninguna función sigue pidiendo 192)
select p.oid::regprocedure as funcion_pendiente
  from pg_proc p join pg_namespace n on n.oid = p.pronamespace
 where n.nspname = 'public' and p.prosrc like '%<> 192%';

notify pgrst, 'reload schema';

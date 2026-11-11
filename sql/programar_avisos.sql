-- =====================================================================
--  AXOLOTL FITNESS CLUB · Ejecutar avisos-diarios todos los días a las 9:00 (México)
--  Requiere: sql/pendientes_noviembre.sql y la Edge Function avisos-diarios publicada.
--
--  Antes de correr:
--   1. Database → Extensions: activar pg_cron y pg_net.
--   2. Cambiar ESCRIBE_AQUI_EL_CRON_SECRET por el mismo valor del secreto CRON_SECRET
--      de la Edge Function.
--  Luego: SQL Editor → pegar → Run.
-- =====================================================================
create extension if not exists pg_cron;
create extension if not exists pg_net;

-- Guarda el secreto en el Vault (si ya existe, lo actualiza)
do $$
begin
  if exists (select 1 from vault.secrets where name = 'cron_secret') then
    perform vault.update_secret((select id from vault.secrets where name = 'cron_secret'),
                                'ESCRIBE_AQUI_EL_CRON_SECRET');
  else
    perform vault.create_secret('ESCRIBE_AQUI_EL_CRON_SECRET', 'cron_secret');
  end if;
end $$;

-- 15:00 UTC = 09:00 hora de México
select cron.unschedule('avisos-diarios')
 where exists (select 1 from cron.job where jobname = 'avisos-diarios');

select cron.schedule(
  'avisos-diarios',
  '0 15 * * *',
  $$
  select net.http_post(
    url     := 'https://sliubdwhvfkpjaglxbre.supabase.co/functions/v1/avisos-diarios',
    headers := jsonb_build_object(
                 'Content-Type', 'application/json',
                 'x-cron-secret', (select decrypted_secret from vault.decrypted_secrets
                                    where name = 'cron_secret')),
    body    := '{}'::jsonb
  );
  $$
);

-- Comprobación: debe aparecer la tarea
select jobid, jobname, schedule from cron.job where jobname = 'avisos-diarios';

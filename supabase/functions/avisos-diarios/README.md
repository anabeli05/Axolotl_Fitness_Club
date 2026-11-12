# avisos-diarios

Envía los avisos de vencimiento y de eliminación y elimina a los clientes con 365 días sin uso.

```bash
supabase functions deploy avisos-diarios --no-verify-jwt
supabase secrets set RESEND_API_KEY=... CORREO_REMITENTE="Axolotl Fitness Club <avisos@axolotlgym.com>" CRON_SECRET=...
```

`--no-verify-jwt` porque la protege `CRON_SECRET` (la key pública de la app no puede ejecutarla).
Después se corre `sql/programar_avisos.sql`.

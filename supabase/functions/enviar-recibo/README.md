# enviar-recibo

Manda por correo el comprobante de pago cuando Recepción o el Encargado renuevan un plan.
El destinatario es siempre el correo registrado del cliente (lo busca la función, no la app).

```bash
# 1. Supabase → SQL Editor: correr sql/recibo_pago.sql
# 2. Desplegar (usa los secretos GMAIL_USER y GMAIL_APP_PASSWORD)
supabase functions deploy enviar-recibo
```

Se deja con verificación JWT (sin `--no-verify-jwt`): la app la llama con su key pública.
Solo envía el último pago aceptado del cliente de los últimos 15 minutos, y una sola vez.

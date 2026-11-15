# enviar-comunicados

Manda por correo los comunicados que publica el Encargado en la sección **Avisos**:
cambios en los Términos y Condiciones, en la Política de Privacidad y de horario.

```bash
# 1. Supabase → SQL Editor: correr sql/comunicados.sql
# 2. Desplegar (usa los secretos GMAIL_USER y GMAIL_APP_PASSWORD)
supabase functions deploy enviar-comunicados
# 3. Volver a desplegar avisos-diarios para que cada mañana reintente lo pendiente
supabase functions deploy avisos-diarios --no-verify-jwt
```

Se deja con verificación JWT, como enviar-recibo: la app la llama con su key pública.
No recibe datos; solo envía lo que la base tiene pendiente.

Envía como máximo 300 correos por ejecución (Gmail permite unos 500 al día por cuenta y
la misma cuenta manda recibos, códigos y avisos). Si hay más destinatarios, el resto sale
en las siguientes ejecuciones (avisos-diarios la llama cada mañana). Se puede cambiar con
el secreto `COMUNICADOS_MAX_POR_EJECUCION`.

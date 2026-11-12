# enviar-codigo-contrasena

Manda un código de 6 dígitos para cambiar la contraseña cuando el usuario no recuerda la actual.
Solo envía si el correo está registrado (con la cuenta activa o inactiva), pero responde igual en ambos casos
(así no se puede averiguar qué correos existen).

```bash
# 1. Supabase → SQL Editor: correr sql/restablecer_contrasena.sql
# 2. Desplegar (usa los secretos RESEND_API_KEY y CORREO_REMITENTE)
supabase functions deploy enviar-codigo-contrasena
```

O desde el panel: Edge Functions → Deploy a new function → Via Editor, con el nombre
`enviar-codigo-contrasena` y el contenido de `index.ts`. Se deja con verificación JWT.

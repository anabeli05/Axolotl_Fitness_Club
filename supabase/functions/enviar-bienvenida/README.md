# enviar-bienvenida

Manda el correo de bienvenida justo después de un alta (la app la llama con `{ "gmail": "..." }`).

- **Personal** (Recepción, Instructor, Encargado): rol asignado y contraseña temporal.
- **Cliente**: número de membresía y contraseña temporal.

Ambos indican que la contraseña se cambia en la sección **Perfil** de la app.
Solo envía si la cuenta se registró hoy o ayer y aún tiene la contraseña inicial.

```bash
# Desplegar (usa los secretos GMAIL_USER y GMAIL_APP_PASSWORD)
supabase functions deploy enviar-bienvenida
```

O desde el panel: Edge Functions → `enviar-bienvenida` → Edit, pegar el contenido de `index.ts` y desplegar.
Se deja con verificación JWT: la app la llama con su key pública.

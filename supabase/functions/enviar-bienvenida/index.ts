// =====================================================================
//  AXOLOTL FITNESS CLUB · Edge Function "enviar-bienvenida"
//  La app la llama justo después de dar de alta a alguien:
//    POST { "gmail": "nuevo@correo.com" }
//  - Personal (RECEPCION, INSTRUCTOR, ENCARGADO): rol asignado + contraseña temporal.
//  - Cliente: número de membresía + contraseña temporal.
//  En ambos casos indica que la contraseña se cambia en la sección Perfil de la app.
//
//  Para que no sirva para mandar correos a cualquiera, solo envía si la cuenta se
//  registró hoy o ayer y todavía tiene la contraseña inicial (Axolotl2026).
//
//  Usa los mismos secretos que avisos-diarios / enviar-recibo:
//    GMAIL_USER, GMAIL_APP_PASSWORD
//  SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY ya los da Supabase.
// =====================================================================
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import nodemailer from "npm:nodemailer@6.9.16";
import bcrypt from "npm:bcryptjs@2.4.3";

// Misma contraseña que asignan crear_personal / crear_cliente (sql/esquema_completo.sql)
const CONTRASENA_INICIAL = "Axolotl2026";

const ROLES_PERSONAL: Record<string, string> = {
  ENCARGADO: "Encargado",
  RECEPCION: "Recepción",
  INSTRUCTOR: "Instructor",
};

type Datos = {
  nombre: string;
  gmail: string;
  rolPersonal: string | null; // etiqueta del rol si es personal
  membresia: string | null;   // número AX000000 si es cliente
};

const esc = (s: string | null | undefined) =>
  (s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));

// ---- Paleta de la app (ClienteTema / GymColors) --------------------------------------
const C = {
  fondo: "#F1EFEB",     // hueso mate (fondo de pantalla)
  tarjeta: "#FAF9F7",   // tarjetas
  interior: "#EFEDE9",  // recuadros internos
  negro: "#111111",     // encabezado
  texto: "#1B1C1F",
  textoSec: "#5B5E66",
  cafe: "#8E723A",      // cajas de acción
  dorado: "#AF8815",    // acento de marca
  borde: "#C9B26A",
  crema: "#F8F4EA",
};

/** Fila del bloque destacado (etiqueta arriba, valor grande abajo). */
const dato = (etiqueta: string, valor: string, mono = false) => `
<tr><td style="padding:14px 20px;border-bottom:1px solid rgba(248,244,234,0.18)">
  <div style="font-size:11px;letter-spacing:1.5px;text-transform:uppercase;color:#E9DDBF;font-weight:700">${etiqueta}</div>
  <div style="margin-top:4px;font-size:22px;font-weight:700;color:${C.crema};${mono ? "font-family:'Courier New',Courier,monospace;letter-spacing:2px;" : ""}">${valor}</div>
</td></tr>`;

function plantilla(d: Datos): { asunto: string; html: string } {
  const nombre = esc(d.nombre.trim().split(/\s+/)[0] || d.nombre);
  const esPersonal = d.rolPersonal != null;

  const asunto = esPersonal
    ? `Bienvenido al equipo de Axolotl Fitness Club · ${d.rolPersonal}`
    : "Bienvenido a Axolotl Fitness Club · Tu acceso a la app";

  const intro = esPersonal
    ? `Ya formas parte del equipo de <b>Axolotl Fitness Club</b>. Tu cuenta en la app está lista; estos son tus datos para entrar por primera vez:`
    : `Ya eres parte de <b>Axolotl Fitness Club</b>. Tu cuenta en la app está lista; estos son tus datos para entrar por primera vez:`;

  const filas = (esPersonal
    ? dato("Rol asignado", esc(d.rolPersonal))
    : dato("Número de membresía", esc(d.membresia) || "—", true)) +
    dato("Contraseña temporal", CONTRASENA_INICIAL, true);

  const html = `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="color-scheme" content="light only">
<title>${esc(asunto)}</title>
<style>
  @media (max-width:520px){
    .contenedor{width:100% !important}
    .pad{padding-left:20px !important;padding-right:20px !important}
    .titulo{font-size:24px !important}
  }
</style>
</head>
<body style="margin:0;padding:0;background:${C.fondo};-webkit-text-size-adjust:100%">
<div style="display:none;max-height:0;overflow:hidden;opacity:0">Tus datos de acceso y cómo cambiar tu contraseña desde Perfil.</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background:${C.fondo}">
<tr><td align="center" style="padding:28px 12px">

  <table role="presentation" class="contenedor" width="520" cellpadding="0" cellspacing="0" border="0"
         style="width:520px;max-width:520px;background:${C.tarjeta};border:1.5px solid ${C.borde};border-radius:18px;overflow:hidden;font-family:Arial,Helvetica,sans-serif">

    <!-- Encabezado -->
    <tr><td style="background:${C.negro};padding:26px 28px;text-align:center">
      <div style="font-size:12px;letter-spacing:4px;color:${C.dorado};font-weight:700">AXOLOTL</div>
      <div style="font-size:20px;letter-spacing:2px;color:#F5F5F5;font-weight:700;margin-top:2px">FITNESS CLUB</div>
      <div style="width:48px;height:3px;background:${C.dorado};margin:14px auto 0;border-radius:2px;font-size:0;line-height:0">&nbsp;</div>
    </td></tr>

    <!-- Bienvenida -->
    <tr><td class="pad" style="padding:30px 32px 8px">
      <div style="font-size:13px;color:${C.cafe};font-weight:700;letter-spacing:1px;text-transform:uppercase">${esPersonal ? "Bienvenida al equipo" : "Bienvenida"}</div>
      <h1 class="titulo" style="margin:6px 0 12px;font-size:28px;line-height:1.2;color:${C.texto}">¡Hola, ${nombre}!</h1>
      <p style="margin:0;font-size:15px;line-height:1.6;color:${C.texto}">${intro}</p>
    </td></tr>

    <!-- Bloque destacado: rol (o membresía) + contraseña temporal -->
    <tr><td class="pad" style="padding:20px 32px">
      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0"
             style="background:${C.cafe};border-radius:14px;border-collapse:separate">
        ${filas}
        <tr><td style="padding:12px 20px;font-size:12px;color:#E9DDBF">Inicia sesión con este correo: <span style="color:${C.crema};font-weight:700">${esc(d.gmail)}</span></td></tr>
      </table>
    </td></tr>

    <!-- Aviso de seguridad: cambiar en Perfil -->
    <tr><td class="pad" style="padding:0 32px 8px">
      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0"
             style="background:${C.interior};border-left:4px solid ${C.dorado};border-radius:10px">
        <tr><td style="padding:16px 18px">
          <div style="font-size:14px;font-weight:700;color:${C.texto};margin-bottom:6px">&#128274; Por tu seguridad, cambia esta contraseña</div>
          <div style="font-size:14px;line-height:1.6;color:${C.textoSec}">
            Al entrar a la app, ve a la sección <b style="color:${C.texto}">Perfil</b> y toca
            <b style="color:${C.texto}">Cambiar contraseña</b>. La nueva debe tener mínimo 8 caracteres,
            con mayúscula, minúscula, número y símbolo.
          </div>
        </td></tr>
      </table>
    </td></tr>

    <!-- Pasos -->
    <tr><td class="pad" style="padding:18px 32px 26px">
      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="font-size:14px;color:${C.texto}">
        ${[
          "Abre la app Axolotl Gym e inicia sesión con tu correo y la contraseña temporal.",
          "Entra a la sección <b>Perfil</b>.",
          "Toca <b>Cambiar contraseña</b> y guarda la nueva.",
        ].map((t, i) => `<tr>
          <td width="34" valign="top" style="padding:6px 0">
            <div style="width:24px;height:24px;line-height:24px;border-radius:12px;background:${C.negro};color:${C.dorado};font-size:12px;font-weight:700;text-align:center">${i + 1}</div>
          </td>
          <td style="padding:8px 0;line-height:1.5">${t}</td>
        </tr>`).join("")}
      </table>
    </td></tr>

    <!-- Pie -->
    <tr><td style="background:${C.interior};padding:18px 28px;text-align:center;font-size:12px;line-height:1.6;color:${C.textoSec}">
      Si no esperabas este correo, avísanos respondiendo a este mensaje.<br>
      <b style="color:${C.cafe}">Axolotl Fitness Club</b> · contacto@axolotlgym.com · +52 314 139 7633
    </td></tr>

  </table>
</td></tr>
</table>
</body>
</html>`;

  return { asunto, html };
}

// ---- Correo por Gmail (secretos GMAIL_USER y GMAIL_APP_PASSWORD) -------------------
// GMAIL_APP_PASSWORD es una "contraseña de aplicación" de Google (16 letras), no la
// contraseña normal de la cuenta. Se usa el puerto 465: Supabase bloquea el 25 y el 587.
const gmailUser = () => Deno.env.get("GMAIL_USER") ?? Deno.env.get("gmail_user") ?? "";
const gmailPass = () => (Deno.env.get("GMAIL_APP_PASSWORD") ?? Deno.env.get("gmail_app_password") ?? "").replace(/\s/g, "");

let transporte: ReturnType<typeof nodemailer.createTransport> | null = null;
function smtp() {
  transporte ??= nodemailer.createTransport({
    host: "smtp.gmail.com",
    port: 465,
    secure: true,
    auth: { user: gmailUser(), pass: gmailPass() },
  });
  return transporte;
}

/** null si salió; si no, el motivo. */
async function enviarGmail(para: string, asunto: string, html: string): Promise<string | null> {
  if (!gmailUser()) return "Falta el secreto GMAIL_USER en Edge Functions → Secrets";
  if (!gmailPass()) return "Falta el secreto GMAIL_APP_PASSWORD en Edge Functions → Secrets";
  try {
    await smtp().sendMail({ from: `"Axolotl Fitness Club" <${gmailUser()}>`, to: para, subject: asunto, html });
    return null;
  } catch (e) {
    console.error("Correo no enviado", para, e);
    return `Gmail: ${e instanceof Error ? e.message : String(e)}`.slice(0, 300);
  }
}

const enviarCorreo = enviarGmail;

/** yyyy-MM-dd de hace [dias] días en hora de México. */
function fechaMx(dias: number): string {
  const d = new Date(Date.now() - dias * 86_400_000);
  return d.toLocaleDateString("en-CA", { timeZone: "America/Mexico_City" });
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Método no permitido", { status: 405 });

  let gmail = "";
  try {
    gmail = String((await req.json()).gmail ?? "").trim().toLowerCase();
  } catch {
    return Response.json({ enviado: false, motivo: "JSON inválido" }, { status: 400 });
  }
  if (!gmail.includes("@") || gmail.length > 200) {
    return Response.json({ enviado: false, motivo: "Correo inválido" }, { status: 400 });
  }

  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

  const { data: u, error } = await db
    .from("usuarios")
    .select("id, nombre, gmail, contrasena, fecha_registro")
    .eq("gmail", gmail)
    .maybeSingle();
  if (error) return Response.json({ enviado: false, motivo: error.message }, { status: 500 });
  if (!u) return Response.json({ enviado: false, motivo: "Usuario no encontrado" }, { status: 404 });

  // Solo cuentas recién creadas que no han cambiado la contraseña inicial
  if (String(u.fecha_registro) < fechaMx(1)) {
    return Response.json({ enviado: false, motivo: "La cuenta no es reciente" }, { status: 409 });
  }
  if (!bcrypt.compareSync(CONTRASENA_INICIAL, String(u.contrasena ?? ""))) {
    return Response.json({ enviado: false, motivo: "La contraseña inicial ya se cambió" }, { status: 409 });
  }

  // Roles del usuario
  const { data: ur } = await db.from("usuario_roles").select("rol_id").eq("usuario_id", u.id);
  const ids = (ur ?? []).map((r: { rol_id: number }) => r.rol_id);
  const { data: rs } = ids.length
    ? await db.from("roles").select("nombre").in("id", ids)
    : { data: [] as { nombre: string }[] };
  const nombres = (rs ?? []).map((r: { nombre: string }) => r.nombre.toUpperCase());

  // Si tiene un rol de personal, va la plantilla de personal (prioridad: Encargado)
  const rolPersonal = ["ENCARGADO", "RECEPCION", "INSTRUCTOR"].find((r) => nombres.includes(r));

  let membresia: string | null = null;
  if (!rolPersonal) {
    const { data: cli } = await db.from("clientes").select("numero_membresia").eq("usuario_id", u.id).maybeSingle();
    membresia = cli?.numero_membresia ?? null;
  }

  const { asunto, html } = plantilla({
    nombre: u.nombre,
    gmail: u.gmail,
    rolPersonal: rolPersonal ? ROLES_PERSONAL[rolPersonal] : null,
    membresia,
  });

  const falla = await enviarCorreo(u.gmail, asunto, html);
  if (falla) return Response.json({ enviado: false, motivo: falla }, { status: 502 });
  return Response.json({ enviado: true });
});

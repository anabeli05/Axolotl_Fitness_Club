// =====================================================================
//  AXOLOTL FITNESS CLUB · Edge Function "enviar-codigo-contrasena"
//  La app la llama cuando alguien quiere cambiar su contraseña y no recuerda la actual:
//    POST { "gmail": "cliente@correo.com" }
//  Crea un código de 6 dígitos (sql/restablecer_contrasena.sql) y lo manda a ese
//  correo SOLO si está registrado. Para no revelar qué correos existen, responde
//  igual aunque no lo esté.
//
//  Usa los mismos secretos que avisos-diarios / enviar-recibo:
//    GMAIL_USER, GMAIL_APP_PASSWORD
// =====================================================================
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import nodemailer from "npm:nodemailer@6.9.16";

type Codigo = { r_nombre: string; r_gmail: string; r_codigo: string };

const esc = (s: string) =>
  s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));

function plantilla(c: Codigo): { asunto: string; html: string } {
  return {
    asunto: `Tu código para cambiar la contraseña: ${c.r_codigo}`,
    html: `<div style="font-family:Arial,Helvetica,sans-serif;background:#F1EFEB;padding:24px">
  <div style="max-width:480px;margin:0 auto;background:#FAF9F7;border:1.5px solid #C9B26A;border-radius:16px;padding:24px">
    <h2 style="margin:0 0 4px;color:#1B1C1F">Cambio de contraseña</h2>
    <p style="margin:0 0 16px;color:#8E723A;font-weight:600">Axolotl Fitness Club</p>
    <p style="margin:0 0 16px;color:#1B1C1F">Hola ${esc(c.r_nombre.split(" ")[0])}, escribe este código en la app para confirmar tu nueva contraseña:</p>
    <p style="margin:0 0 16px;text-align:center;font-size:34px;letter-spacing:8px;font-weight:700;color:#8E723A">${c.r_codigo}</p>
    <p style="margin:0 0 8px;color:#5B5E66;font-size:13px">El código vence en 15 minutos.</p>
    <p style="margin:0;color:#5B5E66;font-size:13px">Si tú no lo pediste, ignora este correo: tu contraseña no cambiará.</p>
    <p style="margin:20px 0 0;color:#5B5E66;font-size:12px">Axolotl Fitness Club · contacto@axolotlgym.com · +52 314 139 7633</p>
  </div>
</div>`,
  };
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

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Método no permitido", { status: 405 });

  let gmail = "";
  try {
    gmail = String((await req.json()).gmail ?? "").trim();
  } catch {
    return Response.json({ enviado: false, motivo: "JSON inválido" }, { status: 400 });
  }
  if (!gmail.includes("@") || gmail.length > 200) {
    return Response.json({ enviado: false, motivo: "Escribe un correo válido" }, { status: 400 });
  }

  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data, error } = await db.rpc("crear_codigo_contrasena", { p_gmail: gmail });
  if (error) {
    // p. ej. "Espera un minuto antes de pedir otro código"
    return Response.json({ enviado: false, motivo: error.message }, { status: 429 });
  }

  const codigo = (data ?? [])[0] as Codigo | undefined;
  // Correo no registrado: misma respuesta, sin enviar nada
  if (!codigo) return Response.json({ enviado: true });

  const { asunto, html } = plantilla(codigo);
  const falla = await enviarCorreo(codigo.r_gmail, asunto, html);
  if (falla) return Response.json({ enviado: false, motivo: falla }, { status: 502 });
  return Response.json({ enviado: true });
});

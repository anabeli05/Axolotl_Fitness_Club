// =====================================================================
//  AXOLOTL FITNESS CLUB · Edge Function "enviar-comunicados"
//  Manda por correo los comunicados que publica el Encargado (sql/comunicados.sql):
//  cambios en los Términos y Condiciones, en la Política de Privacidad y de horario.
//
//  La llaman:
//    · la app, justo después de publicar un comunicado (envío inmediato);
//    · avisos-diarios, cada mañana, para reintentar lo que haya quedado pendiente.
//  No recibe datos: solo envía lo que la base tiene pendiente, así que la app no
//  puede elegir destinatarios ni contenido.
//
//  Usa los mismos secretos que las demás funciones: GMAIL_USER, GMAIL_APP_PASSWORD.
//  SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY ya los da Supabase.
// =====================================================================
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import nodemailer from "npm:nodemailer@6.9.16";

type Pendiente = {
  r_comunicado_id: number;
  r_usuario_id: number;
  r_nombre: string;
  r_gmail: string;
  r_tipo: "TERMINOS" | "PRIVACIDAD" | "HORARIO";
  r_titulo: string;
  r_mensaje: string;
  r_fecha_vigor: string; // yyyy-MM-dd
};

// Máximo de correos por ejecución. Gmail permite unos 500 al día por cuenta, y los
// recibos, códigos y avisos diarios salen de la misma cuenta: se deja margen.
const MAXIMO = Number(Deno.env.get("COMUNICADOS_MAX_POR_EJECUCION") ?? "300");

const fecha = (iso: string) => {
  const [a, m, d] = iso.slice(0, 10).split("-");
  return `${d}/${m}/${a}`;
};

const esc = (s: string | null | undefined) =>
  (s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));

function plantilla(p: Pendiente): { asunto: string; html: string } {
  const nombre = esc(p.r_nombre.split(" ")[0]);
  const mensaje = esc(p.r_mensaje).replace(/\n/g, "<br>");
  const vigor = fecha(p.r_fecha_vigor);

  const intro = {
    TERMINOS: `Actualizamos nuestros <b>Términos y Condiciones</b>. Los cambios entran en vigor el <b>${vigor}</b>.`,
    PRIVACIDAD: `Actualizamos nuestra <b>Política de Privacidad</b>. Los cambios entran en vigor el <b>${vigor}</b>.`,
    HORARIO: `Habrá un <b>cambio de horario</b> en el gimnasio a partir del <b>${vigor}</b>.`,
  }[p.r_tipo];

  const cierre = p.r_tipo === "HORARIO"
    ? "Gracias por tu comprensión."
    : "Puedes consultar el documento completo en la app, desde los enlaces de la pantalla de inicio de sesión. " +
      "Si sigues usando nuestros servicios después de esa fecha, se entiende que aceptas los cambios.";

  const prefijo = { TERMINOS: "Términos y Condiciones", PRIVACIDAD: "Política de Privacidad", HORARIO: "Cambio de horario" }[p.r_tipo];

  return {
    asunto: `${prefijo} · ${p.r_titulo} · Axolotl Fitness Club`,
    html: `<div style="font-family:Arial,Helvetica,sans-serif;background:#F1EFEB;padding:24px">
  <div style="max-width:480px;margin:0 auto;background:#FAF9F7;border:1.5px solid #C9B26A;border-radius:16px;padding:24px">
    <h2 style="margin:0 0 4px;color:#1B1C1F">${esc(p.r_titulo)}</h2>
    <p style="margin:0 0 16px;color:#8E723A;font-weight:600">Axolotl Fitness Club · ${prefijo}</p>
    <p style="margin:0 0 12px;color:#1B1C1F">Hola ${nombre}:</p>
    <p style="margin:0 0 12px;color:#1B1C1F">${intro}</p>
    <div style="margin:0 0 12px;padding:14px;background:#F8F4EA;border-radius:12px;color:#1B1C1F">${mensaje}</div>
    <p style="margin:0 0 12px;color:#1B1C1F">${cierre}</p>
    <p style="margin:20px 0 0;color:#5B5E66;font-size:12px">
    Axolotl Fitness Club · contacto@axolotlgym.com · +52 314 139 7633</p>
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


Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Método no permitido", { status: 405 });
  if (!gmailUser() || !gmailPass()) {
    return Response.json({ motivo: "Faltan los secretos GMAIL_USER o GMAIL_APP_PASSWORD" }, { status: 500 });
  }

  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data, error } = await db.rpc("comunicados_pendientes", { p_limite: MAXIMO });
  if (error) return Response.json({ motivo: error.message }, { status: 500 });

  const pendientes = (data ?? []) as Pendiente[];
  let enviados = 0;
  let falla: string | null = null;

  // Uno por uno con la misma conexión; se marca cada correo que sí salió
  const salieron = new Map<number, number[]>();
  for (const p of pendientes) {
    const { asunto, html } = plantilla(p);
    const error = await enviarGmail(p.r_gmail, asunto, html);
    if (error) {
      falla = error;
      // Límite diario o credenciales: no tiene caso seguir; el resto se reintenta mañana
      if (/limit|quota|535|534|auth|credentials/i.test(error)) break;
      continue;
    }
    salieron.set(p.r_comunicado_id, [...(salieron.get(p.r_comunicado_id) ?? []), p.r_usuario_id]);
    enviados++;
  }
  for (const [comunicadoId, usuarios] of salieron) {
    await db.rpc("marcar_comunicado_enviado", { p_comunicado_id: comunicadoId, p_usuario_ids: usuarios });
  }

  const status = falla && enviados === 0 ? 502 : 200;
  return Response.json({ pendientes: pendientes.length, enviados, motivo: falla }, { status });
});

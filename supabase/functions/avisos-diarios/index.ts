// =====================================================================
//  AXOLOTL FITNESS CLUB · Edge Function "avisos-diarios"
//  Se ejecuta una vez al día (ver sql/programar_avisos.sql):
//    1. Aviso de vencimiento de membresía (3 días antes).
//    2. Aviso de eliminación de la cuenta (30 días antes de cumplir 365 días sin uso).
//    3. Elimina a los clientes que ya cumplieron el plazo y fueron avisados.
//    4. Reintenta los comunicados pendientes (Edge Function enviar-comunicados).
//
//  Secretos (Supabase → Edge Functions → Secrets):
//    GMAIL_USER          cuenta de Gmail que envía los correos
//    GMAIL_APP_PASSWORD  contraseña de aplicación de esa cuenta
//    CRON_SECRET       texto largo cualquiera; el mismo que se guarda en el Vault
//  SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY ya los da Supabase.
//
// =====================================================================
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import nodemailer from "npm:nodemailer@6.9.16";

type Aviso = {
  r_tipo: "VENCIMIENTO" | "ELIMINACION";
  r_cliente_id: number;
  r_nombre: string;
  r_gmail: string;
  r_plan: string | null;
  r_fecha: string; // yyyy-MM-dd
};

const fecha = (iso: string) => {
  const [a, m, d] = iso.split("-");
  return `${d}/${m}/${a}`;
};

function plantilla(a: Aviso): { asunto: string; html: string } {
  const nombre = a.r_nombre.split(" ")[0];
  if (a.r_tipo === "VENCIMIENTO") {
    return {
      asunto: "Tu membresía de Axolotl Fitness Club está por vencer",
      html: `<p>Hola ${nombre}:</p>
<p>Tu membresía <b>${a.r_plan ?? ""}</b> vence el <b>${fecha(a.r_fecha)}</b>.
Para seguir entrenando sin interrupciones, renuévala en recepción.</p>
<p>Axolotl Fitness Club · contacto@axolotlgym.com · +52 314 139 7633</p>`,
    };
  }
  return {
    asunto: "Tu cuenta de Axolotl Fitness Club será eliminada",
    html: `<p>Hola ${nombre}:</p>
<p>Tu cuenta no ha tenido uso en casi un año. Conforme a nuestra Política de Privacidad,
será <b>eliminada el ${fecha(a.r_fecha)}</b> junto con tus datos personales.</p>
<p>Si deseas conservarla, solo inicia sesión en la app, visita el gimnasio o renueva tu membresía antes de esa fecha.</p>
<p>Axolotl Fitness Club · contacto@axolotlgym.com · +52 314 139 7633</p>`,
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

async function enviarCorreo(para: string, asunto: string, html: string): Promise<boolean> {
  return (await enviarGmail(para, asunto, html)) === null;
}

Deno.serve(async (req) => {
  // Solo la tarea programada puede ejecutarla
  if (req.headers.get("x-cron-secret") !== Deno.env.get("CRON_SECRET")) {
    return new Response("No autorizado", { status: 401 });
  }

  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

  const { data: avisos, error } = await db.rpc("avisos_pendientes");
  if (error) return new Response(error.message, { status: 500 });

  let enviados = 0;
  for (const a of (avisos ?? []) as Aviso[]) {
    const { asunto, html } = plantilla(a);
    // Solo se marca si el correo salió; si falla se reintenta mañana
    if (await enviarCorreo(a.r_gmail, asunto, html)) {
      await db.rpc("marcar_aviso", { p_cliente_id: a.r_cliente_id, p_tipo: a.r_tipo, p_fecha: a.r_fecha });
      enviados++;
    }
  }

  const { data: eliminados, error: e2 } = await db.rpc("eliminar_clientes_inactivos");
  if (e2) return new Response(e2.message, { status: 500 });

  // Reintenta los comunicados (términos, privacidad, horario) que quedaron pendientes
  let comunicados: unknown = null;
  try {
    const r = await fetch(`${Deno.env.get("SUPABASE_URL")}/functions/v1/enviar-comunicados`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")}`,
        "Content-Type": "application/json",
      },
      body: "{}",
    });
    comunicados = await r.json();
  } catch (e) {
    console.error("enviar-comunicados no respondió", e);
  }

  return Response.json({ avisos: avisos?.length ?? 0, enviados, eliminados, comunicados });
});

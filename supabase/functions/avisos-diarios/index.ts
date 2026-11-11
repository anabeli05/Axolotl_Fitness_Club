// =====================================================================
//  AXOLOTL FITNESS CLUB · Edge Function "avisos-diarios"
//  Se ejecuta una vez al día (ver sql/programar_avisos.sql):
//    1. Aviso de vencimiento de membresía (3 días antes).
//    2. Aviso de eliminación de la cuenta (30 días antes de cumplir 365 días sin uso).
//    3. Elimina a los clientes que ya cumplieron el plazo y fueron avisados.
//
//  Secretos (Supabase → Edge Functions → Secrets):
//    RESEND_API_KEY    clave de Resend (https://resend.com)
//    CORREO_REMITENTE  p. ej. "Axolotl Fitness Club <avisos@axolotlgym.com>"
//    CRON_SECRET       texto largo cualquiera; el mismo que se guarda en el Vault
//  SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY ya los da Supabase.
//
//  Si enviar-bienvenida usa otro proveedor de correo, cambia solo enviarCorreo().
// =====================================================================
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

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

async function enviarCorreo(para: string, asunto: string, html: string): Promise<boolean> {
  const r = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${Deno.env.get("RESEND_API_KEY")}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ from: Deno.env.get("CORREO_REMITENTE"), to: [para], subject: asunto, html }),
  });
  if (!r.ok) console.error("Correo no enviado", para, r.status, await r.text());
  return r.ok;
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

  return Response.json({ avisos: avisos?.length ?? 0, enviados, eliminados });
});

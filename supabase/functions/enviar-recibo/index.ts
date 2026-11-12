// =====================================================================
//  AXOLOTL FITNESS CLUB · Edge Function "enviar-recibo"
//  La app la llama justo después de renovar un plan en Recepción:
//    POST { "cliente_id": 123 }
//  Toma el pago recién registrado (sql/recibo_pago.sql) y manda el recibo al
//  correo que el cliente tiene registrado. La app no decide el destinatario.
//
//  Usa los mismos secretos que avisos-diarios:
//    RESEND_API_KEY, CORREO_REMITENTE
//  SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY ya los da Supabase.
// =====================================================================
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

type Recibo = {
  r_pago_id: number;
  r_nombre: string;
  r_gmail: string;
  r_numero_membresia: string | null;
  r_plan: string | null;
  r_monto: number;
  r_metodo: string;
  r_fecha_pago: string;
  r_vigencia_inicio: string | null;
  r_vigencia_fin: string | null;
  r_atendio: string | null;
};

const fecha = (iso: string | null) => {
  if (!iso) return "—";
  const [a, m, d] = iso.slice(0, 10).split("-");
  return `${d}/${m}/${a}`;
};

const fechaHora = (iso: string) =>
  new Date(iso).toLocaleString("es-MX", {
    timeZone: "America/Mexico_City",
    day: "2-digit", month: "2-digit", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });

const dinero = (v: number) =>
  "$" + v.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const esc = (s: string | null | undefined) =>
  (s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));

function plantilla(r: Recibo): { asunto: string; html: string } {
  const folio = String(r.r_pago_id).padStart(6, "0");
  const fila = (k: string, v: string) =>
    `<tr><td style="padding:8px 0;color:#5B5E66">${k}</td>` +
    `<td style="padding:8px 0;text-align:right;color:#1B1C1F;font-weight:600">${v}</td></tr>`;

  return {
    asunto: `Comprobante de pago · Axolotl Fitness Club · Folio ${folio}`,
    html: `<div style="font-family:Arial,Helvetica,sans-serif;background:#F1EFEB;padding:24px">
  <div style="max-width:480px;margin:0 auto;background:#FAF9F7;border:1.5px solid #C9B26A;border-radius:16px;padding:24px">
    <h2 style="margin:0 0 4px;color:#1B1C1F">Comprobante de pago</h2>
    <p style="margin:0 0 16px;color:#8E723A;font-weight:600">Axolotl Fitness Club · Folio ${folio}</p>
    <p style="margin:0 0 16px;color:#1B1C1F">Hola ${esc(r.r_nombre.split(" ")[0])}, gracias por tu pago. Este es tu comprobante:</p>
    <table style="width:100%;border-collapse:collapse;font-size:14px">
      ${fila("Cliente", esc(r.r_nombre))}
      ${fila("Membresía", esc(r.r_numero_membresia) || "—")}
      ${fila("Plan", esc(r.r_plan) || "—")}
      ${fila("Vigencia", `${fecha(r.r_vigencia_inicio)} al ${fecha(r.r_vigencia_fin)}`)}
      ${fila("Método de pago", esc(r.r_metodo))}
      ${fila("Fecha de pago", fechaHora(r.r_fecha_pago))}
      ${fila("Atendió", esc(r.r_atendio) || "Recepción")}
    </table>
    <table style="width:100%;margin-top:16px;border-collapse:separate;background:#8E723A;border-radius:12px">
      <tr>
        <td style="padding:14px;color:#FFFFFF;font-size:15px">Total pagado</td>
        <td style="padding:14px;color:#FFFFFF;font-size:20px;font-weight:700;text-align:right">${dinero(r.r_monto)} MXN</td>
      </tr>
    </table>
    <p style="margin:20px 0 0;color:#5B5E66;font-size:12px">Conserva este correo como comprobante.
    Axolotl Fitness Club · contacto@axolotlgym.com · +52 314 139 7633</p>
  </div>
</div>`,
  };
}

/** null si salió; si no, el motivo (lo que respondió Resend) para mostrarlo en la app. */
async function enviarCorreo(para: string, asunto: string, html: string): Promise<string | null> {
  if (!Deno.env.get("RESEND_API_KEY")) return "Falta el secreto RESEND_API_KEY en Edge Functions → Secrets";
  if (!Deno.env.get("CORREO_REMITENTE")) return "Falta el secreto CORREO_REMITENTE en Edge Functions → Secrets";
  const r = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${Deno.env.get("RESEND_API_KEY")}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ from: Deno.env.get("CORREO_REMITENTE"), to: [para], subject: asunto, html }),
  });
  if (r.ok) return null;
  const texto = await r.text();
  console.error("Recibo no enviado", para, r.status, texto);
  let detalle = texto;
  try { detalle = JSON.parse(texto).message ?? texto; } catch { /* texto plano */ }
  return `Resend ${r.status}: ${detalle}`.slice(0, 300);
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Método no permitido", { status: 405 });

  let clienteId: number;
  try {
    clienteId = Number((await req.json()).cliente_id);
  } catch {
    return new Response("JSON inválido", { status: 400 });
  }
  if (!Number.isInteger(clienteId) || clienteId <= 0) {
    return new Response("cliente_id inválido", { status: 400 });
  }

  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data, error } = await db.rpc("recibo_por_enviar", { p_cliente_id: clienteId });
  if (error) return new Response(error.message, { status: 500 });

  const recibo = (data ?? [])[0] as Recibo | undefined;
  if (!recibo) return Response.json({ enviado: false, motivo: "Sin pago reciente por enviar" }, { status: 404 });
  if (!recibo.r_gmail) return Response.json({ enviado: false, motivo: "El cliente no tiene correo" }, { status: 422 });

  const { asunto, html } = plantilla(recibo);
  const falla = await enviarCorreo(recibo.r_gmail, asunto, html);
  if (falla) return Response.json({ enviado: false, motivo: falla }, { status: 502 });

  await db.rpc("marcar_recibo_enviado", { p_pago_id: recibo.r_pago_id });
  return Response.json({ enviado: true, folio: recibo.r_pago_id });
});

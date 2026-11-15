-- =====================================================================
--  AXOLOTL FITNESS CLUB · Comunicados por correo
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--  Requiere pendientes_noviembre.sql (avisos-diarios) ya corrido.
--
--  El Encargado publica un comunicado desde la sección "Avisos" y se manda por
--  correo a todos los usuarios con cuenta activa (Edge Function enviar-comunicados):
--    · TERMINOS   cambio en los Términos y Condiciones  (se recomiendan 15 días antes)
--    · PRIVACIDAD cambio en la Política de Privacidad   (se recomiendan 15 días antes)
--    · HORARIO    cambio de horario o cierre            (se recomiendan 3 días antes)
--  La fecha en que aplica puede ser desde hoy; la app advierte si es menor a la
--  anticipación del punto 25 de los Términos y el 12 de la Política.
--
--  Cada envío queda registrado por usuario: si un correo falla (o se llega al
--  límite diario del proveedor) se reintenta en la siguiente ejecución, que
--  también hace avisos-diarios cada mañana.
--
--  El aviso de eliminación de cuenta (30 días antes de cumplir 365 días sin uso)
--  ya lo manda avisos-diarios; este archivo no lo cambia.
-- =====================================================================

create table if not exists comunicados (
  id          bigint generated always as identity primary key,
  tipo        text not null check (tipo in ('TERMINOS', 'PRIVACIDAD', 'HORARIO')),
  titulo      text not null,
  mensaje     text not null,
  fecha_vigor date not null,                 -- desde cuándo aplica el cambio
  creado_por  bigint references usuarios(id) on delete set null,
  creado_en   timestamptz not null default now()
);
alter table comunicados enable row level security;

create table if not exists comunicados_envios (
  comunicado_id bigint not null references comunicados(id) on delete cascade,
  usuario_id    bigint not null references usuarios(id) on delete cascade,
  enviado_en    timestamptz not null default now(),
  primary key (comunicado_id, usuario_id)
);
alter table comunicados_envios enable row level security;

-- Destinatarios: usuarios con cuenta activa que ya existían cuando se publicó
-- (quien se registra después acepta los términos vigentes al entrar).
create or replace function _destinatarios_comunicado(p_comunicado_id bigint)
returns table (usuario_id bigint, nombre text, gmail text)
language sql stable security definer set search_path = public as $$
  select u.id, u.nombre, u.gmail
    from comunicados co
    join usuarios u on u.estado = 'ACTIVO'
                   and coalesce(trim(u.gmail), '') <> ''
                   and u.fecha_registro <= co.creado_en
   where co.id = p_comunicado_id
$$;

-- El Encargado publica el comunicado. Devuelve su id.
create or replace function crear_comunicado(
  p_actor_gmail text, p_tipo text, p_titulo text, p_mensaje text, p_fecha_vigor date
)
returns bigint language plpgsql security definer set search_path = public as $$
declare v_actor bigint; v_id bigint;
begin
  v_actor := _id_usuario(p_actor_gmail);
  if not _tiene_rol(v_actor, array['ENCARGADO']) then
    raise exception 'Solo el Encargado puede publicar comunicados';
  end if;
  if p_tipo not in ('TERMINOS', 'PRIVACIDAD', 'HORARIO') then
    raise exception 'Tipo de comunicado no válido';
  end if;
  if length(trim(coalesce(p_titulo, ''))) < 3 then
    raise exception 'Escribe el título del comunicado';
  end if;
  if length(trim(coalesce(p_mensaje, ''))) < 10 then
    raise exception 'Describe el cambio (mínimo 10 caracteres)';
  end if;

  if p_fecha_vigor is null or p_fecha_vigor < _hoy_fecha_mx() then
    raise exception 'La fecha en que aplica el cambio no puede ser anterior a hoy';
  end if;

  insert into comunicados (tipo, titulo, mensaje, fecha_vigor, creado_por)
  values (p_tipo, trim(p_titulo), trim(p_mensaje), p_fecha_vigor, v_actor)
  returning id into v_id;
  return v_id;
end $$;

-- Lista para la sección "Avisos" del Encargado, con el avance del envío
drop view if exists comunicados_lista;
create view comunicados_lista as
 select co.id, co.tipo, co.titulo, co.mensaje, co.fecha_vigor, co.creado_en,
        u.nombre as publico,
        -- Mismo criterio que _destinatarios_comunicado (esa función no se expone:
        -- devuelve correos, y en una vista se ejecuta con los permisos de quien consulta)
        (select count(*) from usuarios d
          where d.estado = 'ACTIVO' and coalesce(trim(d.gmail), '') <> ''
            and d.fecha_registro <= co.creado_en)::int as destinatarios,
        (select count(*) from comunicados_envios e where e.comunicado_id = co.id)::int as enviados
   from comunicados co
   left join usuarios u on u.id = co.creado_por;

-- Correos por enviar (solo la Edge Function, con la service role).
-- Solo comunicados que todavía no entran en vigor o entraron hace menos de 7 días.
create or replace function comunicados_pendientes(p_limite int default 100)
returns table (r_comunicado_id bigint, r_usuario_id bigint, r_nombre text, r_gmail text,
               r_tipo text, r_titulo text, r_mensaje text, r_fecha_vigor date)
language sql stable security definer set search_path = public as $$
  select co.id, d.usuario_id, d.nombre, d.gmail, co.tipo, co.titulo, co.mensaje, co.fecha_vigor
    from comunicados co
    cross join lateral _destinatarios_comunicado(co.id) d
   where co.fecha_vigor >= _hoy_fecha_mx() - 7
     and not exists (select 1 from comunicados_envios e
                      where e.comunicado_id = co.id and e.usuario_id = d.usuario_id)
   order by co.id, d.usuario_id
   limit greatest(coalesce(p_limite, 100), 1)
$$;

create or replace function marcar_comunicado_enviado(p_comunicado_id bigint, p_usuario_ids bigint[])
returns void language sql security definer set search_path = public as $$
  insert into comunicados_envios (comunicado_id, usuario_id)
  select p_comunicado_id, unnest(p_usuario_ids)
  on conflict do nothing
$$;

grant execute on function crear_comunicado(text, text, text, text, date) to anon, authenticated;
grant select on comunicados_lista to anon, authenticated;
revoke all on function comunicados_pendientes(int) from public, anon, authenticated;
revoke all on function marcar_comunicado_enviado(bigint, bigint[]) from public, anon, authenticated;
revoke all on function _destinatarios_comunicado(bigint) from public, anon, authenticated;
grant execute on function comunicados_pendientes(int) to service_role;
grant execute on function marcar_comunicado_enviado(bigint, bigint[]) to service_role;

notify pgrst, 'reload schema';

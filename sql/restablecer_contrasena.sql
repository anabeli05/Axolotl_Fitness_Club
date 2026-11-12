-- =====================================================================
--  AXOLOTL FITNESS CLUB · Cambiar la contraseña sin recordar la actual
--  Supabase → SQL Editor → pegar todo → Run (se puede correr varias veces)
--
--  1. En la app se escribe el correo y la contraseña nueva → "Enviar código".
--  2. La Edge Function "enviar-codigo-contrasena" crea un código de 6 dígitos
--     (crear_codigo_contrasena) y lo manda al correo registrado.
--  3. Se escribe el código en la app → restablecer_contrasena cambia la contraseña.
--
--  Funciona para cualquier usuario registrado, con la cuenta activa o inactiva.
--
--  Seguridad: el código se guarda cifrado, dura 15 minutos, admite 5 intentos,
--  se pide como máximo uno por minuto y se borra al usarse.
-- =====================================================================

create table if not exists codigos_contrasena (
  usuario_id  bigint primary key references usuarios(id) on delete cascade,
  codigo_hash text not null,
  expira      timestamptz not null,
  intentos    int not null default 0,
  creado      timestamptz not null default now()
);
alter table codigos_contrasena enable row level security;  -- sin políticas: solo funciones

-- La app revisa la contraseña nueva ANTES de pedir el código ('OK' o el motivo)
create or replace function revisar_contrasena_nueva(p_nueva text)
returns text language plpgsql security definer set search_path = public, extensions as $$
begin
  if p_nueva = 'Axolotl2026' then
    return 'Elige una contraseña distinta a la temporal';
  end if;
  perform validar_password(p_nueva);
  return 'OK';
exception when others then
  return sqlerrm;
end $$;

-- Solo la Edge Function: genera el código y regresa a quién mandarlo
drop function if exists crear_codigo_contrasena(text);
create function crear_codigo_contrasena(p_gmail text)
returns table (r_nombre text, r_gmail text, r_codigo text)
language plpgsql security definer set search_path = public, extensions as $$
declare v_id bigint; v_nombre text; v_gmail text; v_codigo text;
begin
  select id, nombre, gmail into v_id, v_nombre, v_gmail
    from usuarios where lower(gmail) = lower(trim(coalesce(p_gmail, '')));   -- activo o inactivo
  if v_id is null then return; end if;   -- no se revela si el correo existe

  if exists (select 1 from codigos_contrasena
              where usuario_id = v_id and creado > now() - interval '60 seconds') then
    raise exception 'Espera un minuto antes de pedir otro código';
  end if;

  -- 6 dígitos con un generador seguro
  v_codigo := lpad(((('x' || encode(gen_random_bytes(4), 'hex'))::bit(32)::bigint) % 1000000)::text, 6, '0');

  insert into codigos_contrasena (usuario_id, codigo_hash, expira, intentos, creado)
  values (v_id, crypt(v_codigo, gen_salt('bf', 8)), now() + interval '15 minutes', 0, now())
  on conflict (usuario_id) do update
    set codigo_hash = excluded.codigo_hash, expira = excluded.expira, intentos = 0, creado = now();

  return query select v_nombre, v_gmail, v_codigo;
end $$;

-- La app: con el código correcto se cambia la contraseña ('OK' o el motivo)
create or replace function restablecer_contrasena(p_gmail text, p_codigo text, p_nueva text)
returns text language plpgsql security definer set search_path = public, extensions as $$
declare v_id bigint; c codigos_contrasena%rowtype;
begin
  select id into v_id from usuarios
   where lower(gmail) = lower(trim(coalesce(p_gmail, '')));   -- activo o inactivo
  if v_id is not null then
    select * into c from codigos_contrasena where usuario_id = v_id for update;
  end if;

  if v_id is null or c.usuario_id is null or c.expira < now() then
    return 'El código expiró o no es válido. Pide uno nuevo.';
  end if;
  if c.intentos >= 5 then
    delete from codigos_contrasena where usuario_id = v_id;
    return 'Demasiados intentos. Pide un código nuevo.';
  end if;
  if c.codigo_hash <> crypt(coalesce(trim(p_codigo), ''), c.codigo_hash) then
    update codigos_contrasena set intentos = intentos + 1 where usuario_id = v_id;
    return 'Código incorrecto. Te quedan ' || (4 - c.intentos) || ' intentos.';
  end if;

  if p_nueva = 'Axolotl2026' then
    return 'Elige una contraseña distinta a la temporal';
  end if;
  perform validar_password(p_nueva);   -- si no cumple, el código sigue sirviendo

  -- El trigger de usuarios la cifra
  update usuarios set contrasena = p_nueva, intentos_fallidos = 0, bloqueado_hasta = null
   where id = v_id;
  delete from codigos_contrasena where usuario_id = v_id;
  return 'OK';
end $$;

revoke all on function crear_codigo_contrasena(text) from public, anon, authenticated;
grant execute on function crear_codigo_contrasena(text) to service_role;
grant execute on function revisar_contrasena_nueva(text) to anon, authenticated;
grant execute on function restablecer_contrasena(text, text, text) to anon, authenticated;

notify pgrst, 'reload schema';

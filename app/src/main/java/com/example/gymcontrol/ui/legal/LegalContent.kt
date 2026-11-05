package com.example.gymcontrol.ui.legal

data class LegalSection(
    val title: String,
    val content: String
)

object LegalContent {

    val terminosYCondiciones = listOf(
        LegalSection(
            title = "1. Aceptación de los Términos",
            // PENDIENTE: confirmar exactamente cómo será la aceptación (casilla en el login).
            content = "Al acceder y utilizar la aplicación Axolotl Fitness Club (GymControl), el usuario declara haber leído, entendido y aceptado en su totalidad los presentes Términos y Condiciones de Uso Responsable. En caso de no estar de acuerdo, deberá abstenerse de utilizar la plataforma. La aceptación se hace en la pantalla de inicio de sesión, mediante una casilla que debe marcarse para poder iniciar sesión."
        ),
        LegalSection(
            title = "2. Objeto del Servicio",
            content = "La aplicación es un servicio móvil para Android de Axolotl Fitness Club, que cuenta con sucursales. Permite controlar el acceso de los clientes, administrar membresías, solicitar asesorías con instructores, registrar pagos y generar reportes."
        ),
        LegalSection(
            title = "3. Roles de Usuario",
            content = "La aplicación tiene cuatro tipos de usuario (cliente, instructor, recepción y dueño o encargado), y cada uno solo accede a las funciones de su rol."
        ),
        LegalSection(
            title = "4. Capacidad para Usar la Aplicación",
            content = "Solo pueden usar la aplicación personas de 18 años o más."
        ),
        LegalSection(
            title = "5. Uso Responsable de la Cuenta",
            // PENDIENTE: confirmar que ya estén implementados los requisitos de contraseña y el bloqueo de 15 minutos tras 5 intentos fallidos.
            content = "La cuenta y credenciales de acceso son de carácter personal e intransferible. El usuario es el único responsable de mantener la confidencialidad de sus credenciales y de toda actividad realizada desde su cuenta. Las cuentas las da de alta el personal; no hay autorregistro. La contraseña debe tener mínimo 8 caracteres, con mayúscula, minúscula, número y símbolo. Una cuenta inactiva no puede iniciar sesión. Tras 5 intentos fallidos, la cuenta se bloquea durante 15 minutos."
        ),
        LegalSection(
            title = "6. Pagos, Membresías y Cancelaciones",
            // PENDIENTE: confirmar nombres y duraciones de las membresías (los precios actuales no son los definitivos).
            // PENDIENTE: definir el número de días (x) de inactividad para poder eliminar una cuenta.
            // PENDIENTE: definir dónde y cómo se realiza físicamente cada pago.
            // PENDIENTE: definir qué servicios adicionales hay y a qué precio.
            // PENDIENTE: definir política de cancelaciones, reembolsos, facturación e impuestos.
            // PENDIENTE: definir si la membresía es válida en todas las sucursales, si se puede transferir a otra persona y si se puede congelar.
            content = "Las tarifas, vigencias de suscripciones y condiciones de renovación se rigen según el plan contratado. El acceso a las instalaciones está sujeto a encontrarse al corriente con el pago de la membresía correspondiente.\n\n" +
                    "Existen membresías de un día, mensual y trimestral. Una membresía está activa cuando su estado es activa y su fecha de vencimiento es la del día o posterior. Si está vencida, no se permite el ingreso hasta que se renueve. La renovación la hacen recepción o el encargado.\n\n" +
                    "Solo el encargado da de baja a un cliente, y la cuenta queda inactiva. Una cuenta inactiva por un tiempo determinado puede ser eliminada de forma manual por recepción o por el encargado.\n\n" +
                    "Se aceptan pagos en efectivo, con tarjeta y por transferencia. Recepción confirma los pagos en efectivo y con tarjeta; el encargado confirma las transferencias y también puede confirmar los otros dos. Un pago queda pendiente, aceptado o rechazado. Los montos se expresan en pesos mexicanos. La aplicación registra los pagos y no procesa cobros en línea. Los servicios adicionales, como el uso del instructor, se cobran aparte."
        ),
        LegalSection(
            title = "7. Acceso al Gimnasio",
            // PENDIENTE: el reconocimiento facial aún no está implementado.
            // PENDIENTE: definir consecuencias de intentar suplantar a otra persona.
            // PENDIENTE: definir qué hacer si el sistema no reconoce a un cliente registrado.
            // PENDIENTE: definir qué ocurre con quien no acepte el reconocimiento facial (posible: se rechaza su ingreso).
            content = "El cliente es identificado en recepción mediante reconocimiento facial. Recepción acepta o rechaza el ingreso y se reserva el derecho de rechazarlo. La aplicación registra la entrada y la salida. Está prohibido intentar entrar suplantando a otra persona."
        ),
        LegalSection(
            title = "8. Aforo y Horarios de Afluencia",
            // PENDIENTE: definir si el aforo se cuenta por sucursal.
            // PENDIENTE: definir el uso de la pantalla de horarios de menor afluencia (hoy estática).
            content = "La aplicación muestra una cantidad aproximada de personas dentro del gimnasio, calculada con los registros de entrada y salida. Es informativa y puede no coincidir exactamente con la real."
        ),
        LegalSection(
            title = "9. Asesorías con Instructores",
            // PENDIENTE: las asesorías aún no están implementadas.
            // PENDIENTE: definir cancelación y forma de pago de las asesorías.
            content = "El cliente solicita una asesoría con un instructor y la solicitud se aprueba o se rechaza. El costo lo define cada instructor y cada instructor tiene un máximo de clientes. El instructor define con cada cliente los días de asistencia y su objetivo."
        ),
        LegalSection(
            title = "10. Avisos y Notificaciones",
            // PENDIENTE: definir medio del aviso y con cuánta anticipación se envía.
            content = "El aviso de vencimiento de la membresía se envía al cliente fuera de la aplicación."
        ),
        LegalSection(
            title = "11. Normas de Convivencia y Seguridad en las Instalaciones",
            // PENDIENTE: definir dónde están escritas las normas internas y las consecuencias de incumplirlas.
            content = "El uso del gimnasio exige respetar las normas internas de higiene, seguridad y uso adecuado de los equipos y pesas. Queda estrictamente prohibido el comportamiento ofensivo o que atente contra la integridad física de los usuarios y el personal."
        ),
        LegalSection(
            title = "12. Salud y Condición Física",
            content = "El usuario reconoce que la actividad física implica riesgos inherentes. Es responsabilidad del usuario consultar con un médico antes de iniciar cualquier programa de entrenamiento y notificar cualquier condición médica relevante a los instructores. La aplicación no incluye seguimiento de progreso físico ni sustituye la opinión médica."
        ),
        LegalSection(
            title = "13. Uso Aceptable de la Aplicación",
            // PENDIENTE: revisar si se agregan más prohibiciones.
            content = "No se debe compartir la cuenta, intentar entrar a funciones de otro rol, manipular los datos del sistema ni usar la aplicación con fines ilícitos."
        ),
        LegalSection(
            title = "14. Propiedad Intelectual",
            // PENDIENTE: confirmar si existe registro ante el INDAUTOR y, en su caso, agregar su número.
            content = "La aplicación, su diseño, su código, el nombre y el logotipo Axolotl están protegidos por derechos de autor. © 2026 Axolotl. Todos los derechos reservados. Queda prohibido copiar, modificar, distribuir o hacer ingeniería inversa de la aplicación sin autorización por escrito de Axolotl."
        ),
        LegalSection(
            title = "15. Disponibilidad del Servicio",
            // PENDIENTE: redactar los límites de responsabilidad, con revisión legal.
            content = "La aplicación depende de internet y de un servicio en la nube, por lo que no se garantiza que funcione sin interrupciones ni que esté disponible durante el mantenimiento."
        ),
        LegalSection(
            title = "16. Datos Personales",
            content = "El tratamiento de los datos personales se rige por la Política de Privacidad."
        ),
        LegalSection(
            title = "17. Suspensión y Terminación",
            // PENDIENTE: definir otras causas de suspensión.
            content = "La baja de un cliente la hace el encargado y deja la cuenta inactiva."
        ),
        LegalSection(
            title = "18. Modificaciones a los Términos",
            content = "El gimnasio se reserva el derecho de modificar estos términos en cualquier momento. Las modificaciones entrarán en vigor a partir de su publicación en la aplicación."
        ),
        LegalSection(
            title = "19. Contacto",
            // PENDIENTE: el correo es provisional; crear el correo real o cambiarlo.
            // PENDIENTE: confirmar el número de teléfono (parece incompleto).
            content = "Teléfono: +52 314 139 7633\nCorreo: contacto@axolotlgym.com"
        )

        // PENDIENTE (agregar cuando se defina, numerar como 20): "Ley Aplicable y Jurisdicción" - definir ley, tribunales, ciudad o estado.
        // PENDIENTE (agregar al final cuando se defina): fecha de vigencia de los términos.
    )

    val politicaDePrivacidad = listOf(
        LegalSection(
            title = "1. Responsable del Tratamiento de los Datos",
            content = "Axolotl Fitness Club, gimnasio con sucursales. El tratamiento de los datos se realiza para todas sus sucursales. Los medios de contacto del responsable son el teléfono y el correo indicados en el punto 12."
        ),
        LegalSection(
            title = "2. Información que Recopilamos",
            // PENDIENTE: definir dónde se almacena la fotografía de perfil.
            // PENDIENTE: definir qué datos del contacto de emergencia se piden (por ejemplo nombre y teléfono).
            // PENDIENTE: confirmar el nombre del campo facial y que se guarde la representación numérica y no la fotografía.
            // PENDIENTE: confirmar que la app no guarda números de tarjeta ni datos bancarios.
            // PENDIENTE: eliminar de la base de datos (o dejar sin uso) los campos fecha_nacimiento y condiciones_medicas.
            content = "Recopilamos la información necesaria para la prestación del servicio:\n" +
                    "a) De todos los usuarios: nombre completo, correo electrónico, número de teléfono, contraseña (se guarda con hash y no puede leerse), rol, estado de la cuenta, fecha de registro, intentos fallidos de acceso y fecha de bloqueo temporal.\n" +
                    "b) Fotografía de perfil, que pueden asignar el cliente y el instructor.\n" +
                    "c) Del cliente: tipo de membresía, fechas de inicio y vencimiento y estado de la membresía; registro de asistencias (fecha, hora, tipo entrada o salida y método de registro); pagos (monto, tipo de pago, estado, quién lo confirmó y fecha); solicitudes de asesoría y asesorías activas (instructor, días de asistencia y objetivo); y un contacto de emergencia. El cliente declara contar con la autorización de la persona cuyo dato proporciona como contacto de emergencia.\n" +
                    "d) Datos biométricos: representación numérica del rostro, usada para identificar al cliente al entrar.\n" +
                    "e) Del instructor: especialidad, horario, costo de la asesoría y máximo de clientes que puede atender.\n" +
                    "f) Datos de pago: monto, tipo y estado del pago.\n" +
                    "g) No se recaban fecha de nacimiento ni condiciones médicas."
        ),
        LegalSection(
            title = "3. Finalidad del Tratamiento de Datos",
            // PENDIENTE: definir medio del aviso de vencimiento y qué dato se usa para enviarlo.
            // PENDIENTE: confirmar la finalidad (f), contactar al contacto de emergencia.
            content = "Los datos personales se utilizan exclusivamente para los fines de Axolotl:\n" +
                    "(a) autenticación e identificación del cliente para el control de acceso a las instalaciones mediante reconocimiento facial;\n" +
                    "(b) administración de membresías y pagos;\n" +
                    "(c) asignación y seguimiento de asesorías con instructores;\n" +
                    "(d) cálculo de la cantidad de personas dentro del gimnasio y generación de reportes de ingresos y gastos;\n" +
                    "(e) envío de notificaciones importantes sobre el servicio, como el aviso de vencimiento de la membresía, fuera de la aplicación; y\n" +
                    "(f) contactar a la persona indicada en caso de una emergencia del cliente.\n\n" +
                    "Los datos no se usan para ningún otro fin y no existen finalidades secundarias, como promociones o publicidad."
        ),
        LegalSection(
            title = "4. Datos Biométricos (Reconocimiento Facial)",
            // PENDIENTE: definir el mecanismo del consentimiento expreso y separado al registrar el rostro.
            // PENDIENTE: definir qué ocurre si el cliente no acepta el reconocimiento facial (posible: se rechaza su ingreso).
            // PENDIENTE: definir cómo se revoca el consentimiento y qué ocurre con el dato facial.
            content = "El dato facial se usa solo para identificar al cliente al ingresar al gimnasio. Su consentimiento se solicita de forma expresa y separada al registrar el rostro del cliente."
        ),
        LegalSection(
            title = "5. Quién Puede Ver los Datos",
            // PENDIENTE: confirmar si el instructor ve más datos que nombre, días y objetivo.
            // PENDIENTE: confirmar si recepción ve el contacto de emergencia.
            content = "a) El cliente ve sus propios datos.\n" +
                    "b) El instructor ve a los clientes que tiene asignados: nombre, días de asistencia y objetivo.\n" +
                    "c) Recepción ve nombre, tipo de membresía, fecha de vencimiento y foto del cliente al identificarlo, y puede crear, editar y renovar clientes.\n" +
                    "d) El dueño o encargado administra usuarios, membresías, pagos, servicios y gastos y consulta los reportes."
        ),
        LegalSection(
            title = "6. No Compartimos sus Datos con Terceros",
            // PENDIENTE: confirmar la ubicación de los servidores de Supabase.
            content = "Los datos no se venden, no se comparten y no se transfieren a terceros con ningún fin. La base de datos se aloja en Supabase, proveedor de servicio en la nube, únicamente para su almacenamiento."
        ),
        LegalSection(
            title = "7. Protección y Confidencialidad",
            // PENDIENTE: confirmar que ya esté implementado el bloqueo de 15 minutos tras 5 intentos fallidos.
            // PENDIENTE: confirmar si la comunicación con la base de datos viaja cifrada.
            content = "Implementamos medidas de seguridad técnicas y administrativas para proteger la información personal contra acceso no autorizado, alteración, pérdida o divulgación indebida. En particular: las contraseñas se almacenan con hash; la aplicación no puede leerlas ni modificar las tablas de usuarios y roles; el rol se valida tanto en la aplicación como en funciones de la base de datos; y la cuenta se bloquea 15 minutos tras 5 intentos fallidos."
        ),
        LegalSection(
            title = "8. Conservación y Eliminación de Datos",
            // PENDIENTE: definir el número de días (x) de inactividad para poder eliminar una cuenta.
            // PENDIENTE: definir qué datos se borran al eliminar el perfil y si algún registro, como los pagos, se conserva por obligaciones contables.
            content = "Al dar de baja a un cliente, su cuenta queda inactiva. Una cuenta inactiva durante un tiempo determinado puede ser eliminada de forma manual por recepción o por el encargado."
        ),
        LegalSection(
            title = "9. Derechos ARCO (Acceso, Rectificación, Cancelación y Oposición)",
            // PENDIENTE: definir el plazo de respuesta.
            // PENDIENTE: el correo es provisional; crear el correo real o cambiarlo.
            content = "El titular de los datos tiene derecho a consultar, rectificar o solicitar la eliminación de su información personal, a oponerse a su tratamiento y a revocar su consentimiento. Para ello puede dirigirse a la recepción de cualquier sucursal o comunicarse al teléfono +52 314 139 7633 o al correo contacto@axolotlgym.com."
        ),
        LegalSection(
            title = "10. Mayoría de Edad",
            // PENDIENTE: definir cómo se verifica la mayoría de edad en el alta y qué se hace si se detecta a un menor.
            content = "La aplicación está dirigida únicamente a personas de 18 años o más y no recaba datos de menores."
        ),
        LegalSection(
            title = "11. Cambios en la Política de Privacidad",
            content = "Nos reservamos el derecho de actualizar la presente Política de Privacidad en cualquier momento. Cualquier cambio será notificado oportunamente a través de la plataforma."
        ),
        LegalSection(
            title = "12. Contacto en Materia de Privacidad",
            // PENDIENTE: el correo es provisional; crear el correo real o cambiarlo.
            // PENDIENTE: confirmar el número de teléfono (parece incompleto).
            content = "Teléfono: +52 314 139 7633\nCorreo: contacto@axolotlgym.com"
        ),
        LegalSection(
            title = "13. Aceptación",
            // PENDIENTE: confirmar exactamente cómo será la aceptación (casilla en el login).
            content = "La Política de Privacidad y los Términos y Condiciones se muestran en la pantalla de inicio de sesión mediante enlaces, y el usuario debe marcar una casilla de aceptación para poder iniciar sesión."
        ),
        LegalSection(
            title = "14. Marco Legal",
            // PENDIENTE: verificar la ley vigente y su aplicación al proyecto.
            content = "El tratamiento de los datos personales se rige por la legislación mexicana de protección de datos personales en posesión de particulares."
        )

        // PENDIENTE (agregar al final cuando se defina): fecha de última actualización de la política.
    )
}
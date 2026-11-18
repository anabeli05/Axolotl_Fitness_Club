# GymControlBase

Proyecto base para Android Studio creado en Kotlin + Jetpack Compose.

GymControl es la app de Axolotl Fitness Club. Resuelve cuatro problemas de un gimnasio:

Acceso: entrada y salida automáticas con la cámara de recepción (sin tarjetas ni QR).
Cobros: membresías, visitas de un día y asesorías, con validación de transferencias/depósitos por el encargado.
Control: aforo en tiempo real, horarios de menor afluencia, ingresos, gastos y reembolsos.
Comunicación: correos de bienvenida, recibos, vencimientos y avisos generales.

La app está en español e inglés y tiene tema claro y oscuro. Incluye un manual de usuario interactivo dentro de la app.

## Qué incluye

### Cliente
- Login por número de membresía y contraseña.
- Inicio con personas dentro del gimnasio (acceso por reconocimiento facial), horario y ocupación por hora.
- Asesorías con instructores, costo y botón Solicitar/Pendiente.
- Perfil editable, membresía, estado y cambio de contraseña.

### Recepción
- Login por correo y contraseña.
- Vista principal de acceso por reconocimiento facial (entrada = 1 / salida = 0, automático).
- Registro del rostro del cliente desde Clientes (tocar la foto del cliente).
- Clientes con buscador, membresía, fecha de registro, estado y renovación.
- Formulario para nuevo cliente.
- Solicitudes de asesoría con aprobación/rechazo.

### Instructor
- Mis com.example.gymcontrol.ui.encargado.clientes: asesorías activas, días, objetivo y editar.
- Perfil y capacidad máxima.

### Encargado / Dueño
- Dashboard con com.example.gymcontrol.ui.encargado.clientes activos, inactivos, ingresos y gráficas base.
- Usuarios: listado, editar/eliminar y formulario para alta.
- Campos extra de costo/capacidad cuando el rol es Instructor.
- Servicios.
- Gastos.
- Reporte mensual.

## Arquitectura

```
com.example.gymcontrol/
├── data/
│   ├── model/
│   ├── remote/
│   └── repository/
├── navigation/
└── ui/
    ├── auth/
    ├── cliente/
    ├── recepcion/
    ├── instructor/
    ├── encargado/
    ├── components/
    └── theme/
```

Cada pantalla importante tiene su propia carpeta, por lo que puedes trabajar por módulos sin concentrar todo en un solo archivo.

## Datos

Actualmente usa `FakeGymRepository`, con datos de ejemplo.

Cuando tengas backend:

1. Crea tus DTOs en `data/remote/dto`.
2. Agrega Retrofit/OkHttp.
3. Crea `RemoteGymRepository : GymRepository`.
4. Sustituye en `GymApp.kt`:
   `val repository: GymRepository = FakeGymRepository()`
   por tu repositorio remoto.
5. Los Composables pueden conservarse y empezar a consumir ViewModels.

## Importante sobre la base de datos

No conectes la aplicación Android directamente a MySQL o PostgreSQL porque tendrías que exponer usuario, contraseña y acceso de la BD dentro de la APK.

La estructura recomendada es:

```
Android Kotlin
      |
      | HTTPS / JSON
      v
Backend API
(Node.js / Spring Boot / Laravel / etc.)
      |
      v
MySQL / PostgreSQL
```

Si usas Firebase/Supabase, la integración es distinta porque cuentan con SDK/API para com.example.gymcontrol.ui.encargado.clientes.

## Navegación de desarrollo

Hay un botón flotante `☰` temporal que permite saltar entre todas las vistas para que puedas trabajar en el diseño sin tener que cerrar sesión cada vez.

Cuando termines la app, elimina `RoleDevMenu()` de `GymNavGraph.kt` y crea la navegación inferior/drawer definitiva según cada rol.

## Requisitos

- Android Studio compatible con API 37.
- JDK 17.
- compileSdk 37.
- Kotlin 2.3.21.
- Compose BOM 2026.08.00.
- Navigation Compose 2.10.0.

## Siguiente etapa recomendada

1. Diseñar cada pantalla.
2. Crear ViewModels.
3. Definir contrato real del backend.
4. Implementar autenticación.
5. Implementar reconocimiento facial/cámara (ver `sql/reconocimiento_facial.sql`).
6. Conectar CRUD de com.example.gymcontrol.ui.encargado.clientes, usuarios, servicios y gastos.
7. Implementar solicitudes y reportes.

## Reconocimiento facial

- Base de datos: correr una vez `sql/reconocimiento_facial.sql` en Supabase (SQL Editor).
- Modelo: `app/src/main/assets/mobilefacenet.tflite` (MobileFaceNet, licencia BSD-3, de FaceRecognitionAuth de Marcos Carlomagno).
- Detección: ML Kit Face Detection; si hay varias personas se toma el rostro más cercano (el más grande).
- Solo se guarda un vector de 192 números por cliente (`rostros_clientes`), nunca fotos.
- Movimientos en `registros_acceso` (`es_entrada`: TRUE = entrada, FALSE = salida) y estado en `clientes.dentro`.
- Umbral de similitud: `_umbral_rostro()` en el SQL (0.70).

## Inversión en equipo

| Equipo                    | Cantidad | Costo (MXN) |
|---------------------------|:--------:|------------:|
| Laptop ASUS ExpertBook    | 6        | 66,000      |
| OPPO Reno 14 Pro          | 1        | 9,000       |
| POCO M7 Pro               | 1        | 4,000       |
| Redmi 13 Pro 5G           | 1        | 8,000       |
| Samsung Galaxy A55 5G     | 1        | 8,000       |
| Samsung Galaxy A56 5G     | 1        | 8,000       |
| Samsung Galaxy A54 5G     | 1        | 10,000      |
| **Total inversión en equipo** |      | **113,000** |

## Mano de obra

| Integrante | Rol                             | Horas | Tarifa/hora (MXN) | Subtotal (MXN) |
|------------|---------------------------------|:-----:|:-----------------:|---------------:|
| Fernando   |  Desarrollo Android             | 120   | 250               | 30,000         |
| Axel       |  Desarrollo Android             | 120   | 250               | 30,000         |
| Anabeli    |  Base de datos / Supabase       | 100   | 250               | 25,000         |
| Yureni     |  Interfaz y diseño              | 90    | 250               | 22,500         |
| Isabela    |  Interfaz y diseño              | 90    | 250               | 22,500         |
| Sahid      |  Traducción y manual            | 60    | 250               | 15,000         |
|            | **Total mano de obra**          | **580** |                 | **145,000**    |


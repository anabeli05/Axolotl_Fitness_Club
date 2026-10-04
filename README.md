# GymControlBase

Proyecto base para Android Studio creado en Kotlin + Jetpack Compose.

## Qué incluye

### Cliente
- Login por número de membresía y contraseña.
- Inicio con QR de acceso, horario y ocupación por hora.
- Asesorías con instructores, costo y botón Solicitar/Pendiente.
- Perfil editable, membresía, estado y cambio de contraseña.

### Recepción
- Login por correo y contraseña.
- Vista principal de escaneo QR.
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
5. Implementar QR/cámara.
6. Conectar CRUD de com.example.gymcontrol.ui.encargado.clientes, usuarios, servicios y gastos.
7. Implementar solicitudes y reportes.

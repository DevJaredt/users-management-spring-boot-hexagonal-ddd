# Colección Bruno — Flujo completo

Colección [Bruno](https://www.usebruno.com/) para probar de punta a punta el sistema de gestión de
usuarios (users → Kafka → accounts → notifiers → Brevo).

## Cómo usarla

1. Abre Bruno → **Open Collection** → selecciona esta carpeta (`bruno/`).
2. Arriba a la derecha, selecciona el entorno **Render** (archivo `environments/Render.bru`).
3. Ejecuta las peticiones **en orden** (o usa *Run* en la colección). La primera es **Login**, que
   guarda el `token` para las demás.

> Requiere el admin semilla: `admin@example.com` / `Admin1234!` (rol `ADMIN`, estado `ACTIVE`).
> Sin un ADMIN activo no se puede hacer login ni actualizar/eliminar usuarios.

## Orden del flujo

| # | Carpeta | Petición | Método | Auth | Qué prueba |
|---|---|---|---|---|---|
| 1 | Auth | Login (admin) | POST | — | Obtiene el JWT (guarda `{{token}}`) |
| 2 | Users | Crear usuario | POST | — | Crea usuario (guarda `{{userId}}`/`{{userEmail}}`) → dispara `create-user` + `create-account` |
| 3 | Users | Obtener usuario | GET | Bearer | Lee el usuario por id |
| 4 | Users | Listar usuarios | GET | Bearer | Lista todos |
| 5 | Users | Actualizar status (ACTIVE) | PUT | Bearer (ADMIN) | Cambia `PENDING → ACTIVE` → dispara `update-user` (correo) |
| 6 | Accounts | Obtener cuenta por userId | GET | — | Verifica la cuenta creada automáticamente |
| 7 | Accounts | Crear cuenta (idempotente) | POST | — | Crea/recupera la cuenta (idempotente por `userId`) |
| 8 | Notifications | Correo de prueba (SMTP Brevo) | POST | — | Envía un correo directo por Brevo (sin Kafka) |
| 9 | Users | Eliminar usuario | DELETE | Bearer (ADMIN) | Borra el usuario de prueba (204) |

## Variables (entorno `Render`)

| Variable | Valor |
|---|---|
| `usersBaseUrl` | URL del servicio users (Render) |
| `accountsBaseUrl` | URL del servicio accounts (Render) |
| `notifiersBaseUrl` | URL del servicio notifiers (Render) |
| `adminEmail` / `adminPassword` | credenciales del admin semilla |
| `testRecipient` | destinatario del correo de prueba |

`token`, `userId`, `userEmail`, `newUserId`, `newUserEmail` se generan solos por scripts durante el flujo.

## Notas

- **Autenticación**: `GET/PUT/DELETE /api/users` requieren JWT (`Authorization: Bearer`); `PUT/DELETE`
  además rol `ADMIN`. `POST /api/users` y todo `accounts`/`notifications` son públicos.
- Los usuarios nuevos nacen **PENDING**: el login solo funciona con cuentas **ACTIVE** y para
  activarlas hace falta un **ADMIN activo** (por eso el admin semilla).
- En el plan **free** de Render los servicios se duermen a los 15 min; la primera petición puede
  tardar ~1 min.
- El correo de prueba va al `testRecipient`; revisa también **spam** (el dominio puede no tener
  DKIM/DMARC).

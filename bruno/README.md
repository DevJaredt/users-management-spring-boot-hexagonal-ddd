# Colección Bruno — Flujo completo

Colección [Bruno](https://www.usebruno.com/) para probar de punta a punta el sistema:
`users → Kafka → accounts / notifiers / search`, más **archivos en Backblaze B2**, **caché Valkey**
y **búsquedas en OpenSearch**.

## Cómo usarla

1. Abre Bruno → **Open Collection** → selecciona esta carpeta (`bruno/`).
2. Arriba a la derecha, elige el entorno:
   - **Local** (por defecto): `http://localhost:8080/8081/8082/8083`.
   - **Render**: URLs públicas (ajústalas en `environments/Render.bru`).
3. Ejecuta las peticiones **en orden** (o usa *Run*). La primera es **Login**, que guarda el
   `token` para las demás.

> Requiere el admin semilla: `admin@example.com` / `Admin1234!` (rol `ADMIN`, estado `ACTIVE`).
> Sin un ADMIN activo no se puede hacer login ni actualizar/eliminar usuarios ni subir archivos.

## Entornos

| Variable | Local | Render |
|---|---|---|
| `usersBaseUrl` | `http://localhost:8080` | URL del servicio users |
| `accountsBaseUrl` | `http://localhost:8082` | URL del servicio accounts |
| `notifiersBaseUrl` | `http://localhost:8081` | URL del servicio notifiers |
| `searchBaseUrl` | `http://localhost:8083` | URL del servicio search |
| `adminEmail` / `adminPassword` | admin semilla | admin semilla |
| `testRecipient` | correo de prueba | correo de prueba |
| `searchQuery` | texto a buscar en OpenSearch | texto a buscar en OpenSearch |
| `fileName` | `certificado-prueba.pdf` | archivo a descargar |

`token`, `userId`, `userEmail`, `newUserId` y `newUserEmail` se generan solos por scripts.

## Orden del flujo

| # | Carpeta | Petición | Método | Auth | Qué prueba |
|---|---|---|---|---|---|
| 1 | Auth | Login (admin) | POST | — | Obtiene el JWT (guarda `{{token}}`) |
| 2 | Users | Crear usuario | POST | — | Crea usuario (guarda `{{userId}}`/`{{userEmail}}`) → `create-user` + `create-account` |
| 3 | Users | Obtener usuario | GET | Bearer | Lee el usuario por id (primera vez llena la caché) |
| 4 | Users | Listar usuarios | GET | Bearer | Lista todos |
| 5 | Users | Actualizar status (ACTIVE) | PUT | Bearer (ADMIN) | `PENDING → ACTIVE` → `update-user` (correo) |
| 6 | Accounts | Obtener cuenta por userId | GET | — | Verifica la cuenta creada automáticamente |
| 7 | Accounts | Crear cuenta (idempotente) | POST | — | Crea/recupera la cuenta |
| 8 | Files | Subir archivo del usuario | POST | Bearer (ADMIN) | Sube el PDF de ejemplo a B2 bajo `users/{userId}/` |
| 9 | Files | Listar archivos del usuario | GET | Bearer | Metadatos de los archivos en B2 |
| 10 | Files | Descargar archivo del usuario | GET | Bearer | Descarga `{{fileName}}` desde el bucket privado |
| 11 | Files | Eliminar archivo del usuario | DELETE | Bearer (ADMIN) | Borra el objeto de B2 |
| 12 | Search | Buscar usuarios | GET | — | Consulta OpenSearch (CQRS); requiere `OPENSEARCH_ENABLED=true` |
| 13 | Search | Obtener usuario indexado | GET | — | Lee el documento del índice por `{{userId}}` |
| 14 | Notifications | Correo de prueba (SMTP Brevo) | POST | — | Envía un correo directo por Brevo (sin Kafka) |
| 15 | Cleanup | Eliminar usuario | DELETE | Bearer (ADMIN) | Borra el usuario y publica `delete-user` |

> Con este orden, el botón **Run** de Bruno puede ejecutar la colección completa: el usuario se
> elimina al final (carpeta **Cleanup**).

## Archivos (B2)

- La petición **Subir archivo** usa `bruno/Files/certificado-prueba.pdf` como archivo de ejemplo.
- Si cambias de archivo, hazlo desde la pestaña **Body → Multipart Form** de Bruno.
- La ruta `@file(./certificado-prueba.pdf)` es relativa al archivo `.bru`.
- El servicio acepta imágenes (JPEG/PNG/WebP/GIF) o PDF, máximo 5 MB.
- Para probar con el admin semilla, define `userId` manualmente en el entorno:
  `00000000-0000-0000-0000-000000000001`.

## Búsqueda (OpenSearch)

- `create-user` / `update-user` indexan el documento; `delete-user` lo elimina.
- Hay **consistencia eventual**: espera 1–2 segundos después de crear/actualizar antes de buscar.
- El admin semilla no pasa por Kafka, así que no aparece indexado hasta que lo actualices o crees
  otro usuario.
- Con `q=` vacío se listan todos los documentos indexados.

## Notas

- **Autenticación**: `GET/PUT/DELETE /api/users` requieren JWT (`Authorization: Bearer`);
  `PUT/DELETE` y la subida de archivos requieren rol `ADMIN`. `POST /api/users`,
  `accounts`, `notifications` y `search` son públicos.
- Los usuarios nuevos nacen **PENDING**: el login solo funciona con cuentas **ACTIVE**.
- En Render free los servicios se duermen a los 15 min; la primera petición puede tardar ~1 min.
- El correo de prueba va al `testRecipient`; revisa también **spam**.
- Si Brevo responde `525 Unauthorized IP address`, autoriza tu IP en la SMTP key o quita la restricción.

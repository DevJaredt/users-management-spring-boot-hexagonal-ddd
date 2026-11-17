# Integración de servicios cloud: Backblaze B2, Valkey y OpenSearch

Guía de implementación del taller sobre la arquitectura de microservicios de este repositorio.
Cubre los dos servicios **obligatorios** (B2 y Valkey) y el **opcional para nota alta**
(OpenSearch con CQRS), integrados con arquitectura hexagonal y DDD.

> Versión PDF con el checklist de aprovisionamiento: [`servicios-cloud-requeridos.pdf`](servicios-cloud-requeridos.pdf).

## 1. Qué se implementó

| Guía | Servicio cloud | Dónde vive en el código | Estado |
|---|---|---|---|
| Guía 9 | **Backblaze B2** (bucket privado compatible con S3) | `users` | Obligatorio |
| Guía 10 | **Valkey/Redis** en Aiven (caché distribuida con TTL) | `users` | Obligatorio |
| Guía 11 | **OpenSearch** en Aiven (CQRS + eventos) | `search` | Opcional / nota alta |

> **RabbitMQ vs Kafka.** La Guía 11 propone RabbitMQ para sincronizar el índice. Este proyecto ya
> usa **Kafka** para los eventos de usuario, así que se reutiliza el mismo broker: `users` publica y
> `search` consume. El patrón CQRS es idéntico; solo cambia el transporte.

### Arquitectura resultante

```
                       ┌──────────────────────── AIVEN ────────────────────────┐
 POST /api/users ─────▶│ users ──(1) MySQL/TiDB                                │
 PUT/DELETE            │   │                                                   │
                       │   ├──(2) Kafka: create-user / update-user / delete-user
                       │   │                                                   │
                       │   ├──(3) Backblaze B2: users/{userId}/archivo         │
                       │   └──(4) Valkey: users:by-id:{userId}  (TTL 300 s)    │
                       │                    │                                  │
                       │                    ▼                                  │
                       │              search (CQRS) ──▶ OpenSearch índice users│
                       └───────────────────────────────────────────────────────┘
 GET /api/search/users ───────────────▶ search ──▶ OpenSearch (no toca MySQL)
```

### Mapa guía → capas hexagonales

| Preocupación | Puerto (aplicación) | Adaptador (infraestructura) |
|---|---|---|
| Archivos de usuario | `UserFileStoragePort` | `B2FileStorageAdapter` (AWS SDK v2 S3) |
| Caché de usuarios | `UserCachePort` | `ValkeyUserCacheAdapter` (Jedis) |
| Índice de búsqueda | `UserSearchIndexPort` | `OpenSearchUserIndexAdapter` (API REST) |
| Eventos de usuario | `UserEventPublisherPort` (users) | `UserEventKafkaPublisher` |
| Consumo de eventos | `SyncUserIndexUseCase` (search) | `UserEventIndexListener` |

Los adaptadores se activan con `B2_ENABLED`, `CACHE_ENABLED` y `OPENSEARCH_ENABLED`. Si están en
`false`, se registra una implementación no-op y el proyecto arranca sin credenciales cloud.

---

## 2. Guía 9 — Backblaze B2 (obligatorio)

### 2.1 En la nube (lo que debes tener)

1. Cuenta gratuita en <https://www.backblaze.com> → **B2 Cloud Storage**.
2. Bucket **privado** (por ejemplo `usuarios-lab-2026`), cifrado `SSE-B2`, Object Lock deshabilitado.
3. Ciclo de vida: **Guardar solo la última versión del archivo** (coincide con la semántica de
   sobreescritura por nombre que usa la aplicación).
4. **Application Keys** → `Add a New Application Key`:
   - Acceso restringido al bucket creado.
   - Type of Access: **Read and Write**.
   - `List All Bucket Names`: deshabilitado.
   - Prefijo: vacío.
   - Duración: vacía (laboratorio).
5. Guarda `keyID` y `applicationKey`; la clave secreta se muestra **una sola vez**.

### 2.2 En el código

- `UserFileStoragePort` define `store`, `load`, `list` y `delete` sin conocer el proveedor.
- `B2FileStorageAdapter` usa AWS SDK v2 (`S3Client`) con `endpointOverride` y `pathStyleAccess`.
- Cada archivo se guarda como `users/{userId}/{nombreNormalizado}` (carpeta por usuario).
- `UserFileName` normaliza el nombre y elimina rutas para impedir *path traversal*.
- Solo se aceptan **imágenes** (`jpeg`, `png`, `webp`, `gif`) y **PDF**; máximo 5 MB por archivo
  (`B2_MAX_FILE_SIZE_BYTES`).
- El bucket es privado: la descarga siempre pasa por el microservicio, que valida que el usuario
  exista y exige JWT.

### 2.3 Variables de entorno

```dotenv
B2_ENABLED=true
B2_ENDPOINT=https://s3.us-east-005.backblazeb2.com
B2_REGION=us-east-005
B2_BUCKET=usuarios-lab-2026
B2_KEY_ID=...
B2_APPLICATION_KEY=...
B2_MAX_FILE_SIZE_BYTES=5242880
```

### 2.4 Endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `POST` | `/api/users/{userId}/files` | ADMIN | Sube un archivo (`multipart/form-data`, campo `file`) |
| `GET` | `/api/users/{userId}/files` | ADMIN, REVIEWER | Lista los metadatos de los archivos |
| `GET` | `/api/users/{userId}/files/{fileName}` | ADMIN, REVIEWER | Descarga el archivo |
| `DELETE` | `/api/users/{userId}/files/{fileName}` | ADMIN | Elimina el archivo |

### 2.5 Prueba manual

```bash
# 1) Obtener JWT (usuario semilla admin@example.com / Admin1234!)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin1234!"}' | jq -r .accessToken)

# 2) Subir un PDF
curl -X POST "http://localhost:8080/api/users/00000000-0000-0000-0000-000000000001/files" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@certificado.pdf"

# 3) Listar
curl "http://localhost:8080/api/users/00000000-0000-0000-0000-000000000001/files" \
  -H "Authorization: Bearer $TOKEN"

# 4) Descargar
curl -OJ "http://localhost:8080/api/users/00000000-0000-0000-0000-000000000001/files/certificado.pdf" \
  -H "Authorization: Bearer $TOKEN"

# 5) Eliminar
curl -X DELETE "http://localhost:8080/api/users/00000000-0000-0000-0000-000000000001/files/certificado.pdf" \
  -H "Authorization: Bearer $TOKEN"
```

En PowerShell usa `curl.exe` (el alias `curl` apunta a `Invoke-WebRequest`).

**Qué mostrar en el video:** el objeto con candado en *Browse Files* de B2, la lista devuelta por la
API y la descarga del mismo PDF.

> Si subes un archivo con el **mismo nombre**, B2 reemplaza el objeto y el ciclo de vida conserva
> solo la última versión. Es una forma sencilla de demostrar la política configurada.

---

## 3. Guía 10 — Valkey/Redis en Aiven (obligatorio)

### 3.1 En la nube

1. Aiven → **Create service** → **Valkey** → plan **Free**.
2. Región y nombre (por ejemplo `cache-usuarios-2026`).
3. Esperar estado **Running** y copiar la **Service URI** (`rediss://default:...@host:puerto`).
4. `Service settings` → `Set data eviction policy` → **allkeys-lru**.
5. Verificar con **Redis Insight**:
   - `Add database` → pegar la URI `rediss://...`.
   - `Add key` → tipo `String`, TTL `300`, nombre `users:by-id:demo`, valor `ACTIVO`.
   - Comprobar que la clave desaparece al agotarse el TTL.

### 3.2 En el código

- `GetUserByIdService` hace **read-through**: consulta `UserCachePort`; si no hay entrada, lee de
  MySQL y guarda el usuario en caché.
- `UpdateUserService` refresca la entrada; `DeleteUserService` la elimina.
- `ValkeyUserCacheAdapter` usa `SETEX` con `CACHE_TTL_SECONDS` y serializa un `CachedUser`
  (incluye el hash BCrypt, nunca la contraseña en texto plano).
- La caché es **fail-open**: cualquier fallo de Valkey se registra y la petición sigue contra MySQL.

### 3.3 Variables de entorno

```dotenv
CACHE_ENABLED=true
CACHE_URI=rediss://default:CONTRASENA@host-aiven:15426
CACHE_TTL_SECONDS=300
CACHE_KEY_PREFIX=users:by-id:
```

### 3.4 Cómo demostrar que funciona

1. `GET /api/users/{id}` dos veces.
2. En los logs de `users`:
   - primera llamada: consulta a MySQL y `usuario cacheado ... ttl=300s`;
   - segunda llamada: `cache hit`.
3. En Redis Insight aparece la clave `users:by-id:{id}` con el TTL restante.
4. `PUT /api/users/{id}` refresca el valor; `DELETE` elimina la clave.
5. Si esperas 5 minutos sin consultar, la clave expira y la siguiente lectura vuelve a MySQL.

> Aiven cierra conexiones inactivas a los 300 s (`valkey_timeout`). Jedis crea conexiones nuevas del
> pool cuando es necesario; por eso el adaptador no mantiene una conexión única.

---

## 4. Guía 11 — OpenSearch en Aiven (opcional para nota alta)

### 4.1 En la nube

1. Aiven → **Create service** → **OpenSearch** → plan **Free**.
2. Esperar **Running**.
3. `Overview` → pestaña **OpenSearch** → copiar `Service URI`, usuario (`avnadmin`) y contraseña.
4. Entrar a **OpenSearch Dashboards** → tenant `Private` → `Dev tools`.
5. Probar `GET _search { "query": { "match_all": {} } }`.

### 4.2 CQRS con Kafka

| Lado | Comportamiento |
|---|---|
| Escritura | `POST/PUT/DELETE /api/users` persiste en MySQL y publica en Kafka |
| Proyección | `search` consume `create-user`, `update-user` y `delete-user` e indexa/elimina en OpenSearch |
| Lectura | `GET /api/search/users` consulta **solo** OpenSearch |

El índice `users` se crea al arrancar `search` con mappings explícitos (`name` y `email` como texto
con subcampo `keyword`, `role`/`status` como keyword, 1 shard y 0 réplicas para el plan gratuito).

### 4.3 Variables de entorno

```dotenv
OPENSEARCH_ENABLED=true
OPENSEARCH_URL=https://host-aiven:15425
OPENSEARCH_USERNAME=avnadmin
OPENSEARCH_PASSWORD=...
OPENSEARCH_INDEX=users
```

### 4.4 Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/search/users?q=john&page=0&size=20` | Búsqueda de texto libre (nombre, correo, rol, estado) |
| `GET` | `/api/search/users?q=` | Lista todos los indexados |
| `GET` | `/api/search/users/{id}` | Documento indexado por id |

### 4.5 Prueba manual

```bash
# Crear un usuario (users escribe en MySQL y publica create-user)
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"id":"u-busqueda-1","name":"John Arrieta","email":"john.search@example.com","password":"Pass1234","role":"MEMBER"}'

# Esperar 1-2 s (consistencia eventual) y buscar en OpenSearch
curl "http://localhost:8083/api/search/users?q=arrieta"

# Actualizar y volver a buscar
curl -X PUT http://localhost:8080/api/users/u-busqueda-1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"John Arrieta Actualizado","email":"john.search@example.com","password":null,"role":"MEMBER","status":"ACTIVE"}'

# Eliminar: delete-user retira el documento del índice
curl -X DELETE http://localhost:8080/api/users/u-busqueda-1 -H "Authorization: Bearer $TOKEN"
```

**Qué mostrar en el video:** el índice en `Dev tools` (`GET users/_search`), el resultado de
`/api/search/users?q=...` y cómo un `PUT`/`DELETE` se refleja en el índice en segundos.

---

## 5. Orden sugerido para el video de entrega

1. **Arquitectura (30 s):** mostrar `users`, `search`, Kafka, B2, Valkey y OpenSearch en el README.
2. **B2 (2 min):** bucket privado + Application Key; subir PDF desde Swagger/curl; ver objeto en
   *Browse Files*; listar y descargar.
3. **Valkey (2 min):** `GET /api/users/{id}` dos veces mostrando `cache hit` en logs; clave y TTL en
   Redis Insight; actualizar usuario y ver el refresco.
4. **OpenSearch (3 min):** crear usuario; ver `create-user` en Kafka; buscar en `/api/search/users`;
   actualizar y eliminar mostrando el índice.
5. **Cierre (30 s):** explicar hexagonal (puertos/adaptadores), CQRS y degradación fail-open.

## 6. Diagnóstico rápido

| Síntoma | Causa probable | Solución |
|---|---|---|
| `503 File storage is not configured` | `B2_ENABLED=false` o faltan credenciales | Revisar `B2_*` |
| `502 Error al comunicarse con el almacenamiento` | Endpoint/región o Application Key incorrectos | Verificar endpoint y permisos del bucket |
| `502 Error al comunicarse con el índice` | URL/credenciales de OpenSearch | Revisar `OPENSEARCH_*` |
| `503 Search is not available` | `OPENSEARCH_ENABLED=false` | Activar y reiniciar `search` |
| El usuario no aparece en la búsqueda | Consistencia eventual o `search` sin conexión | Esperar 1-2 s; revisar logs del consumidor |
| `cache hit` nunca aparece | `CACHE_ENABLED=false` o URI inválida | Revisar logs `ValkeyUserCacheAdapter` |

## 7. Seguridad y costos

- **Nunca** subas `.env`, `B2_APPLICATION_KEY`, `CACHE_URI` con contraseña ni `OPENSEARCH_PASSWORD`
  al repositorio.
- Las credenciales viven en variables de entorno (local) y en los secretos de Render (`sync: false`).
- Los buckets y los servicios de Aiven usan TLS; el bucket es privado.
- Los logs solo contienen identificadores técnicos (`userId`, `eventId`), nunca datos personales.
- Los planes gratuitos de B2 (10 GB), Valkey (Free-1) y OpenSearch (Free) alcanzan para el taller;
  OpenSearch puede apagarse por inactividad y se reactiva desde la consola de Aiven.

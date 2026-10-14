# Users Management — Microservicios con Spring Boot, Hexagonal y DDD

Sistema de gestión de usuarios desplegable en **Render**, compuesto por **tres microservicios**
que se comunican de forma síncrona (HTTP) y asíncrona (Kafka), con la API REST como punto de
entrada activo.

Spring es el único *composition root* de cada servicio: cada uno arranca su propio contexto y sus
dependencias se resuelven con configuración y component scanning.

## Arquitectura

```
                        ┌───────────────────── RENDER ─────────────────────┐
                        │                                                  │
   paso 1 (sync) ───────┼──▶  users  ────────── paso 5 (sync) ──▶ accounts  │── paso 6 (sync) ──▶ TiDB
                        │      │  ▲                             accounts ◀──┼─── accounts
   paso 2 (async) ──────┼──────┘  │                                          │
                        │         └── paso 2 ──▶  Kafka  ── paso 3 ──▶ notifiers ── paso 4 (sync) ──▶ Brevo
                        │                            ▲                        │
                        │                            └────── paso 7 (async) ───┘
                        └──────────────────────────────────────────────────┘
```

| Servicio | Responsabilidad | Pasos del diagrama |
|---|---|---|
| **users** | CRUD de usuarios, JWT, publica eventos y crea la cuenta asociada | 1, 2, 5 |
| **notifiers** | Consume eventos y envía el correo con la API REST de Brevo | 3, 4, 7 |
| **accounts** | Persiste la cuenta asociada a cada usuario | 6 |

Infraestructura: **Kafka** (Aiven en producción) y **MySQL/TiDB** compartida por `users` y
`accounts` (tablas `users` y `accounts`).

## Estructura del repositorio

```
.
├── Dockerfile              # imagen del servicio users
├── compose.yaml            # pila local: Kafka + MySQL + los 3 servicios
├── render.yaml             # Blueprint con los 3 web services
├── src/                    # servicio users (raíz del repo)
├── notifiers/              # servicio notifiers (proyecto Maven independiente)
└── accounts/               # servicio accounts (proyecto Maven independiente)
```

Cada servicio es un proyecto Maven independiente: se compila y se despliega por separado.

## Verificación (tests)

```bash
./mvnw clean test                      # servicio users
./mvnw -f notifiers/pom.xml clean test # servicio notifiers
./mvnw -f accounts/pom.xml clean test  # servicio accounts
```

En Windows se puede utilizar `mvnw.cmd` o `mvnw.ps1`.

## Ejecución local

El archivo `compose.yaml` levanta **Kafka (KRaft), MySQL y los tres servicios**, con el esquema y
las tablas `users` y `accounts` cargados automáticamente.

```bash
docker compose up --build
```

| Servicio | URL |
|---|---|
| users (Swagger UI) | http://localhost:8080/swagger-ui/index.html |
| accounts | http://localhost:8082/api/accounts |
| notifiers (health) | http://localhost:8081/actuator/health |
| Kafka (desde tu equipo) | `localhost:29092` |

Para probar el envío real de correo, exporta antes las credenciales de Brevo:

```bash
BREVO_API_KEY=xkeysib-... BREVO_FROM_ADDRESS=remitente@verificado.com docker compose up --build
```

Sin credenciales la pila igualmente arranca: el envío falla contra Brevo y queda registrado como
`FAILED` en el tópico `notification-status` (el flujo completo se puede observar en los logs).

## Despliegue en Render

`render.yaml` define los **tres web services** con build Docker y despliegue automático de cada
commit que llegue a la rama `main`.

1. Crear un **Blueprint** en Render y seleccionar este repositorio.
2. Completar en el panel los secretos marcados como requeridos:
   `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`,
   `KAFKA_SASL_JAAS_CONFIG`, `KAFKA_TRUSTSTORE_LOCATION`, `ACCOUNTS_BASE_URL` (URL pública del
   servicio accounts), `BREVO_API_KEY` y `BREVO_FROM_ADDRESS`.
3. Usar una instancia **MySQL/TiDB** accesible desde Internet o desde la red privada de Render y
   ejecutar `src/main/resources/schema.sql` **una vez** para crear las tablas `users` y `accounts`.
4. Crear los tópicos `user-events` y `notification-status` en el broker (o dejar
   `KAFKA_TOPICS_AUTO_CREATE=true`).
5. Desplegar el Blueprint. El servicio `users` expone Swagger UI en `/swagger-ui.html`.

Las credenciales nunca deben guardarse en `application.properties` ni en `render.yaml`. Para
desarrollo local se proporcionan como variables de entorno.

## Variables de entorno principales

| Variable | Servicio | Para qué |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_SSL_MODE` | users, accounts | conexión a MySQL/TiDB |
| `KAFKA_BOOTSTRAP_SERVERS` | users, notifiers | broker de Kafka |
| `KAFKA_SECURITY_PROTOCOL`, `KAFKA_SASL_MECHANISM`, `KAFKA_SASL_JAAS_CONFIG`, `KAFKA_TRUSTSTORE_LOCATION` | users, notifiers | autenticación SASL_SSL (Aiven) |
| `KAFKA_TOPIC_USER_EVENTS`, `KAFKA_TOPIC_NOTIFICATION_STATUS` | users, notifiers | nombres de los tópicos |
| `ACCOUNTS_BASE_URL` | users | URL del servicio accounts |
| `BREVO_API_KEY`, `BREVO_FROM_ADDRESS`, `BREVO_FROM_NAME` | notifiers | API transaccional de Brevo |
| `BREVO_RETRY_*`, `BREVO_CIRCUIT_*` | notifiers | parámetros de resiliencia (opcionales) |
| `JWT_SECRET`, `JWT_EXPIRATION_SECONDS` | users | firma y vigencia del token |

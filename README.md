# Users Management — Microservicios con Spring Boot, Hexagonal y DDD

Sistema de gestión de usuarios desplegable en **Render**, compuesto por **tres microservicios**
que se comunican de forma síncrona (HTTP) y asíncrona (Kafka), con la API REST como punto de
entrada activo.

Spring es el único *composition root* de cada servicio: cada uno arranca su propio contexto y sus
dependencias se resuelven con configuración y component scanning.

## Arquitectura

```
                     ┌─────────────────────── RENDER ───────────────────────┐
                     │                                                      │
   paso 1 (sync) ────┼──▶  users  ── paso 5 (sync) ──▶  accounts  ── paso 6 ─┼─▶ TiDB
                     │      │  ▲                          │                  │  (MySQL)
   paso 2 (async) ───┼──────┘  │ publica                  └── paso 7 (async) │
                     │         │ create-user/update-user      publica        │
                     │         │                              create-account │
                     │         │                                  │          │
                     │         └──────────▶  AIVEN Kafka  ◀───────┘          │
                     │                          │  ▲                          │
                     │            paso 3 ───────┘  └── notification-status    │
                     │                ▼                     (async)          │
                     │            notifiers ── paso 4 (sync) ──▶ SMTP/Brevo   │
                     └──────────────────────────────────────────────────────┘
```

| Servicio | Responsabilidad | Pasos del diagrama |
|---|---|---|
| **users** | CRUD de usuarios, JWT; publica `create-user`/`update-user` (productor 1) y crea/sincroniza la cuenta (REST) | 1, 2, 5 |
| **notifiers** | Consume `update-user` y `create-account` y envía el correo por el relay SMTP de Brevo; publica el estado | 3, 4 |
| **accounts** | Persiste la cuenta asociada y publica `create-account` (productor 2) | 6, 7 |

Infraestructura: **Kafka** (Aiven en producción, con **SSL/mTLS** por certificado de cliente) y
**MySQL/TiDB** compartida por `users` y `accounts` (tablas `users` y `accounts`).

Tópicos: `create-user` y `update-user` (los publica **users**), `create-account` (lo publica
**accounts**, paso 7) y `notification-status` (lo publica **notifiers**). Hay **dos productores**
hacia Kafka: `users` (Paso 2) y `accounts` (Paso 7). **notifiers** consume `update-user` y
`create-account`; el **alta** se notifica una sola vez desde `create-account` (`create-user` queda
como evento informativo, sin correo).

## Estructura del repositorio

Cada microservicio es un proyecto Maven **independiente y autocontenido** (su propio `pom.xml`,
`src/`, `Dockerfile` y Maven Wrapper). La raíz solo contiene la documentación y la orquestación.

```
.
├── compose.yaml            # pila local: Kafka + MySQL + los 3 servicios
├── render.yaml             # Blueprint con los 3 web services
├── users/                  # servicio users      (pasos 1, 2, 5)
├── notifiers/              # servicio notifiers  (pasos 3, 4, 7)
├── accounts/               # servicio accounts   (paso 6)
└── docs/                   # documentación (esquema de base de datos, etc.)
```

Cada servicio se compila, se prueba y se despliega por separado.

## Documentación

- [Esquema de la base de datos](docs/esquema-base-datos.md) — tablas `users` y `accounts`, DDL y datos semilla.
- [Tópicos de Kafka y contrato de los eventos](docs/kafka-topicos-y-eventos.md) — productores, consumidores y JSON de cada evento.

## Verificación (tests)

```bash
cd users     && ./mvnw clean test   # servicio users
cd notifiers && ./mvnw clean test   # servicio notifiers
cd accounts  && ./mvnw clean test   # servicio accounts
```

En Windows se puede utilizar `mvnw.cmd` o `mvnw.ps1` dentro de cada carpeta.

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

Para probar el envío real de correo, exporta antes las credenciales SMTP de Brevo
(Brevo → SMTP & API → SMTP):

```bash
BREVO_SMTP_USERNAME=be4003001@smtp-brevo.com BREVO_SMTP_PASSWORD=xsmtpsib-... \
BREVO_FROM_ADDRESS=remitente@verificado.com docker compose up --build
```

El `BREVO_SMTP_USERNAME` es el **login SMTP** que muestra Brevo (formato
`beXXXXXXX@smtp-brevo.com`), no tu correo personal. La contraseña es la **SMTP key** que genera
Brevo en "Open SMTP key settings".

Sin credenciales la pila igualmente arranca: el envío falla contra Brevo y queda registrado como
`FAILED` en el tópico `notification-status` (el flujo completo se puede observar en los logs).

## Conexión a Aiven (SSL / mTLS)

En local la pila usa un Kafka en **PLAINTEXT**. Para apuntar a **Aiven** —que en este proyecto
autentica con **certificado de cliente**, no SASL— exporta los 3 PEM y activa SSL:

```bash
export KAFKA_BOOTSTRAP_SERVERS=kafka-<id>.aivencloud.com:<puerto>
export KAFKA_SECURITY_PROTOCOL=SSL
export KAFKA_SSL_KEY="$(cat service.key)"    # Access key   (clave privada PEM)
export KAFKA_SSL_CERT="$(cat service.crt)"   # Access cert  (certificado de acceso, PEM)
export KAFKA_SSL_CA="$(cat ca.crt)"          # CA cert      (certificado de la CA, PEM)
```

En PowerShell:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS = "kafka-<id>.aivencloud.com:<puerto>"
$env:KAFKA_SECURITY_PROTOCOL = "SSL"
$env:KAFKA_SSL_KEY  = Get-Content -Raw service.key
$env:KAFKA_SSL_CERT = Get-Content -Raw service.crt
$env:KAFKA_SSL_CA   = Get-Content -Raw ca.crt
```

Los tres servicios (`users`, `notifiers`, `accounts`) usan las mismas variables. Los eventos viajan
como **JSON** y los certificados **nunca** deben subirse al repositorio.

## Despliegue en Render

`render.yaml` define los **tres web services** con build Docker y despliegue automático de cada
commit que llegue a la rama `main`.

1. Crear un **Blueprint** en Render y seleccionar este repositorio.
2. Completar en el panel los secretos marcados como requeridos:
   `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`,
   `KAFKA_SSL_KEY`, `KAFKA_SSL_CERT`, `KAFKA_SSL_CA` (los 3 PEM del certificado de cliente),
   `ACCOUNTS_BASE_URL` (URL pública del servicio accounts), `BREVO_SMTP_USERNAME`,
   `BREVO_SMTP_PASSWORD` y `BREVO_FROM_ADDRESS`.
3. Usar una instancia **MySQL/TiDB** accesible desde Internet o desde la red privada de Render y
   ejecutar `users/src/main/resources/schema.sql` **una vez** para crear las tablas `users` y `accounts`.
4. Crear los tópicos `create-user`, `update-user`, `create-account` y `notification-status` en el
   broker (o dejar `KAFKA_TOPICS_AUTO_CREATE=true`).
5. Desplegar el Blueprint. El servicio `users` expone Swagger UI en `/swagger-ui.html`.

> **Puerto SMTP en Render.** El plan **free** de Render bloquea el tráfico saliente por los puertos
> `25`, `465` y `587`, así que el servicio `notifiers` sale por el puerto **`2525`** de Brevo
> (definido en `render.yaml`). Si subes `notifiers-service` a un plan pago, puedes usar `587`
> (STARTTLS) cambiando `BREVO_SMTP_PORT`.

Las credenciales nunca deben guardarse en `application.properties` ni en `render.yaml`. Para
desarrollo local se proporcionan como variables de entorno.

## Variables de entorno principales

| Variable | Servicio | Para qué |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_SSL_MODE` | users, accounts | conexión a MySQL/TiDB |
| `KAFKA_BOOTSTRAP_SERVERS` | users, notifiers, accounts | broker de Kafka |
| `KAFKA_SECURITY_PROTOCOL=SSL`, `KAFKA_SSL_KEY`, `KAFKA_SSL_CERT`, `KAFKA_SSL_CA` | users, notifiers, accounts | certificado de cliente (mTLS) de Aiven: los 3 PEM |
| `KAFKA_TOPIC_CREATE_USER`, `KAFKA_TOPIC_UPDATE_USER` | users, notifiers | tópicos de usuarios |
| `KAFKA_TOPIC_CREATE_ACCOUNT` | accounts, notifiers | tópico de cuentas |
| `KAFKA_TOPIC_NOTIFICATION_STATUS` | notifiers | estado del envío (async) |
| `ACCOUNTS_BASE_URL` | users | URL del servicio accounts |
| `BREVO_SMTP_HOST`, `BREVO_SMTP_PORT`, `BREVO_SMTP_AUTH`, `BREVO_SMTP_STARTTLS`, `BREVO_SMTP_SSL` | notifiers | relay SMTP de Brevo |
| `BREVO_SMTP_USERNAME`, `BREVO_SMTP_PASSWORD` | notifiers | login SMTP (`beXXXXXXX@smtp-brevo.com`) + SMTP key |
| `BREVO_FROM_ADDRESS`, `BREVO_FROM_NAME` | notifiers | remitente del correo |
| `BREVO_RETRY_*`, `BREVO_CIRCUIT_*` | notifiers | parámetros de resiliencia (opcionales) |
| `JWT_SECRET`, `JWT_EXPIRATION_SECONDS` | users | firma y vigencia del token |

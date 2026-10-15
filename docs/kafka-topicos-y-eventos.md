# Tópicos de Kafka y contrato de los eventos

Documentación de los tópicos, quién produce/consume cada uno y el **contrato JSON** de cada
evento. Los mensajes viajan como **JSON** (serializados/deserializados con Jackson); la clave del
mensaje (`key`) es siempre el `userId`.

## Resumen

| Tópico | Productor | Consumidor | Paso del diagrama |
|---|---|---|---|
| `create-user` | **users** | **notifiers** | 2 → 3 |
| `update-user` | **users** | **notifiers** | 2 → 3 |
| `create-account` | **accounts** | **notifiers** | 6 → 7 → 3 |
| `notification-status` | **notifiers** | *(auditoría / observabilidad)* | 7 |

- El **borrado de usuario no publica** nada (solo el alta y la actualización).
- **notifiers** consume los tres primeros tópicos y, por cada evento, envía el correo y publica el
  resultado en `notification-status`.
- Los nombres son configurables por variables de entorno: `KAFKA_TOPIC_CREATE_USER`,
  `KAFKA_TOPIC_UPDATE_USER`, `KAFKA_TOPIC_CREATE_ACCOUNT`, `KAFKA_TOPIC_NOTIFICATION_STATUS`.

## `create-user` (alta de usuario)

Lo publica **users** cuando crea un usuario. Lo consume **notifiers** y envía el correo de "cuenta
creada".

```json
{
  "eventId": "9f1c2e34-5a6b-4c7d-8e9f-0a1b2c3d4e5f",
  "eventType": "USER_CREATED",
  "occurredAt": "2026-10-15T14:32:10.123Z",
  "userId": "00000000-0000-0000-0000-000000000001",
  "name": "John Arrieta",
  "email": "john@example.com",
  "role": "ADMIN",
  "status": "PENDING"
}
```

## `update-user` (actualización de usuario)

Lo publica **users** al actualizar un usuario. **notifiers** envía el correo de "cuenta
actualizada" (incluye el estado).

```json
{
  "eventId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
  "eventType": "USER_UPDATED",
  "occurredAt": "2026-10-15T14:35:00.000Z",
  "userId": "00000000-0000-0000-0000-000000000001",
  "name": "John Arrieta",
  "email": "john@example.com",
  "role": "ADMIN",
  "status": "ACTIVE"
}
```

## `create-account` (alta de cuenta)

Lo publica **accounts** cuando persiste la cuenta asociada al usuario (paso 6). **notifiers** envía
el correo de "cuenta de servicio creada".

```json
{
  "eventId": "aaaa1111-bbbb-2222-cccc-3333dddd4444",
  "eventType": "ACCOUNT_CREATED",
  "occurredAt": "2026-10-15T14:32:11.500Z",
  "accountId": "55555555-6666-7777-8888-999999999999",
  "userId": "00000000-0000-0000-0000-000000000001",
  "name": "John Arrieta",
  "email": "john@example.com"
}
```

## `notification-status` (resultado del envío)

Lo publica **notifiers** tras intentar el correo. No incluye el email del destinatario: solo
identificadores técnicos.

```json
{
  "eventId": "9f1c2e34-5a6b-4c7d-8e9f-0a1b2c3d4e5f",
  "userId": "00000000-0000-0000-0000-000000000001",
  "status": "SENT",
  "detail": null,
  "occurredAt": "2026-10-15T14:32:12.000Z"
}
```

`status` puede ser `SENT`, `FAILED` o `SKIPPED`. `detail` solo trae información cuando falla.

## Notas de configuración

- **Serialización:** `JsonSerializer` en los productores y `JsonDeserializer` (envuelto en
  `ErrorHandlingDeserializer`) en el consumidor, con `spring.json.add.type.headers=false` para que
  el contrato sea el JSON plano de arriba (sin cabeceras de tipo).
- **Seguridad:** en Aiven la conexión usa `security.protocol=SSL` con **certificado de cliente**
  (mTLS). Ver el README, sección *Conexión a Aiven (SSL / mTLS)*.
- **Topicos locales:** con `KAFKA_TOPICS_AUTO_CREATE=true` (por defecto en local) los tópicos se
  crean automáticamente; en Aiven conviene crearlos a mano o ajustar los permisos.

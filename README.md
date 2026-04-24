# Bravo Credit — Prueba técnica

Plataforma multipaís para la gestión de solicitudes de crédito. El sistema permite crear solicitudes, aplicar reglas de negocio por país, integrar proveedores bancarios, evaluar riesgo de forma asíncrona, actualizar estados manualmente y reflejar cambios en tiempo real en el frontend.

---

## Tabla de contenidos

1. [Prerrequisitos](#prerrequisitos)
2. [Inicio rápido](#inicio-rápido)
3. [Comandos disponibles](#comandos-disponibles)
4. [Modelo de datos](#modelo-de-datos)
5. [Arquitectura](#arquitectura)
6. [Decisiones técnicas](#decisiones-técnicas)
7. [Consideraciones de seguridad](#consideraciones-de-seguridad)
8. [Escalabilidad y grandes volúmenes de datos](#escalabilidad-y-grandes-volúmenes-de-datos)
9. [Concurrencia, cola, caché y webhooks](#concurrencia-cola-caché-y-webhooks)
10. [Despliegue en Kubernetes](#despliegue-en-kubernetes)
11. [Supuestos](#supuestos)

---

## Prerrequisitos

- Docker y Docker Compose
- Java 21
- Node.js 20+
- `make`

---

## Inicio rápido

Desde una instalación limpia, el setup completo tarda menos de 5 minutos.

**1. Iniciar la infraestructura (Postgres + Redis)**

```bash
make dev
```

**2. Configurar variables de entorno (solo la primera vez)**

El backend lee su configuración desde `apps/backend/.env`. Los valores por defecto del archivo de ejemplo funcionan directamente con el Docker Compose:

```bash
cp apps/backend/.env.example apps/backend/.env
```

**3. Ejecutar el backend**

```bash
make run-backend
```

Spring Boot ejecuta las migraciones de Flyway automáticamente al arrancar. La API queda disponible en `http://localhost:8080`.

**4. Ejecutar el frontend**

```bash
make run-frontend
```

La interfaz queda disponible en `http://localhost:5173`.

**5. Iniciar sesión**

Credenciales del usuario administrador creadas por la migración inicial:

| Campo      | Valor      |
|------------|------------|
| Usuario    | `admin`    |
| Contraseña | `admin123` |

> Cambiar antes de cualquier despliegue real.

---

## Comandos disponibles

| Comando               | Descripción                                                  |
|-----------------------|--------------------------------------------------------------|
| `make dev`            | Inicia Postgres y Redis con Docker Compose                   |
| `make dev-down`       | Detiene y elimina los contenedores y volúmenes               |
| `make run-backend`    | Ejecuta el backend Spring Boot (compila desde fuente)        |
| `make run-frontend`   | Ejecuta el servidor de desarrollo Vite                       |
| `make build`          | Construye las imágenes Docker del backend y frontend         |
| `make push`           | Construye y publica las imágenes en el registro configurado  |
| `make deploy`         | Aplica todos los manifiestos de Kubernetes                   |
| `make undeploy`       | Elimina todos los recursos de Kubernetes                     |
| `make logs-backend`   | Sigue los logs del backend en Kubernetes                     |
| `make logs-frontend`  | Sigue los logs del frontend en Kubernetes                    |

El registro y el tag de imagen son configurables:

```bash
make push REGISTRY=miregistro.io TAG=v1.0.0
```

---

## Modelo de datos

### `credit_applications`

Tabla principal. Almacena todos los datos de la solicitud, incluyendo la información bancaria desnormalizada obtenida del proveedor correspondiente al momento de la creación.

| Columna               | Tipo           | Notas                                              |
|-----------------------|----------------|----------------------------------------------------|
| `id`                  | UUID           | Clave primaria, generada automáticamente           |
| `country`             | VARCHAR(5)     | Código ISO del país (MX, CO)                      |
| `full_name`           | VARCHAR(255)   | Nombre completo del solicitante                    |
| `document_id`         | VARCHAR(50)    | Documento de identidad según país                  |
| `requested_amount`    | DECIMAL(15,2)  | Monto solicitado                                   |
| `monthly_income`      | DECIMAL(15,2)  | Ingreso mensual declarado                          |
| `status`              | VARCHAR(30)    | PENDING / UNDER_REVIEW / APPROVED / REJECTED       |
| `bank_account_number` | VARCHAR(100)   | Del proveedor bancario (no expuesto en la API)     |
| `bank_total_debt`     | DECIMAL(15,2)  | Del proveedor bancario                             |
| `bank_credit_score`   | INTEGER        | Del proveedor bancario                             |
| `bank_name`           | VARCHAR(100)   | Nombre del proveedor                               |
| `bank_currency`       | VARCHAR(3)     | Código ISO de moneda                               |
| `created_at`          | TIMESTAMPTZ    |                                                    |
| `updated_at`          | TIMESTAMPTZ    |                                                    |

### `job_queue`

Cola de trabajos asíncronos respaldada por PostgreSQL. Los trabajos se encolan automáticamente mediante un trigger de base de datos al crear cada solicitud.

| Columna          | Tipo        | Notas                                              |
|------------------|-------------|----------------------------------------------------|
| `id`             | UUID        | Clave primaria                                     |
| `application_id` | UUID        | FK → credit_applications                           |
| `job_type`       | VARCHAR(50) | p. ej. `RISK_EVALUATION`                           |
| `status`         | ENUM        | PENDING → PROCESSING → DONE / FAILED              |
| `attempts`       | INTEGER     | Contador de reintentos                             |
| `payload`        | JSONB       | Metadatos arbitrarios del trabajo                  |
| `error_message`  | TEXT        | Motivo del último fallo                            |
| `created_at`     | TIMESTAMPTZ |                                                    |
| `processed_at`   | TIMESTAMPTZ |                                                    |

### `application_status_history`

Registro de auditoría inmutable. Poblado automáticamente por un trigger de base de datos en cada cambio de estado de `credit_applications`.

| Columna           | Tipo        | Notas                                               |
|-------------------|-------------|-----------------------------------------------------|
| `id`              | UUID        | Clave primaria                                      |
| `application_id`  | UUID        | FK → credit_applications                            |
| `previous_status` | VARCHAR(30) | Estado anterior                                     |
| `new_status`      | VARCHAR(30) | Estado nuevo                                        |
| `changed_by`      | VARCHAR(100)| Actor: nombre de usuario o `risk-worker`            |
| `source`          | ENUM        | API / WORKER / WEBHOOK / SYSTEM                    |
| `reason`          | TEXT        | Contexto opcional                                   |
| `changed_at`      | TIMESTAMPTZ |                                                     |

### `app_users`

Usuarios de la aplicación para autenticación JWT.

| Columna          | Tipo        | Notas                                         |
|------------------|-------------|-----------------------------------------------|
| `id`             | UUID        | Clave primaria                                |
| `username`       | VARCHAR(100)| Único                                         |
| `password_hash`  | VARCHAR(255)| bcrypt                                        |
| `role`           | ENUM        | ADMIN / USER / ANALYST / RISK_MANAGER        |
| `created_at`     | TIMESTAMPTZ |                                               |

### Triggers de base de datos

**`trg_enqueue_on_insert`** — Se ejecuta `AFTER INSERT ON credit_applications`. Inserta automáticamente un trabajo de tipo `RISK_EVALUATION` en `job_queue`. Ningún código de aplicación gestiona este encolamiento; es garantizado por la base de datos.

**`trg_status_change_audit`** — Se ejecuta `AFTER UPDATE ON credit_applications` cuando cambia `status`. Lee las variables de sesión de PostgreSQL `app.audit_source` y `app.changed_by` (establecidas por la aplicación con `set_config()` dentro de la misma transacción) y escribe una fila en `application_status_history`. Al ocurrir dentro de la misma transacción que el `UPDATE`, el registro de auditoría es atómico: si la transacción se revierte, la auditoría también se revierte.

---

## Arquitectura

El backend sigue **Arquitectura Limpia** (también conocida como Hexagonal / Puertos y Adaptadores):

```
presentation/       → Controladores, DTOs, manejadores de excepciones (adaptadores de entrada)
application/        → Casos de uso (orquestación, sin dependencias de frameworks)
domain/             → Entidades, objetos de valor, puertos de dominio (interfaces), políticas
infrastructure/     → Implementaciones: JPA, Redis, SSE, workers, seguridad, webhooks
```

Las dependencias apuntan únicamente hacia adentro. El dominio no conoce Spring, JPA ni Redis. Esto hace que la lógica de negocio sea portable e independientemente verificable.

### Patrón Strategy — Políticas por país y proveedores bancarios

Agregar un nuevo país requiere implementar dos interfaces y registrarlas como beans de Spring:

**`CreditPolicy`** (capa de dominio):

```
country()           → identifica el país que gestiona esta política
validateDocument()  → valida el formato del documento de identidad específico del país
validateRules()     → aplica reglas de crédito del país (ratio de ingresos, límites de deuda, etc.)
evaluateRisk()      → devuelve un ApplicationStatus basado en los datos de la solicitud
```

**`BankingProvider`** (puerto de dominio):

```
country()           → identifica el país al que sirve este proveedor
getInfo()           → devuelve BankingInfo (score, deuda, cuenta, moneda, nombre del banco)
```

**`CountryStrategyConfig`** (infraestructura) recoge todos los beans de `CreditPolicy` y `BankingProvider` y construye los mapas `Map<CountryCode, CreditPolicy>` y `Map<CountryCode, BankingProvider>`. Los casos de uso inyectan estos mapas y delegan al proveedor correcto por código de país. Agregar un nuevo país sigue el principio abierto/cerrado: basta con declarar el nuevo bean y Spring lo registra automáticamente. Ningún código existente necesita cambiar. La misma configuración puede extenderse con nuevos mapas indexados por país (p. ej., `Map<CountryCode, ComplianceChecker>`) sin tocar el código existente.

Países implementados: **México (MX)** y **Colombia (CO)**.

---

## Decisiones técnicas

### SSE en lugar de WebSockets para actualizaciones en tiempo real

El frontend necesita _recibir_ eventos de cambio de estado desde el backend. Nunca necesita _enviar_ mensajes de vuelta por el mismo canal. Server-Sent Events (SSE) es la elección exacta: unidireccional, nativo de HTTP/1.1 y trivialmente integrado en Spring mediante `SseEmitter`. Los WebSockets añaden complejidad de protocolo bidireccional, un handshake separado y gestión de estado de conexión que este caso de uso no justifica.

La API `EventSource` del navegador no permite establecer cabeceras personalizadas, lo que significa que la cabecera estándar `Authorization: Bearer <token>` no puede enviarse. El `JwtAuthenticationFilter` se extiende para aceptar también un query param `?token=` como alternativa, utilizado exclusivamente en el endpoint SSE. La desventaja es que el token aparece en los logs de acceso del servidor en ese endpoint — una limitación conocida y aceptable para un MVP.

### PostgreSQL como cola de trabajos (SKIP LOCKED)

En lugar de introducir un broker de mensajes (Kafka, RabbitMQ), la cola de trabajos está respaldada por la tabla `job_queue`. Los workers reclaman trabajos usando:

```sql
SELECT ... FROM job_queue WHERE status = 'PENDING'
ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1
```

`SKIP LOCKED` hace que los workers concurrentes omitan filas ya bloqueadas por otra transacción, proporcionando distribución natural sin contención. Este enfoque no tiene dependencias externas y es operacionalmente más simple. Es adecuado para la escala de este MVP; un broker dedicado sería la evolución correcta a mayor throughput.

### Auditoría mediante triggers de PostgreSQL y variables de sesión

El historial de auditoría (`application_status_history`) es escrito por un trigger de base de datos, no por código de aplicación. El trigger lee `app.audit_source` y `app.changed_by` desde variables de configuración locales a la sesión de PostgreSQL, establecidas por la aplicación con `set_config(..., true)` (local = scoped a la transacción actual). Esto garantiza que el registro de auditoría se escribe en la misma transacción que la actualización de estado, garantizando que nunca pueda existir un estado donde el `UPDATE` se refleje en `credit_applications` pero no en el historial, o viceversa.

### Aislamiento transaccional en el worker de riesgo

`RiskEvaluationWorker` separa la transacción de base de datos de los efectos secundarios externos. `RiskEvaluationTxProcessor` ejecuta la reclamación del trabajo, la evaluación de dominio y la actualización de estado dentro de un límite `@Transactional`. Solo después de que la transacción hace commit, el worker llama al notificador de webhook y al publicador de eventos SSE. Esto evita mantener una transacción de base de datos abierta mientras se espera una llamada HTTP, y previene que se envíen eventos SSE o webhooks para actualizaciones que posteriormente se revirtieron.

### Guardia de estado terminal en el worker

`ApplicationStatus.isTerminal()` es verificado por el worker antes de intentar una evaluación de riesgo. Si otro actor (p. ej., un analista humano a través de la API) ya aprobó o rechazó la solicitud mientras el trabajo estaba encolado, el worker lo omite limpiamente en lugar de lanzar un error de transición inválida. Esto maneja el escenario de "trabajo tardío" en una recuperación de fallo parcial.

### Caché Redis — write-through, solo por ID

Las consultas individuales de solicitudes por ID se cachean en Redis con TTL de 1 día. El caché se actualiza en cada `save()` (write-through), por lo que siempre es consistente con la última escritura. Las consultas de lista (`GET /applications?country=X&status=Y`) intencionalmente no se cachean: con el worker de riesgo ejecutándose cada 5 segundos y produciendo escrituras continuas, cualquier caché de lista sería invalidado antes de poder ser servido dos veces.

---

## Consideraciones de seguridad

**Autenticación**: Todos los endpoints de la API excepto `POST /api/v1/auth/login` requieren un JWT válido en la cabecera `Authorization: Bearer`. Los tokens se firman con HMAC-SHA256 y expiran tras 24 horas (configurable con `JWT_EXPIRATION_MS`).

**Almacenamiento de contraseñas**: Las contraseñas de los usuarios se hashean con bcrypt antes de persistirlas. Las contraseñas en texto plano nunca se almacenan.

**Manejo de PII y datos bancarios**: El DTO `CreditApplicationResponse` omite deliberadamente `bank_account_number`. Los detalles bancarios (deuda, score, nombre del banco, moneda) se incluyen solo donde son necesarios para la interfaz. El payload del evento SSE usa el mismo DTO, de modo que los datos bancarios sensibles no se difunden a través del flujo de eventos.

**Token en query param**: El endpoint SSE acepta el JWT como `?token=` porque `EventSource` no puede enviar cabeceras personalizadas. Esto significa que el token aparece en los logs de acceso del servidor. En producción, esto se mitiga emitiendo tokens de vida corta exclusivos para SSE, o colocando la aplicación detrás de un proxy inverso que elimine los query params de los logs.

**Inyección SQL**: Todas las interacciones con la base de datos usan parámetros nombrados de JPA o repositorios de Spring Data. El contexto de auditoría usa `setParameter()` con parámetros nombrados en queries nativas, nunca concatenación de strings.

**Credenciales por defecto**: La migración crea un usuario `admin` con contraseña `admin123`. Debe cambiarse antes de cualquier despliegue en producción.

---

## Escalabilidad y grandes volúmenes de datos

### Índices existentes

La migración inicial crea los siguientes índices:

| Índice                               | Columnas                                           | Propósito                                      |
|--------------------------------------|----------------------------------------------------|------------------------------------------------|
| `idx_applications_country`           | `(country)`                                        | Filtro por país                                |
| `idx_applications_status`            | `(status)`                                         | Filtro por estado                              |
| `idx_applications_country_status`    | `(country, status)`                                | Filtro compuesto para la consulta principal    |
| `idx_applications_created_at`        | `(created_at DESC)`                                | Ordenación por fecha de creación               |
| `idx_job_queue_pending`              | `(status, created_at) WHERE status = 'PENDING'`    | Solo indexa filas pendientes; permanece pequeño aunque acumulen millones de trabajos completados |
| `idx_status_history_application`     | `(application_id, changed_at DESC)`                | Historial de auditoría por solicitud           |

### Índices adicionales recomendados

A medida que los datos crecen:

- **`(country, status, created_at DESC)`** — Índice cobertor para la consulta de lista paginada, eliminando un paso de ordenación adicional.
- **`(document_id)`** — Si la deduplicación o búsqueda por solicitante se convierte en una funcionalidad requerida.
- **`(application_id)` en `job_queue`** — Para consultar todos los trabajos de una solicitud concreta (depuración, reintentos).

### Particionamiento de tablas

`credit_applications` es la tabla de mayor volumen. Dos estrategias viables:

**Particionamiento por rango en `created_at`** (paso inicial recomendado): Particiones mensuales o trimestrales. Las consultas filtradas por rango de fechas solo acceden a las particiones relevantes. Las particiones antiguas se pueden desconectar y archivar sin bloquear la tabla activa. `pg_partman` automatiza la creación y retención de particiones.

**Particionamiento por lista en `country`**: Si la aplicación se expande a muchos países con requisitos regulatorios independientes y patrones de consulta aislados, particionar por país permite gestionar, indexar y archivar los datos de cada país de forma independiente. Puede combinarse con el particionamiento por fecha como subpartición.

### Consultas críticas y cómo evitar cuellos de botella

**Consulta de lista** (`SELECT ... WHERE country = ? AND status = ?`): Resuelta por el índice compuesto. A muy alto volumen, agregar paginación basada en cursor (keyset pagination sobre `(created_at, id)`) en lugar de `OFFSET`, que se degrada a medida que el offset crece.

**Sondeo del worker** (`SELECT ... WHERE status = 'PENDING' ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1`): El índice parcial sobre `(status, created_at) WHERE status = 'PENDING'` significa que este escaneo solo toca filas pendientes. A mayor throughput, aumentar el `LIMIT` por ciclo y/o desplegar más instancias del servicio.

**Consulta de historial de auditoría**: El índice `(application_id, changed_at DESC)` gestiona eficientemente las páginas de historial por solicitud independientemente del tamaño total de la tabla.

**Actualizaciones frecuentes de estado**: `credit_applications` recibirá muchos `UPDATE`s (cambios de `status`, `updated_at`). En PostgreSQL, cada `UPDATE` produce una nueva versión de la fila (MVCC), lo que genera bloat con el tiempo y mayor carga en el autovacuum. A muy alto volumen, el enfoque alternativo es event sourcing: cada transición de estado se registra como un `INSERT` en una tabla de solo-append (similar a `application_status_history`, que ya existe), y el estado actual de una solicitud se calcula a partir del último evento. Una vista materializada o una tabla de "estado actual" denormalizada sirve las lecturas. Esto convierte las escrituras en inserciones puras, que escalan mejor bajo alta concurrencia.

### Archivado y compresión

Para un sistema en producción con millones de registros:

- **Desconexión de particiones**: Las solicitudes terminales (APPROVED / REJECTED) más antiguas que una ventana de retención definida pueden moverse de las particiones activas a una partición de archivo o almacenamiento externo. Desconectar una partición en PostgreSQL es una operación de metadatos — no bloquea la tabla activa.
- **Almacenamiento columnar**: Para cargas de trabajo analíticas sobre datos históricos, archivos Parquet en object storage (S3/GCS) accedidos mediante PostgreSQL Foreign Data Wrapper, o una capa OLAP dedicada (DuckDB, BigQuery, ClickHouse), mantienen la base de datos OLTP ligera.
- **`application_status_history`**: Al archivar una solicitud, sus filas de historial deben moverse primero, ya que `application_status_history.application_id` es una FK que referencia `credit_applications`. Eliminar la solicitud sin mover el historial viola la restricción de integridad referencial. El orden correcto es: exportar el historial → exportar la solicitud → eliminar el historial → eliminar la solicitud (o usar `ON DELETE CASCADE` si el historial se elimina junto con la solicitud).

---

## Concurrencia, cola, caché y webhooks

### Cola de trabajos

La cola es una tabla PostgreSQL (`job_queue`). El encolamiento lo realiza el trigger de base de datos `trg_enqueue_on_insert` — se dispara automáticamente en cada inserción en `credit_applications`, por lo que ningún código de aplicación puede olvidarse de encolar.

Los workers consumen trabajos con `SELECT FOR UPDATE SKIP LOCKED`. Este patrón proporciona:
- **Procesamiento sin colisiones**: una fila bloqueada es invisible para otros workers.
- **Escalabilidad horizontal**: ejecutar N instancias del servicio; cada una reclama trabajos diferentes sin necesidad de coordinación.
- **Tolerancia a fallos**: un trabajo permanece en `PENDING` hasta que se marca como `DONE` o `FAILED` en la misma transacción que evalúa el riesgo. Si el proceso muere, la transacción se revierte y el trabajo queda disponible para el siguiente ciclo.

En un despliegue con múltiples instancias de worker en producción, los trabajos transitarían por un estado `PROCESSING` explícito, acompañado de un mecanismo de recuperación basado en TTL o heartbeat para reclamar trabajos de workers caídos. La configuración actual de instancia única se apoya en el rollback transaccional para una seguridad equivalente.

El `RiskEvaluationWorker` procesa hasta 5 trabajos por ciclo de 5 segundos. Escalar significa aumentar el tamaño del lote o desplegar más instancias del servicio.

### Caché

Redis se usa como caché write-through para consultas individuales de solicitudes por ID:

- **Qué se cachea**: el objeto de solicitud completo, serializado como JSON.
- **Clave**: `credit-application::<uuid>`
- **TTL**: 1 día.
- **Write-through**: cada `save()` actualiza el caché inmediatamente. Caché y BD son siempre consistentes respecto a la última escritura.
- **Lectura**: `findById()` consulta Redis primero. Un cache miss cae a la BD sin poblar el caché en la lectura (la siguiente escritura lo hará).
- **Qué no se cachea**: consultas de lista. Dado que el worker se ejecuta cada 5 segundos produciendo escrituras constantes, cualquier lista cacheada estaría obsoleta antes de poder ser servida dos veces.
- **Los fallos de caché no son fatales**: las excepciones de Redis se capturan, se registran como advertencias y la operación continúa contra la base de datos.

### Webhooks

Cuando el estado de una solicitud cambia — ya sea a través de la API o del worker de evaluación de riesgo — el sistema llama a `WebhookNotifier.notify(application)`. El adaptador de infraestructura (`HttpWebhookNotifier`) envía un `POST` a la URL configurada en `WEBHOOK_URL` con la solicitud actualizada como cuerpo JSON.

Si `WEBHOOK_URL` está vacío, el notificador es un no-op. La llamada al webhook ocurre _fuera_ de la transacción de base de datos (después del commit), por lo que un endpoint externo lento o con fallos no afecta a la integridad de los datos. Un mecanismo resiliente de reintentos involucraría el patrón transactional outbox.

### Actualizaciones en tiempo real (SSE)

`SseEmitterRegistry` mantiene un conjunto thread-safe de instancias `SseEmitter` activas (una por pestaña del navegador conectada). En cualquier cambio de estado — desde la API o el worker — `SseApplicationEventPublisher.publish()` difunde un evento `application-update` a todos los clientes conectados. Los emitters obsoletos (clientes desconectados) se eliminan de forma lazy en el siguiente intento de difusión.

---

## Despliegue en Kubernetes

Los manifiestos están en `infra/k8s/`. Aplicar con `make deploy`.

```
infra/k8s/
  namespace.yaml          → namespace bravo
  postgres/               → Secret, PVC, Deployment, Service
  redis/                  → Deployment, Service
  backend/                → ConfigMap, Secret, Deployment, Service
  frontend/               → Deployment, Service
  ingress.yaml            → Ingress nginx: /api → backend, / → frontend
```

**Ingress y SSE**: El Ingress nginx tiene `proxy-buffering: off` y timeouts de lectura/envío de 1 hora. Esto es imprescindible para SSE — sin deshabilitar el buffering, nginx retiene eventos en memoria y los entrega en ráfagas en lugar de transmitirlos de forma inmediata.

**Orden de arranque**: El Deployment del backend incluye un init container que espera a que Postgres esté disponible (`pg_isready`) antes de que arranque el proceso de Spring Boot, evitando fallos de migración de Flyway en arranques en frío del clúster.

**Secretos**: Los archivos `infra/k8s/postgres/secret.yaml` e `infra/k8s/backend/secret.yaml` contienen marcadores `CHANGE_ME`. En producción, usar una solución de gestión de secretos (Sealed Secrets, External Secrets Operator, o `kubectl create secret` directamente) en lugar de incluir valores reales en el repositorio.

**Escalado del backend**: El backend es stateless. Aumentar `replicas` en `infra/k8s/backend/deployment.yaml`. Múltiples réplicas ejecutan cada una su propio `RiskEvaluationWorker`; `SKIP LOCKED` garantiza que no procesen el mismo trabajo dos veces.

---

## Supuestos

- **Dos países implementados**: México (MX) y Colombia (CO). La arquitectura permite agregar más implementando `CreditPolicy` y `BankingProvider` para cada nuevo `CountryCode`.
- **Datos bancarios simulados**: `MexicoBankingProvider` y `ColombiaBankingProvider` devuelven datos aleatorios pero estructuralmente válidos. En producción, estas clases llamarían a APIs bancarias externas reales.
- **Flujo de estados compartido**: Se usa un único grafo de transiciones para ambos países. El worker de riesgo transiciona directamente de `PENDING` a `APPROVED` (con los datos bancarios simulados, el score siempre es suficientemente alto). El estado `UNDER_REVIEW` es alcanzable únicamente a través de la actualización manual de estado vía API. `REJECTED` está soportado en el dominio pero no se produce en la implementación actual. El método `CreditPolicy.evaluateRisk()` puede devolver diferentes transiciones por país, habilitando flujos divergentes sin cambios de esquema.
- **Filtros de listado obligatorios**: La consulta de lista requiere `country` y `status` como parámetros obligatorios. Este supuesto es el que justifica el índice compuesto `(country, status)`: una consulta sin ambos filtros no se beneficiaría de él.
- **Rol único para todas las acciones de la API**: Cualquier usuario autenticado puede crear, listar y actualizar solicitudes. Un sistema en producción añadiría autorización por rol en cada endpoint (p. ej., solo `RISK_MANAGER` puede aprobar).
- **Sin HTTPS en local**: La terminación TLS se espera en el Ingress o balanceador de carga en producción. El entorno local funciona sobre HTTP plano.
- **Firma JWT simétrica**: HMAC-SHA256 con un secreto compartido. La firma asimétrica (RS256) sería preferible para una arquitectura multiservicio donde otros servicios necesiten verificar tokens sin compartir el secreto.

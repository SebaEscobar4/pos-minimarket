# POS Minimarket

[![CI](https://github.com/SebaEscobar4/pos-minimarket/actions/workflows/ci.yml/badge.svg)](https://github.com/SebaEscobar4/pos-minimarket/actions/workflows/ci.yml)

Sistema web de punto de venta e inventario para un minimarket familiar de una sola sucursal y una sola caja. Registra ventas, controla el efectivo esperado en caja y mantiene un inventario trazable, con cada operación crítica auditada por usuario y fecha.

Es un monolito modular con backend en Spring Boot, frontend en React y PostgreSQL. Las reglas de negocio están documentadas en [`docs/`](docs/) y las decisiones de arquitectura en [`adr/`](adr/).

## Funcionalidades

- **Identidad y acceso:** inicio de sesión con sesión revocable en servidor, contraseñas con hash Argon2id, cambio obligatorio de la contraseña inicial y límite de intentos fallidos.
- **Catálogo:** categorías y productos, con búsqueda unificada por código de barras o nombre.
- **Inventario:** saldo por producto respaldado por un libro de movimientos; el stock nunca cambia sin dejar registro.
- **Caja:** apertura con monto inicial, movimientos, efectivo esperado, cierre y diferencias.
- **Ventas:** carrito, pago en efectivo, tarjeta o transferencia, cálculo de vuelto, comprobante interno, historial y anulación. Cada confirmación y anulación actualiza venta, inventario y caja de forma atómica.
- **Reportes:** conciliación de ventas y anulaciones, productos más vendidos, margen bruto estimado desde el costo histórico, stock bajo, libro de inventario y cierres de caja.
- **Auditoría:** proyección cronológica de las operaciones críticas.

## Stack

| Capa | Tecnologías |
|---|---|
| Backend | Java 21, Spring Boot 4.1 (Web, Security, Data JPA, Validation, Session JDBC, Actuator), Flyway |
| Frontend | React 19, TypeScript 5.9, Vite 8 |
| Datos | PostgreSQL 18 |
| Pruebas | JUnit 5, Spring Security Test, Testcontainers, ArchUnit, Vitest, Testing Library |
| Calidad | JaCoCo, SpotBugs, Spotless, Maven Enforcer, ESLint, Prettier |
| Entorno | Docker Compose, GitHub Actions, Dependabot, Trivy |

## Arquitectura

```text
Navegador
   │
   ▼
React + Vite (:5173)
   │  /api y /actuator
   ▼
Spring Boot (:8080)
   │  identity · catalog · inventory · cash · sales · reporting · shared
   ▼
PostgreSQL 18 (:5432)
```

- El backend se divide en módulos de dominio (`identity`, `catalog`, `inventory`, `cash`, `sales`, `reporting`, `shared`). Una prueba ArchUnit impide ciclos entre ellos.
- El frontend replica esos límites bajo `src/features`.
- Flyway es el único mecanismo de esquema; Hibernate usa `ddl-auto=validate`.
- El dinero se maneja en CLP exacto y las cantidades como enteros.
- Los errores HTTP usan `application/problem+json` con un identificador de correlación.

Las decisiones y sus alternativas están en los ADR:

| ADR | Decisión |
|---|---|
| [ADR-001](adr/ADR-001-modular-monolith.md) | Monolito modular |
| [ADR-002](adr/ADR-002-java-maven-stack.md) | Java 21, Spring Boot y Maven |
| [ADR-003](adr/ADR-003-inventory-balance-ledger.md) | Saldo más historial de inventario |
| [ADR-004](adr/ADR-004-sale-transaction-concurrency-idempotency.md) | Transacción, bloqueo e idempotencia de la venta |
| [ADR-005](adr/ADR-005-session-authentication.md) | Sesión revocable y cookie segura |
| [ADR-006](adr/ADR-006-scanner-http-sse.md) | Lector mediante HTTP y SSE |
| [ADR-007](adr/ADR-007-single-react-app-and-origin.md) | Una aplicación React y mismo origen |
| [ADR-008](adr/ADR-008-money-and-quantity.md) | Dinero CLP exacto y cantidades enteras |
| [ADR-009](adr/ADR-009-product-search.md) | Búsqueda unificada de productos |
| [ADR-010](adr/ADR-010-free-tier-infrastructure.md) | Infraestructura sin costo |

## Ejecución local

Requisitos: JDK 21, Node.js 24 LTS con npm 11 o superior, Docker con Compose y Git. No se requiere ningún servicio pagado.

### 1. Base de datos

```bash
docker compose up -d postgres
docker compose ps
```

### 2. Backend

En la primera ejecución sobre una base sin usuarios, define estas variables de entorno fuera del repositorio:

- `POS_BOOTSTRAP_ADMIN_USERNAME`: nombre del administrador inicial.
- `POS_BOOTSTRAP_ADMIN_PASSWORD`: contraseña temporal única de 12 a 128 caracteres.
- `POS_BOOTSTRAP_ADMIN_DISPLAY_NAME`: nombre visible (opcional).

El backend guarda únicamente el hash Argon2id, exige reemplazar la contraseña antes de operar y deja de usar el bootstrap cuando ya existe un usuario. Retira las variables sensibles después de crear el administrador.

Linux o macOS:

```bash
cd backend
./mvnw spring-boot:run
```

Windows (PowerShell o CMD):

```text
cd backend
mvnw.cmd spring-boot:run
```

La primera ejecución descarga Maven, valida Java 21 y aplica las migraciones Flyway. El estado de salud queda en `http://localhost:8080/actuator/health`.

### 3. Frontend

```bash
cd frontend
npm ci
npm run dev
```

La interfaz queda en `http://localhost:5173` y redirige `/api` y `/actuator` al backend.

## Pruebas y calidad

Backend rápido, sin contenedores:

```bash
cd backend
./mvnw test
```

Backend completo (requiere Docker para Testcontainers):

```bash
cd backend
./mvnw verify
```

Frontend completo (formato, lint, pruebas con cobertura y build):

```bash
cd frontend
npm run quality
npm audit --audit-level=high
```

El pipeline de GitHub Actions ejecuta lo mismo en cada push: compilación, formato, pruebas, cobertura, límites de arquitectura, análisis estático, migración sobre PostgreSQL limpio y escaneo de vulnerabilidades, secretos y configuración con Trivy.

## Configuración

- `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` reemplazan los valores locales del backend.
- `POS_COOKIE_SECURE=true` es obligatorio al publicar mediante HTTPS; localmente permanece en `false`.
- `POS_ALLOWED_ORIGINS` acepta una lista exacta separada por comas.
- Las credenciales incluidas como valores predeterminados son exclusivamente locales y no deben reutilizarse en ambientes públicos. Los secretos no se guardan en Git.

## Respaldo local

Con PostgreSQL levantado y sin operaciones en curso, PowerShell puede crear un respaldo local y su checksum SHA-256:

```powershell
.\scripts\backup-postgres.ps1
```

El resultado se guarda en `backups/`, que Git ignora. La restauración se ensaya sobre una base temporal, sin sobrescribir la base principal:

```powershell
.\scripts\test-postgres-restore.ps1 -BackupPath .\backups\pos-AAAAMMDDTHHMMSSZ-identificador.dump
```

El respaldo todavía no está cifrado: es preparación operacional local y no debe considerarse respaldo productivo.

## Estructura

```text
backend/     Spring Boot y módulos de dominio
frontend/    React por funcionalidades
docs/        Alcance, reglas de negocio, calidad y backlog
adr/         Decisiones arquitectónicas
scripts/     Respaldo y restauración de PostgreSQL
```

## Estado

MVP operativo de venta, catálogo, inventario, caja, reportes y auditoría. Pendiente:

- Lector de códigos de barras desde el teléfono ([ADR-006](adr/ADR-006-scanner-http-sse.md)): diseñado, aún sin implementar.
- Tiempo de inactividad y duración máxima de la sesión (decisión OQ-01 diferida).
- Cifrado y política de retención de los respaldos.

## Autor

Sebastián Escobar · [github.com/SebaEscobar4](https://github.com/SebaEscobar4)

## Licencia

[MIT](LICENSE)

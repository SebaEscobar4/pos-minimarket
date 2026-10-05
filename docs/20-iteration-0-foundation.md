# 20 - Iteración 0: fundación técnica

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Validado localmente

**Versión:** 0.1

**Fecha:** 2026-08-03

## 1. Alcance implementado

- US-000: estructura backend/frontend y límites modulares.
- US-001: PostgreSQL local reproducible con Docker Compose.
- US-002: Flyway y migración inicial.
- US-003: controles gratuitos de compilación, formato, pruebas, cobertura, arquitectura, análisis estático, dependencias, secretos y configuración.
- US-004: correlación, Problem Details, salud y logging seguro.

No se incorporó lógica de catálogo, inventario, caja, venta, identidad ni lector.

## 2. Versiones

| Componente | Decisión |
|---|---|
| Java | 21 LTS |
| Spring Boot | 4.1.0 estable |
| Maven | Wrapper 3.3.4; distribución 3.9.16 |
| PostgreSQL | 18.4 estable; imagen Alpine local |
| Node.js | 24.19.0 LTS |
| React / Vite | 19.2.8 / 8.2.0 |
| TypeScript | 5.9.3 por compatibilidad del analizador estático |

## 3. Evidencia esperada

| Comando | Evidencia |
|---|---|
| `./mvnw test` | Unitarias y arquitectura sin Docker |
| `./mvnw verify` | Lo anterior, integración PostgreSQL, cobertura y SpotBugs |
| `npm run quality` | Formato, ESLint, pruebas con cobertura y build |
| `npm audit --audit-level=high` | Dependencias frontend sin hallazgos altos bloqueantes |
| `docker compose up -d postgres` | PostgreSQL saludable y persistente localmente |

## 4. Validación local del 2026-08-04

La Iteración 0 se comprobó con Java 21.0.11, Maven 3.9.16 mediante Wrapper, Docker Engine 29.6.1, PostgreSQL 18.4, Node.js portable 24.19.0 y npm 11.17.0.

Durante la validación se corrigió la configuración de Flyway para Spring Boot 4.1: se utiliza `spring-boot-starter-flyway`, porque incluir solamente `flyway-core` no activaba la autoconfiguración modular y dejaba la migración sin ejecutar. Después de la corrección, `mvnw.cmd test`, `mvnw.cmd verify`, `npm run quality` y `npm audit --audit-level=high` finalizaron correctamente. PostgreSQL local quedó saludable mediante Docker Compose.

## 5. Salida

La Iteración 0 superó el gate local y habilitó el inicio de la Iteración 1. La validación en CI seguirá siendo necesaria antes de integrar o liberar cambios.

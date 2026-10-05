# Registro de decisiones arquitectónicas

Los ADR documentan decisiones con impacto estructural. Las decisiones de las Entregas 1 y 2 fueron aprobadas y se mantienen en estado **Aceptado**.

| ADR | Decisión |
|---|---|
| [ADR-001](ADR-001-modular-monolith.md) | Monolito modular |
| [ADR-002](ADR-002-java-maven-stack.md) | Java 21, Spring Boot y Maven |
| [ADR-003](ADR-003-inventory-balance-ledger.md) | Saldo más historial de inventario |
| [ADR-004](ADR-004-sale-transaction-concurrency-idempotency.md) | Transacción, bloqueo e idempotencia |
| [ADR-005](ADR-005-session-authentication.md) | Sesión revocable y cookie segura |
| [ADR-006](ADR-006-scanner-http-sse.md) | Lector mediante HTTP y SSE |
| [ADR-007](ADR-007-single-react-app-and-origin.md) | Una aplicación React y mismo origen preferido |
| [ADR-008](ADR-008-money-and-quantity.md) | Dinero CLP exacto y cantidades enteras |
| [ADR-009](ADR-009-product-search.md) | Búsqueda unificada de productos |
| [ADR-010](ADR-010-free-tier-infrastructure.md) | Infraestructura sin costo durante la etapa actual |

## Ciclo de estados

- **Propuesto:** pendiente de aprobación.
- **Aceptado:** decisión vigente.
- **Reemplazado:** otra ADR la sustituye.
- **Descartado:** se decidió no aplicarla.

Una ADR aceptada no se edita para ocultar un cambio. Si una decisión cambia, se crea otra ADR que explique el nuevo contexto y referencie la anterior.

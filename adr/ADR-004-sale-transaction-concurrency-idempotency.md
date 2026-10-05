# ADR-004 — Transacción, bloqueo e idempotencia de venta

**Estado:** Aceptado

**Fecha:** 2026-07-31

## Contexto

Una venta afecta líneas, pago, stock y caja. Dos solicitudes pueden competir por la última unidad o repetir una confirmación por doble clic, reintento o red inestable.

## Decisión

- Una única transacción `READ COMMITTED` coordina la confirmación.
- Se bloquea la caja abierta y las filas de saldo con `SELECT ... FOR UPDATE`.
- Los productos se bloquean ordenados por identificador.
- Cada intención lleva una clave UUID de idempotencia.
- La venta guarda clave única y hash canónico de solicitud.
- Misma clave/mismo hash devuelve el resultado previo; misma clave/otro hash produce conflicto.

## Consecuencias

### Positivas

- No existe venta parcial ni stock negativo.
- Comportamiento determinista ante doble envío.
- Diseño comprensible y comprobable con PostgreSQL real.

### Costos

- Las ventas concurrentes de los mismos productos esperan brevemente.
- Se deben controlar timeouts y posibles deadlocks.
- El orden y hash canónico requieren pruebas.

## Alternativas descartadas

- Validar stock sin bloqueo: condición de carrera.
- Lock global de inventario: serializa operaciones innecesariamente.
- Cola/Kafka: no resuelve la necesidad con menor complejidad.
- Aislamiento `SERIALIZABLE` general: mayor costo y reintentos sin necesidad demostrada.

# ADR-003 — Saldo de inventario más historial de movimientos

**Estado:** Aceptado

**Fecha:** 2026-07-31

## Contexto

Calcular el stock sumando todos los movimientos en cada búsqueda entrega trazabilidad, pero degrada consultas y complica bloqueos. Guardar solo un número actual es rápido, pero no explica cambios ni permite auditoría.

## Decisión

Mantener:

- `inventory_balance`: saldo operacional por producto.
- `inventory_movement`: libro histórico inmutable.

Todo cambio bloquea el saldo, calcula anterior/resultante, actualiza el saldo e inserta el movimiento dentro de una transacción. Se incorporará una reconciliación periódica para detectar divergencias.

## Consecuencias

### Positivas

- Consultas rápidas en el POS.
- Trazabilidad completa.
- Bloqueo claro de la unidad concurrente.

### Costos

- Dos representaciones deben mantenerse atómicamente.
- Requiere prueba de reconciliación.

## Alternativas descartadas

- Stock calculado siempre desde movimientos: innecesario para la operación frecuente.
- Solo columna `stock` en producto: insuficiente para auditoría.
- Event sourcing: complejidad desproporcionada.

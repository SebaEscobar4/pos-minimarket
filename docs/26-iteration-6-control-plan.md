# 26 - Plan de Iteración 6: control

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** US-061 y US-062 implementadas y validadas; iteración completada

**Fecha:** 2026-08-05

## Objetivo

Completar el control posterior a la venta con anulación administrativa, restitución transaccional de inventario, devolución reconciliable y consulta de auditoría. US-056 (comprobante interno) y el alcance mínimo de US-060 (historial y detalle) ya están operativos.

## US-061 - Anular venta como administrador

### Alcance

- Solo ADMIN puede anular una venta `CONFIRMED`.
- La solicitud exige motivo y método de devolución: efectivo, tarjeta o transferencia.
- Se conserva la venta original y se crea un registro único de anulación con actor, fecha, motivo, monto exacto y método.
- Cada línea genera un movimiento `SALE_REVERSAL` que repone exactamente su cantidad.
- Una devolución en efectivo exige una caja abierta, aplica el mismo redondeo legal al monto exacto y registra `CASH_REFUND` en la caja vigente.
- Tarjeta y transferencia se registran como devolución interna exacta; el sistema no ejecuta operaciones bancarias.
- Una segunda anulación o una carrera concurrente se rechaza sin efectos adicionales.

### Diseño propuesto

- Flyway V10 crea `sale_cancellation` con relación única a `sale` y restricciones monetarias e históricas.
- La migración reemplaza la protección de `sale` para permitir únicamente la transición `CONFIRMED` a `VOIDED` cuando ya existe su anulación; los demás campos permanecen inmutables.
- La transacción bloquea la venta, luego la caja cuando corresponda y finalmente los saldos de inventario en orden estable por UUID.
- Endpoint propuesto: `POST /api/v1/admin/sales/{saleId}/cancellations`.
- El historial y comprobante muestran estado, motivo, devolución y actor sin sobrescribir los datos originales.

### Evidencia obligatoria

- Pruebas unitarias de motivo, método, redondeo de devolución y segunda anulación.
- Integración con PostgreSQL para reposición exacta, salida de caja actual y medios no efectivos.
- Dos anulaciones concurrentes producen un solo registro y una sola reversión.
- Una falla intermedia revierte anulación, inventario, caja y estado.
- Una caja histórica cerrada continúa inmutable.

### Resultado de US-061

- Flyway V10, dominio, servicio transaccional y API administrativa implementados.
- Historial frontend con estado, detalle de anulación y confirmación explícita en dos pasos.
- Las devoluciones en efectivo aplican el mismo redondeo legal de la venta y afectan solamente la caja abierta actual.
- Las pruebas cubren autorización, validación, medios efectivos y electrónicos, inmutabilidad, concurrencia y rollback de todos los efectos ante una falla intermedia.
- Gate validado con `mvnw.cmd test`, `mvnw.cmd verify` y `npm run quality`. La auditoría npm offline informa cero vulnerabilidades; la consulta online del registro quedó bloqueada por el entorno de red.

## US-062 - Consultar auditoría de operaciones críticas

### Alcance

- Vista administrativa cronológica con actor, fecha, tipo, referencia y resumen no sensible.
- Incluye apertura/cierre y movimientos de caja, ajustes de inventario, confirmación/anulación de venta y eventos de autenticación ya registrados.
- Los datos se proyectan desde los libros existentes; no se duplica el historial en una segunda fuente mutable.
- Filtros mínimos por periodo, tipo de evento y referencia, interpretados en `America/Santiago`.

### API e interfaz propuestas

- `GET /api/v1/admin/audit-events` con paginación y filtros acotados.
- Apartado “Auditoría” visible únicamente para ADMIN.
- Enlaces desde una venta anulada hacia sus movimientos de inventario y caja mediante el folio interno.

### Resultado de US-062

- `GET /api/v1/admin/audit-events` proyecta autenticación, caja, inventario, ventas y anulaciones desde sus libros existentes, sin crear una segunda fuente mutable.
- Filtros validados por fechas inclusivas en `America/Santiago`, tipo exacto y referencia literal, con paginación entre 1 y 100 elementos.
- La respuesta conserva actor, instante, tipo, referencia y resumen seguro; no expone direcciones de origen ni credenciales.
- La sección frontend “Auditoría” está disponible solo para ADMIN, con filtros, tabla cronológica y paginación.
- Autorización, normalización, PostgreSQL real, API y UI están cubiertos por pruebas automatizadas.

## Orden de implementación

1. V10 y dominio de anulación.
2. Servicio transaccional, autorización y API.
3. Pruebas unitarias, integración, concurrencia e inmutabilidad.
4. Acción de anulación y detalle ampliado en el historial frontend.
5. Proyección de auditoría, filtros y pruebas de autorización.
6. Gate completo y prueba de caja negra con una venta controlada.

## Fuera de alcance

- Devoluciones parciales.
- Cambio de productos dentro de una venta confirmada.
- Ejecución automática de devoluciones bancarias.
- Nota de crédito o documento tributario electrónico; requiere la futura integración con SII.

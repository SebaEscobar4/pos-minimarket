# 27 - Iteraci&oacute;n 7: operaci&oacute;n

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** US-080 y US-081 implementadas y validadas; restricci&oacute;n administrativa de US-082 aplicada para el contexto actual de un &uacute;nico administrador

**Fecha:** 2026-08-05

## Alcance implementado

- Reporte de ventas por per&iacute;odo inclusivo y medio de pago, interpretado en `America/Santiago` y acotado a 366 d&iacute;as.
- Conciliaci&oacute;n visible entre ventas registradas, anuladas y netas, tanto en cantidad como en monto exacto.
- Resumen de devoluciones por medio, separando monto exacto y efectivo pagadero cuando interviene el redondeo legal.
- Productos m&aacute;s vendidos y margen bruto estimado exclusivamente desde l&iacute;neas de venta vigentes y su costo hist&oacute;rico guardado al confirmar.
- Stock bajo actual, totales por tipo de movimiento y libro paginado de inventario trazable a `inventory_movement`.
- Aperturas y cierres de caja, montos esperado y contado, y diferencia acumulada para el per&iacute;odo.
- Interfaz de tablas, filtros y totales sin gr&aacute;ficos complejos, conforme al alcance aprobado del MVP.

## API administrativa

| M&eacute;todo | Ruta | Resultado |
|---|---|---|
| `GET` | `/api/v1/admin/reports/sales` | Ventas, anulaciones, medios, margen y productos principales |
| `GET` | `/api/v1/admin/reports/inventory` | Stock bajo, agregados y libro de movimientos |
| `GET` | `/api/v1/admin/reports/cash` | Aperturas, cierres y diferencias de caja |

Los reportes son proyecciones de solo lectura del m&oacute;dulo `reporting`; no poseen tablas ni duplican libros operacionales. No fue necesaria una migraci&oacute;n Flyway.

## Seguridad y decisi&oacute;n abierta

- Las rutas exigen rol `ADMIN` en el backend y la navegaci&oacute;n se muestra solo al administrador.
- Esta restricci&oacute;n es consistente con la operaci&oacute;n real declarada: un solo usuario administrador controla el sistema.
- OQ-02, sobre un eventual alcance limitado de reportes para `SELLER`, permanece abierta. No se convirti&oacute; en requisito ni se expuso informaci&oacute;n administrativa al rol vendedor.

## Evidencia ejecutable

- Pruebas unitarias validan per&iacute;odos predeterminados, fechas inclusivas chilenas, filtros y paginaci&oacute;n.
- `ApplicationContextIT` concilia cuatro ventas, tres anulaciones, el margen hist&oacute;rico, movimientos de inventario y autorizaci&oacute;n contra PostgreSQL real.
- El frontend prueba consultas HTTP, conciliaci&oacute;n visible, productos principales y cambio entre reportes de ventas, inventario y caja.
- `mvnw.cmd test`: 98 pruebas aprobadas.
- `mvnw.cmd verify`: 98 pruebas unitarias y 17 pruebas de integraci&oacute;n aprobadas; cobertura, arquitectura, Flyway y SpotBugs aprobados.
- Pruebas frontend: 55 aprobadas; cobertura global de ramas 75,81%, formato, lint, TypeScript y build de producci&oacute;n aprobados.

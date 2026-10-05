# 25 - Iteración 5: ventas

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** US-050 a US-055 implementadas y validadas localmente; comprobante e historial mínimo incorporados para el MVP

**Fecha:** 2026-08-05

## Alcance implementado

- ADMIN y SELLER pueden crear una venta solamente con una caja abierta.
- La búsqueda única admite código exacto o nombre parcial; el carrito permite modificar cantidades y quitar productos.
- El backend vuelve a consultar productos, precios y existencias, y calcula los importes sin confiar en los totales del navegador.
- El pago puede ser efectivo, tarjeta o transferencia. En efectivo se aplica la regla legal chilena al total final, se exige monto recibido suficiente y se calcula el vuelto sobre el monto redondeado.
- La confirmación guarda número correlativo, total, forma de pago, líneas y fotografías históricas de código, nombre, precio de venta y costo.
- En una única transacción `READ COMMITTED` se guarda la venta, se descuenta inventario y, cuando corresponde, se registra el ingreso efectivo en caja.
- Una clave UUID de idempotencia permite reintentar sin duplicar ventas; reutilizarla con otro contenido devuelve conflicto.
- Al confirmar se muestra un comprobante interno no tributario. ADMIN puede consultar las ventas recientes y abrir su detalle.

## API incorporada

| Método | Ruta | Resultado |
|---|---|---|
| `POST` | `/api/v1/sales` | Confirma o recupera idempotentemente una venta |
| `GET` | `/api/v1/sales` | Lista paginada de ventas recientes para ADMIN |
| `GET` | `/api/v1/sales/{id}` | Devuelve el detalle histórico para ADMIN |

## Persistencia y concurrencia

- Flyway V8 crea la secuencia de folios y las tablas `sale`, `sale_line` y `payment` con restricciones e historial inmutable.
- Flyway V9 incorpora total efectivo y ajuste de redondeo. Conserva los pagos históricos con versión 0 y exige la política vigente en toda inserción nueva.
- La transacción bloquea primero la caja y después los saldos de inventario en orden estable por UUID de producto.
- Los movimientos `SALE_OUT` y `CASH_SALE` referencian la venta confirmada y no pueden quedar aplicados parcialmente.
- Los totales de tarjeta y transferencia se obtienen de pagos confirmados; el efectivo esperado conserva la fórmula de caja aprobada.

## Evidencia ejecutable

- Las pruebas unitarias cubren las diez terminaciones posibles del total, cantidades, cálculo de vuelto, hash canónico e idempotencia.
- `ApplicationContextIT` aplica las nueve migraciones sobre PostgreSQL limpio y verifica venta en efectivo con redondeo, tarjeta y transferencia, folio, comprobante, actualización de caja e inventario, autorización e inmutabilidad.
- Los casos negativos verifican caja cerrada, efectivo o stock insuficiente, contenido diferente con la misma clave y ausencia de efectos parciales.
- El frontend prueba búsqueda, carrito, efectivo insuficiente, confirmación y comprobante; el gate incluye formato, lint, cobertura y build de producción.

## Restricción monetaria conocida

El dominio de caja y ventas usa CLP entero, según las reglas aprobadas. Como el catálogo preexistente todavía admite precios con hasta dos decimales, la confirmación rechaza explícitamente un producto con precio de venta fraccionario en vez de redondearlo silenciosamente. Unificar la validación del catálogo requerirá una decisión y migración de datos separadas si ya existen precios fraccionarios.

## Base legal del redondeo

La Ley 20.956 y su reglamento establecen que los pagos en efectivo terminados entre 1 y 5 pesos bajan a la decena inferior y los terminados entre 6 y 9 suben a la decena superior. El ajuste no modifica el monto exacto del documento tributario. El POS conserva el total exacto de la venta y muestra separadamente el ajuste y el efectivo pagadero; tarjeta y transferencia no se redondean.

- Banco Central de Chile: <https://www.bcentral.cl/contenido/-/details/contenido-general-faqs>
- Decreto 1266 en Ley Chile: <https://www.bcn.cl/leychile/navegar?idNorma=1111243>

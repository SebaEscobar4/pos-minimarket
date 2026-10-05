# 05 — Reglas de negocio

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Aprobado - Entrega 1

**Versión:** 0.1

**Fecha:** 2026-07-31

Los identificadores de estas reglas serán estables y se utilizarán en historias, criterios de aceptación, pruebas y trazabilidad.

## 1. Reglas generales

| ID | Regla |
|---|---|
| BR-GEN-001 | Las cantidades del MVP son números enteros. |
| BR-GEN-002 | Los montos se expresan en CLP mediante tipos exactos; se prohíben cálculos monetarios con punto flotante. |
| BR-GEN-003 | La zona horaria comercial es `America/Santiago`; los instantes persistidos deben permitir una interpretación inequívoca. |
| BR-GEN-004 | Toda acción crítica registra usuario, fecha y contexto suficiente para auditoría. |
| BR-GEN-005 | Los registros históricos importantes no se eliminan físicamente; se desactivan o compensan. |
| BR-GEN-006 | Toda validación crítica se ejecuta en backend aunque también exista en frontend. |

## 2. Usuarios y permisos

| ID | Regla |
|---|---|
| BR-ID-001 | Solo un usuario activo y autenticado puede ejecutar operaciones internas. |
| BR-ID-002 | Un vendedor puede abrir/cerrar caja y registrar ventas. |
| BR-ID-003 | Solo un administrador puede gestionar usuarios, ajustar inventario o anular ventas. |
| BR-ID-004 | Ocultar una acción en la interfaz no reemplaza la autorización del backend. |

## 3. Catálogo y búsqueda

| ID | Regla |
|---|---|
| BR-CAT-001 | Un código de barras es opcional, textual y único cuando está informado. |
| BR-CAT-002 | Los ceros iniciales de un código forman parte de su valor y no se eliminan. |
| BR-CAT-003 | Un producto inactivo no puede incorporarse a nuevas ventas. |
| BR-CAT-004 | Desactivar o editar un producto no modifica ventas históricas. |
| BR-CAT-005 | El stock de un producto no se modifica desde la edición del producto. |
| BR-CAT-006 | Crear un producto con stock inicial requiere un movimiento de inventario explícito. |
| BR-SEARCH-001 | La búsqueda por código utiliza coincidencia exacta después de normalizar únicamente espacios externos. |
| BR-SEARCH-002 | La búsqueda por nombre permite coincidencia parcial sin distinguir mayúsculas, minúsculas ni acentos. |
| BR-SEARCH-003 | El POS solo devuelve productos activos; administración puede incluir inactivos mediante un filtro explícito. |
| BR-SEARCH-004 | Los resultados del POS muestran precio y saldo vigente obtenidos desde backend. |
| BR-SEARCH-005 | Un producto sin stock puede mostrarse como no disponible, pero no agregarse al carrito. |
| BR-SEARCH-006 | Un código desconocido no crea automáticamente un producto ni una línea. |
| BR-SEARCH-007 | El lector móvil solo entrega el código; el backend resuelve todos los datos del producto. |

## 4. Inventario

| ID | Regla |
|---|---|
| BR-INV-001 | El stock cambia exclusivamente mediante movimientos de inventario. |
| BR-INV-002 | Todo movimiento registra producto, tipo, cantidad, saldo anterior, saldo resultante, fecha, usuario, motivo o referencia. |
| BR-INV-003 | El saldo actual y su movimiento se guardan en la misma transacción. |
| BR-INV-004 | El stock nunca puede quedar negativo. |
| BR-INV-005 | Una venta no puede consumir más unidades que el saldo disponible al momento de confirmar. |
| BR-INV-006 | Las operaciones concurrentes no pueden vender dos veces la última unidad. |
| BR-INV-007 | Un ajuste manual requiere un motivo no vacío. |
| BR-INV-008 | Un movimiento confirmado no se edita ni elimina; un error se corrige con otro movimiento. |
| BR-INV-009 | Una anulación válida repone exactamente las cantidades vendidas mediante movimientos de reversión. |

## 5. Caja

| ID | Regla |
|---|---|
| BR-CASH-001 | Solo puede existir una sesión de caja abierta simultáneamente. |
| BR-CASH-002 | No se puede confirmar una venta sin caja abierta. |
| BR-CASH-003 | Una sesión cerrada no se reabre ni recibe nuevas operaciones. |
| BR-CASH-004 | Los ingresos y retiros manuales requieren motivo y usuario responsable. |
| BR-CASH-005 | Solo pagos y devoluciones en efectivo afectan el efectivo físico esperado. |
| BR-CASH-006 | El efectivo esperado es monto inicial más ventas e ingresos en efectivo, menos retiros y devoluciones en efectivo. |
| BR-CASH-007 | Tarjeta y transferencia aparecen en el resumen, pero no aumentan el efectivo esperado. |
| BR-CASH-008 | La diferencia es efectivo contado menos efectivo esperado y se conserva aun cuando sea distinta de cero. |
| BR-CASH-009 | El cierre registra usuario y fecha y no puede ocultar diferencias mediante edición posterior. |

## 6. Venta y pago

| ID | Regla |
|---|---|
| BR-SALE-001 | Una venta confirmada contiene al menos una línea. |
| BR-SALE-002 | Toda cantidad vendida es un entero mayor que cero. |
| BR-SALE-003 | Cada venta utiliza un único método de pago. |
| BR-SALE-004 | El monto contable del pago equivale al total exacto de la venta; el efectivo pagadero solo puede diferir por el redondeo legal. |
| BR-SALE-005 | Para efectivo, el monto recibido debe ser igual o superior al total efectivo redondeado; el vuelto es la diferencia entre ambos. |
| BR-SALE-006 | El backend recalcula precios, subtotales y total al confirmar y no confía en totales enviados por el cliente. |
| BR-SALE-007 | Cada línea guarda producto, nombre, código cuando exista, cantidad, precio unitario y costo unitario estimado del momento. |
| BR-SALE-008 | Cambiar posteriormente nombre, código, precio o costo del producto no altera la venta histórica. |
| BR-SALE-009 | Venta, líneas, pago, movimientos de inventario y efecto de caja se confirman en una sola transacción. |
| BR-SALE-010 | Si cualquier parte de la confirmación falla, se revierten todos los efectos. |
| BR-SALE-011 | Cada solicitud de confirmación utiliza una clave de idempotencia; repetirla no crea otra venta. |
| BR-SALE-012 | Antes de confirmar, cancelar significa descartar el carrito y no genera una venta anulada. |
| BR-SALE-013 | Una venta confirmada no se edita directamente. |
| BR-SALE-014 | El folio interno de venta es único y no constituye folio tributario. |
| BR-SALE-015 | El redondeo se aplica una sola vez al total final pagado en efectivo: terminaciones 1 a 5 bajan a la decena inferior, 6 a 9 suben a la decena superior y 0 no cambia. |
| BR-SALE-016 | Tarjeta y transferencia conservan el total exacto; la venta en efectivo guarda total exacto, ajuste, total efectivo, recibido y vuelto. |

## 7. Anulación

| ID | Regla |
|---|---|
| BR-VOID-001 | Solo una venta confirmada puede anularse. |
| BR-VOID-002 | Una venta anulada no puede anularse nuevamente. |
| BR-VOID-003 | La anulación requiere administrador, motivo y método de devolución. |
| BR-VOID-004 | La anulación conserva la venta original y registra fecha, usuario, motivo, monto y método de devolución. |
| BR-VOID-005 | La anulación genera movimientos inversos de inventario dentro de la misma transacción. |
| BR-VOID-006 | Una devolución en efectivo genera una salida de caja y reduce el efectivo esperado. |
| BR-VOID-007 | Una devolución por tarjeta o transferencia se registra internamente, pero el sistema no ejecuta la devolución bancaria. |
| BR-VOID-008 | Si falla un efecto de la anulación, no se confirma ninguno. |
| BR-VOID-009 | Una devolución en efectivo requiere una sesión abierta y afecta esa sesión, aunque la venta pertenezca a una sesión anterior. |
| BR-VOID-010 | Una anulación nunca reabre ni modifica los totales de una sesión histórica cerrada. |

## 8. Lector móvil

| ID | Regla |
|---|---|
| BR-SCAN-001 | Una vinculación es temporal, aleatoria, revocable, de un solo uso y asociada a una sesión POS. |
| BR-SCAN-002 | Un evento contiene únicamente el código leído y un identificador único. |
| BR-SCAN-003 | Un evento expirado, repetido o perteneciente a otra sesión se rechaza. |
| BR-SCAN-004 | El teléfono no determina nombre, precio, stock, total ni descuentos. |
| BR-SCAN-005 | Recibir un código no omite las validaciones normales de búsqueda y venta. |

## 9. Trazabilidad inicial

| Regla crítica | Caso de uso | Componente de dominio | Prueba futura mínima |
|---|---|---|---|
| BR-CASH-001 | UC-CASH-001 | CashSession | Integración con dos aperturas simultáneas |
| BR-SEARCH-001 | UC-CAT-004 / UC-SALE-002 | Product | Código exacto, con ceros iniciales y desconocido |
| BR-SEARCH-002 | UC-CAT-004 / UC-SALE-002 | Product | Nombre parcial, mayúsculas y acentos |
| BR-INV-004 | UC-INV-002 / UC-SALE-004 | InventoryBalance | Caso límite de última unidad |
| BR-INV-006 | UC-SALE-004 | InventoryBalance | Dos ventas simultáneas; solo una confirma |
| BR-SALE-009 | UC-SALE-004 | Sale / Payment / InventoryMovement / CashSession | Falla intermedia sin efectos parciales |
| BR-SALE-011 | UC-SALE-004 | IdempotencyKey / Sale | Doble envío produce una sola venta |
| BR-VOID-002 | UC-SALE-006 | SaleCancellation | Segunda anulación rechazada sin efectos |
| BR-VOID-005 | UC-SALE-006 | InventoryMovement | Reposición exacta y auditable |
| BR-CASH-006 | UC-CASH-003 | CashSession / CashMovement | Cálculo con ventas, ingresos, retiros y devolución |
| BR-SCAN-003 | UC-SCAN-002 | ScannerPairing / ScanEvent | Evento expirado, repetido y sesión incorrecta |

La matriz completa se elaborará en la Entrega 3 cuando existan criterios de aceptación y casos de prueba detallados.

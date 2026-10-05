# 03 — Glosario de dominio

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Aprobado - Entrega 1

**Versión:** 0.1

**Fecha:** 2026-07-31

Este glosario establece el lenguaje común del proyecto. Los documentos, historias, API y código deberán utilizar estos términos de manera consistente.

| Término | Definición |
|---|---|
| Administrador | Usuario interno con permisos sobre usuarios, catálogo, ajustes, anulaciones, caja y reportes. |
| Ajuste de inventario | Corrección manual positiva o negativa del stock. Siempre requiere motivo y autorización administrativa. |
| Anulación | Operación controlada que cambia una venta confirmada a anulada y genera efectos compensatorios auditables. No elimina la venta. |
| Boleta interna | Comprobante o registro interno de venta. No es un documento tributario electrónico. |
| Caja física | Lugar real donde se conserva el efectivo del negocio. En el MVP existe una sola. |
| Carrito | Selección temporal de productos y cantidades antes de confirmar. No constituye todavía una venta. |
| Categoría | Clasificación utilizada para organizar productos. |
| Código de barras | Identificador textual opcional de un producto. Cuando existe debe ser único y conserva posibles ceros iniciales. |
| Código desconocido | Código para el cual no existe un producto activo identificable. No produce creación automática. |
| Confirmación de venta | Caso de uso que registra atómicamente venta, líneas, pago y efectos en inventario y caja. |
| Costo unitario estimado | Precio de compra vigente capturado en una línea al confirmar la venta. Se usa para una estimación histórica de ganancia, no como valorización contable. |
| Diferencia de caja | Efectivo contado al cerrar menos efectivo esperado. Puede ser positiva, negativa o cero. |
| Efectivo contado | Monto físico informado por el usuario durante el cierre. |
| Efectivo esperado | Monto calculado como apertura más ventas e ingresos en efectivo, menos retiros y devoluciones en efectivo. |
| Entrada de mercadería | Movimiento que incrementa el stock por recepción de productos. No exige un módulo avanzado de proveedores. |
| Evento de escaneo | Mensaje único enviado por un lector vinculado que contiene el código leído y un identificador de evento. |
| Folio interno | Identificador legible y único asignado a una venta. No corresponde a folio tributario. |
| Ganancia bruta estimada | Ventas menos costos unitarios estimados capturados al vender. No incorpora impuestos, gastos ni valorización contable avanzada. |
| Idempotencia | Propiedad que impide que la repetición de la misma solicitud confirme dos ventas. |
| Ingreso manual | Movimiento de efectivo que aumenta el efectivo esperado y no corresponde a una venta. Requiere motivo. |
| Inventario | Existencias disponibles y su historial de variaciones. |
| Línea de venta | Producto, cantidad y valores históricos incluidos en una venta. |
| Lector móvil | Interfaz web temporal utilizada desde un teléfono para leer códigos y enviarlos al backend. |
| Método de devolución | Medio mediante el cual se registra la devolución asociada a una anulación: efectivo, tarjeta o transferencia. Una devolución en efectivo afecta la caja abierta al momento de devolver. |
| Método de pago | Medio único usado para pagar una venta: efectivo, tarjeta o transferencia. |
| Movimiento de caja | Registro auditable que explica una variación del efectivo esperado. |
| Movimiento de inventario | Registro inmutable que explica una variación positiva o negativa del stock. |
| Operación compensatoria | Nuevo registro que revierte los efectos de una operación anterior sin borrar el historial. |
| Pago | Registro del monto total y método utilizado en una venta confirmada. |
| Precio aplicado | Precio unitario de venta capturado al confirmar. No cambia si luego se modifica el producto. |
| Producto | Artículo que el minimarket puede mantener en inventario y vender. |
| Producto activo | Producto habilitado para incorporarse a nuevas ventas. |
| Producto inactivo | Producto conservado para historial, pero no disponible para nuevas ventas. |
| Producto sin stock | Producto activo cuyo saldo disponible es cero. Puede visualizarse como no disponible, pero no venderse. |
| Reconciliación de inventario | Verificación de que el saldo actual coincide con el resultado del historial de movimientos. |
| Retiro o gasto | Movimiento manual que reduce el efectivo esperado. Requiere motivo. |
| Saldo de inventario | Cantidad actual disponible de un producto, mantenida transaccionalmente junto con los movimientos. |
| Sesión de caja | Periodo comprendido entre una apertura y un cierre. Solo una puede estar abierta. |
| Sesión de vinculación | Autorización temporal que relaciona un lector móvil con una sesión concreta del POS. |
| Stock | Sinónimo operativo de saldo de inventario disponible. En el MVP se expresa en unidades enteras. |
| Stock mínimo | Umbral configurado para identificar productos con existencias bajas. |
| Transacción | Unidad atómica de base de datos: todos sus cambios se confirman o todos se revierten. |
| Usuario | Persona interna autenticada que realiza operaciones en el sistema. |
| Vendedor | Usuario interno que opera ventas y apertura/cierre, sin facultad para ajustes o anulaciones. |
| Venta | Registro histórico creado al confirmar un carrito válido dentro de una sesión abierta. |
| Venta anulada | Venta conservada en el historial cuyos efectos fueron compensados. No puede anularse nuevamente. |
| Venta confirmada | Venta registrada correctamente con pago y efectos correspondientes. |
| Vuelto | Efectivo recibido menos total de venta. Solo se calcula para pagos en efectivo y nunca puede ser negativo al confirmar. |

## Convenciones de lenguaje

- Usar **anular** para una venta ya confirmada y **cancelar** para descartar un carrito aún no confirmado.
- Usar **saldo de inventario** para el valor actual y **movimiento de inventario** para la evidencia histórica.
- Usar **boleta interna** o **comprobante interno**, nunca “boleta electrónica”, mientras no exista integración tributaria.
- Usar **producto sin stock**, no “producto agotado eliminado”; el producto continúa existiendo.
- El código de barras se representa como texto, nunca como número.

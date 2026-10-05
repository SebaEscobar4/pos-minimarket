# 01 — Visión del producto

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Aprobado - Entrega 1

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Propósito

Construir un sistema web confiable y sencillo para operar un minimarket familiar de una sola sucursal y una sola caja. El producto debe registrar las ventas, controlar el dinero esperado en caja y mantener un inventario trazable, reduciendo registros manuales, pérdidas de información y diferencias difíciles de explicar.

El proyecto también se desarrollará con una disciplina similar a la de un equipo profesional: decisiones documentadas, cambios pequeños, pruebas verificables, seguridad proporcional al riesgo y aprobación técnica antes de implementar.

## 2. Problema que resuelve

El negocio necesita responder con evidencia a preguntas operativas básicas:

- ¿Qué se vendió, cuándo, a qué precio y mediante qué método de pago?
- ¿Cuánto efectivo debería existir al cerrar la jornada?
- ¿Por qué cambió el stock de un producto?
- ¿Qué productos tienen poco stock?
- ¿Qué usuario realizó una venta, ajuste, anulación o movimiento de caja?

Sin un registro central y auditable, estos datos pueden quedar dispersos, perderse o depender de cálculos manuales.

## 3. Usuarios y contexto

El sistema será utilizado por pocos usuarios internos de un negocio familiar:

- **Administrador:** controla configuración, usuarios y operaciones sensibles.
- **Vendedor:** realiza la operación cotidiana de caja y ventas.

El computador será el punto de venta principal. En una fase posterior, un teléfono podrá vincularse temporalmente como lector inalámbrico de códigos de barras.

## 4. Propuesta de valor

El sistema proporcionará:

1. Un registro histórico confiable de ventas y anulaciones.
2. Control de apertura, movimientos, efectivo esperado y cierre de caja.
3. Stock disponible respaldado por un historial de movimientos.
4. Búsqueda rápida de productos por código de barras o nombre.
5. Separación clara entre efectivo, tarjeta y transferencia.
6. Trazabilidad de operaciones críticas por usuario y fecha.
7. Una base técnica mantenible que pueda evolucionar sin sobredimensionar el negocio.

## 5. Principios del producto

- El backend es la autoridad sobre precios, stock, permisos y totales.
- Una venta no puede quedar registrada parcialmente.
- El historial importante no se reescribe para ocultar correcciones.
- Los errores se corrigen mediante operaciones compensatorias auditables.
- La simplicidad operacional tiene prioridad sobre funciones avanzadas.
- El flujo manual de venta debe funcionar antes de incorporar el teléfono lector.
- Las medidas de seguridad y las pruebas acompañan cada módulo desde el inicio.
- Ninguna función se considera terminada solamente porque compila.

## 6. Resultado esperado del MVP operativo

Al finalizar el MVP operativo, un vendedor podrá:

1. Iniciar sesión.
2. Abrir la única caja con un monto inicial.
3. Encontrar un producto ingresando su código o parte de su nombre.
4. Agregar productos con stock al carrito.
5. Confirmar una venta con un único método de pago.
6. Obtener el vuelto cuando corresponda.
7. Consultar la venta posteriormente.
8. Cerrar la caja comparando efectivo esperado y contado.

El administrador podrá además gestionar productos, registrar entradas y ajustes, anular ventas, gestionar usuarios y consultar reportes.

## 7. Indicadores de éxito

- Cero ventas confirmadas sin sus efectos correspondientes en inventario y pago.
- Cero existencias negativas.
- Cero diferencias de caja atribuibles a errores de cálculo del sistema.
- Cien por ciento de operaciones críticas con actor y fecha identificables.
- Cien por ciento de flujos críticos de regresión aprobados antes de liberar.
- El usuario responsable puede explicar el comportamiento de cada módulo aprobado.

## 8. Restricciones aprobadas

- Una sucursal, una caja y una sesión abierta simultáneamente.
- Moneda CLP y cantidades enteras en el MVP.
- Un método de pago por venta.
- Operación online; no se implementará sincronización offline inicialmente.
- Durante desarrollo, staging y el primer despliegue solo se utilizarán recursos locales o planes gratuitos.
- Ningún servicio, recurso o ampliación que pueda generar cobros se habilitará sin aprobación explícita del responsable técnico.
- Sin cuentas de clientes, fiado, impuestos, descuentos ni integración con el SII.
- Sin microservicios ni infraestructura distribuida compleja.

## 9. Evolución prevista

Después de estabilizar el MVP operativo podrán evaluarse:

- Teléfono como lector inalámbrico.
- PWA y optimizaciones para instalación.
- Pagos mixtos.
- Productos vendidos por peso o fracción.
- Integración tributaria.
- Funciones avanzadas de proveedores, costos e informes.

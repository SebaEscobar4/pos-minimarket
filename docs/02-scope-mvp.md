# 02 — Alcance del MVP

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Aprobado - Entrega 1

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Objetivo del alcance

Delimitar una primera versión utilizable y comprobable, evitando incorporar necesidades hipotéticas que aumenten el riesgo o retrasen la operación principal.

## 2. MVP operativo incluido

### 2.1 Identidad y acceso

- Inicio y cierre de sesión.
- Usuarios internos creados por un administrador.
- Roles `ADMINISTRADOR` y `VENDEDOR`.
- Autorización aplicada en backend y representada en frontend.
- Activación y desactivación de usuarios.

No existirá registro público ni autoservicio de recuperación de contraseña en la primera versión.

### 2.2 Productos y categorías

- Crear, editar, consultar, activar y desactivar productos.
- Crear y editar categorías.
- Código de barras opcional y único cuando esté informado.
- Nombre, categoría, precio de compra, precio de venta y stock mínimo.
- Precios expresados en CLP.
- Búsqueda administrativa por código exacto o coincidencia parcial de nombre.
- Filtro administrativo para incluir productos inactivos.

### 2.3 Búsqueda de productos en el POS

La pantalla de venta tendrá un único campo de búsqueda que aceptará código o nombre:

- Un código de barras se tratará como texto y se buscará por coincidencia exacta.
- El nombre permitirá coincidencia parcial sin distinguir mayúsculas, minúsculas ni acentos.
- Una coincidencia exacta de código podrá seleccionar o agregar directamente el producto.
- Una búsqueda por nombre mostrará una lista para que el vendedor elija.
- Los resultados mostrarán al menos nombre, código cuando exista, precio de venta y stock disponible.
- Los productos inactivos no aparecerán en el POS.
- Los productos sin stock podrán mostrarse como no disponibles, pero no agregarse.
- Un código desconocido informará el problema y no creará productos ni líneas de venta.

El backend resolverá siempre el producto y devolverá sus datos vigentes. El cliente o el teléfono nunca serán fuente confiable de nombre, precio o stock.

### 2.4 Inventario

- Un saldo actual por producto.
- Historial inmutable de movimientos.
- Entrada de mercadería.
- Salida por venta.
- Ajustes positivos y negativos con motivo obligatorio.
- Registro de productos dañados y vencidos.
- Reversión por anulación.
- Consulta de stock y movimientos.
- Prevención de stock negativo, incluso con ventas simultáneas.

El saldo y el movimiento se actualizarán dentro de la misma transacción.

### 2.5 Caja

- Una sola sesión de caja abierta simultáneamente.
- Apertura con monto inicial.
- Ingresos manuales.
- Retiros o gastos.
- Ventas y devoluciones en efectivo reflejadas en el efectivo esperado.
- Totales separados para efectivo, tarjeta y transferencia.
- Cierre con monto contado y cálculo de diferencia.
- Historial de sesiones y movimientos.
- Una sesión cerrada no se reabre ni acepta operaciones nuevas.

### 2.6 Venta manual

- Carrito temporal no persistido antes de confirmar.
- Agregar productos mediante búsqueda por nombre o código.
- Modificar cantidades enteras positivas.
- Eliminar líneas.
- Calcular subtotales, total y vuelto.
- Un único método de pago: efectivo, tarjeta o transferencia.
- Confirmación protegida contra envíos duplicados.
- Confirmación atómica de venta, líneas, pago, caja e inventario.
- Precio de venta y costo estimado guardados en cada línea.
- Cancelación previa a la confirmación mediante descarte del carrito.

### 2.7 Historial y anulación

- Ventas por día o rango de fechas.
- Detalle completo de una venta.
- Estados `CONFIRMADA` y `ANULADA`.
- Anulación realizada por un administrador, con motivo obligatorio.
- Reposición de stock mediante movimientos inversos.
- Registro del método y monto de devolución.
- Las devoluciones en efectivo se registran en la sesión de caja que esté abierta al momento de devolver el dinero y disminuyen su efectivo esperado.
- Una devolución en efectivo no reabre ni modifica una sesión histórica ya cerrada.
- Las devoluciones electrónicas se registran, pero no se ejecutan contra bancos o adquirentes.

### 2.8 Reportes básicos

- Ventas y totales por método de pago.
- Productos más vendidos.
- Productos con stock bajo.
- Movimientos de inventario.
- Aperturas, cierres y diferencias de caja.
- Ganancia bruta estimada usando el costo guardado al vender.

Los reportes iniciales utilizarán tablas, filtros y totales; no requieren gráficos complejos.

### 2.9 Operación y recuperación

- Entornos separados para local, CI, staging y producción.
- HTTPS en ambientes públicos.
- Copias de seguridad.
- Procedimiento de restauración ensayado antes de operar con datos importantes.
- Zona horaria comercial `America/Santiago`.

### 2.10 Restricción de costos de infraestructura

- Desarrollo local mediante herramientas gratuitas y contenedores locales.
- CI dentro de cuotas gratuitas disponibles.
- Staging y primer despliegue exclusivamente en planes gratuitos.
- Prohibido crear recursos con cobro por uso, escalado automático pagado o suscripciones pagadas sin aprobación explícita.
- Si un proveedor exige una tarjeta incluso para su plan gratuito, deberá solicitarse aprobación y demostrar que existe un límite de gasto efectivo; de lo contrario se escogerá otra alternativa.
- Las cuotas, suspensión por inactividad, caducidad, límites de base de datos y ausencia de SLA deben documentarse para el proveedor seleccionado.
- Si ninguna alternativa gratuita cumple seguridad, integridad o recuperación mínimas, el sistema permanecerá local o en staging y no se declarará listo para operación real.

## 3. Extensión posterior del MVP

El lector móvil se implementará únicamente después de estabilizar la venta manual:

- QR de vinculación generado por el POS.
- Token temporal, aleatorio, revocable y de un solo uso.
- Interfaz web móvil con acceso a cámara.
- Envío exclusivo del código leído y un identificador de evento.
- Validación del backend.
- Entrega del evento únicamente a la sesión POS vinculada.
- Protección contra eventos repetidos, expirados o enviados a otra sesión.

La extensión no cambia la autoridad del backend ni las reglas de venta.

## 4. Fuera de alcance

- Múltiples sucursales o cajas.
- Más de una sesión de caja abierta.
- Cuentas de clientes, fiado, abonos o deudas.
- Aplicación móvil nativa.
- Funcionamiento y sincronización offline.
- Productos por peso, litros o cantidades fraccionarias.
- Pagos mixtos.
- Descuentos, cupones y promociones.
- Impuestos, facturación electrónica e integración con el SII.
- Integración bancaria o devolución electrónica automática.
- Contabilidad completa.
- Compras y proveedores avanzados.
- FIFO, promedio ponderado u otra valorización contable de inventario.
- Microservicios, Kafka, Kubernetes, CQRS completo o event sourcing.
- Analítica avanzada y gráficos complejos.
- Infraestructura o servicios pagados mientras no exista aprobación expresa.

## 5. Supuestos aprobados

- El negocio dispone de conexión suficiente para operar el sistema online.
- Los precios y cantidades del MVP se expresan en pesos y unidades enteras.
- Un vendedor puede abrir y cerrar caja.
- Solo un administrador puede ajustar inventario o anular ventas.
- El cambio de nombre o precio de un producto no altera la representación histórica guardada en una venta.
- Las correcciones de registros históricos se hacen mediante operaciones compensatorias.

## 6. Criterio de aceptación del MVP operativo

El MVP será candidato a uso real cuando el flujo completo —inicio de sesión, apertura, búsqueda, venta, inventario, historial, anulación y cierre— funcione en staging, pase la regresión crítica, no tenga defectos críticos o altos abiertos y se haya demostrado una restauración de respaldo.

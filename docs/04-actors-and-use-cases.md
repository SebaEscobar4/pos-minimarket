# 04 — Actores y casos de uso

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Aprobado - Entrega 1

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Actores

### Administrador

Responsable de configuración y operaciones sensibles. Puede realizar todas las operaciones del vendedor y además gestionar usuarios, catálogo, ajustes, anulaciones y reportes.

### Vendedor

Responsable de la operación cotidiana. Puede iniciar sesión, abrir y cerrar caja, buscar productos, registrar ventas, consultar la sesión activa y utilizar el lector vinculado.

### Lector móvil vinculado

Dispositivo temporal, no un usuario humano ni una fuente confiable de datos comerciales. Solo puede enviar códigos y sus identificadores de evento mientras su vinculación sea válida.

### Reloj del sistema

Actor técnico que aporta instantes consistentes para expiraciones, auditoría, apertura, cierre y reportes.

## 2. Matriz de permisos inicial

| Caso de uso | Administrador | Vendedor | Lector vinculado |
|---|:---:|:---:|:---:|
| Iniciar sesión | Sí | Sí | No |
| Gestionar usuarios | Sí | No | No |
| Gestionar productos y categorías | Sí | No | No |
| Buscar productos en administración | Sí | No | No |
| Consultar productos en POS | Sí | Sí | No |
| Registrar entrada o ajuste | Sí | No | No |
| Consultar inventario | Sí | Consulta necesaria para venta | No |
| Abrir y cerrar caja | Sí | Sí | No |
| Registrar ingreso o retiro manual | Sí | No inicialmente | No |
| Registrar venta | Sí | Sí | No |
| Anular venta | Sí | No | No |
| Consultar reportes | Sí | Solo sesión activa y ventas propias si se aprueba | No |
| Crear vinculación del lector | Sí | Sí | No |
| Enviar código leído | No | No | Sí |

La visibilidad exacta de reportes para el vendedor se confirmará en una entrega posterior; no bloquea el modelo central.

## 3. Casos de uso principales

### UC-ID-001 — Iniciar sesión

**Actor:** Administrador o vendedor.

**Resultado:** Se crea una sesión autenticada con los permisos del usuario activo.

**Excepciones:** Credenciales inválidas, usuario inactivo o demasiados intentos.

### UC-CAT-001 — Crear producto

**Actor:** Administrador.

**Precondiciones:** Categoría válida; código no repetido cuando se informa.

**Resultado:** Producto creado inicialmente activo, sin alterar stock de forma directa.

El stock inicial, si existe, se registrará mediante un movimiento de entrada o ajuste inicial explícito.

### UC-CAT-002 — Editar producto

**Actor:** Administrador.

**Resultado:** Se actualizan datos vigentes sin modificar ventas históricas ni movimientos previos.

### UC-CAT-003 — Activar o desactivar producto

**Actor:** Administrador.

**Resultado:** El producto conserva su historial. Un producto inactivo deja de estar disponible para nuevas ventas.

### UC-CAT-004 — Buscar productos en administración

**Actor:** Administrador.

**Entrada:** Código exacto o texto parcial de nombre; filtro opcional de estado.

**Resultado:** Lista paginada de coincidencias con información administrativa.

### UC-INV-001 — Registrar entrada de mercadería

**Actor:** Administrador.

**Resultado:** Se crea un movimiento positivo y se actualiza el saldo en la misma transacción.

### UC-INV-002 — Ajustar inventario

**Actor:** Administrador.

**Precondiciones:** Motivo obligatorio y cantidad válida.

**Resultado:** Movimiento auditable y nuevo saldo nunca negativo.

### UC-CASH-001 — Abrir caja

**Actor:** Administrador o vendedor.

**Precondiciones:** No existe otra sesión abierta; monto inicial válido.

**Resultado:** Nueva sesión abierta vinculada al usuario que la inicia.

### UC-CASH-002 — Registrar movimiento manual de caja

**Actor:** Administrador.

**Entrada:** Tipo, monto y motivo.

**Resultado:** Ingreso o retiro auditable que modifica el efectivo esperado.

### UC-SALE-001 — Iniciar venta

**Actor:** Administrador o vendedor.

**Precondición:** Existe una sesión de caja abierta.

**Resultado:** Se presenta un carrito temporal vacío; todavía no existe una venta persistida.

### UC-SALE-002 — Buscar y agregar producto

**Actor:** Administrador o vendedor.

**Entrada:** Código exacto o parte del nombre.

**Flujo principal por código:**

1. El usuario introduce o escanea un código.
2. El backend busca una coincidencia exacta.
3. Valida que el producto esté activo y tenga stock.
4. Devuelve los datos vigentes.
5. El producto se agrega al carrito o incrementa su cantidad.

**Flujo principal por nombre:**

1. El usuario introduce parte del nombre.
2. El backend busca sin distinguir mayúsculas ni acentos.
3. La interfaz presenta los productos activos coincidentes.
4. El usuario selecciona uno disponible.
5. El producto se agrega al carrito.

**Alternativas:** Código inexistente, producto inactivo, producto sin stock o varias coincidencias por nombre. Ninguna alternativa crea productos automáticamente.

### UC-SALE-003 — Modificar carrito

**Actor:** Administrador o vendedor.

**Resultado:** Se cambia una cantidad entera positiva o se elimina una línea. Los cálculos visuales son informativos y se recalculan definitivamente en backend.

### UC-SALE-004 — Confirmar venta

**Actor:** Administrador o vendedor.

**Precondiciones:** Caja abierta, carrito no vacío, productos activos, stock suficiente, método único de pago y efectivo recibido suficiente cuando corresponda.

**Resultado exitoso:** En una única transacción se crea la venta y sus líneas, se registra el pago, se descuenta inventario, se actualiza caja cuando corresponde y se consume la clave de idempotencia.

**Resultado fallido:** No queda ningún efecto parcial.

### UC-SALE-005 — Consultar historial de ventas

**Actor:** Administrador; alcance reducido para vendedor por confirmar.

**Resultado:** Ventas filtradas y detalle histórico sin depender de los datos actuales del producto.

### UC-SALE-006 — Anular venta

**Actor:** Administrador.

**Precondiciones:** Venta confirmada y no anulada; motivo y método de devolución informados. Si la devolución será en efectivo, debe existir una caja abierta en ese momento.

**Resultado:** Estado anulado, registro de anulación, movimientos inversos de inventario y efecto sobre la caja actualmente abierta si la devolución es en efectivo. Una caja histórica cerrada nunca se modifica.

### UC-CASH-003 — Cerrar caja

**Actor:** Administrador o vendedor.

**Entrada:** Efectivo contado.

**Resultado:** Se calculan totales y diferencia, se registra el cierre y la sesión deja de aceptar operaciones.

### UC-SCAN-001 — Vincular lector móvil

**Actor:** Administrador o vendedor.

**Precondiciones:** Sesión autenticada y caja abierta.

**Resultado:** QR con autorización temporal de un solo uso para una sesión POS concreta.

### UC-SCAN-002 — Enviar código escaneado

**Actor:** Lector móvil vinculado.

**Entrada:** Código y un identificador único de evento.

**Resultado:** El backend valida la vinculación y entrega el evento solo al POS correspondiente. El POS continúa el flujo de `UC-SALE-002`.

## 4. Relaciones críticas

- `UC-SALE-004` depende de caja, catálogo, inventario, pagos e idempotencia.
- `UC-SALE-006` compensa efectos producidos por `UC-SALE-004`.
- `UC-SCAN-002` no vende ni agrega datos comerciales por sí mismo; solo inicia una búsqueda exacta de código.
- `UC-CASH-003` resume pagos y movimientos, pero solo el efectivo afecta la comparación física.

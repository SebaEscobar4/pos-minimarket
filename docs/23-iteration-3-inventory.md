# 23 - Iteración 3: inventario

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** US-030 a US-033 implementadas y validadas localmente

**Fecha:** 2026-08-04

## Alcance implementado

- US-030: entradas de inventario con cantidad positiva y motivo o referencia.
- US-031: ajustes positivos y negativos exclusivos de ADMIN; todo ajuste exige motivo y ninguno puede dejar saldo negativo.
- US-032: consulta de saldo y libro paginado. Cada movimiento conserva producto, tipo, cantidad, delta, saldo anterior/resultante, fecha y actor. Los movimientos confirmados no admiten actualización ni eliminación.
- US-033: la modificación bloquea el saldo mediante `SELECT ... FOR UPDATE` bajo `READ COMMITTED`. Saldo y movimiento se confirman en una sola transacción.
- Flyway V5 crea `inventory_movement`, sus restricciones e índices, limita el saldo máximo y agrega un trigger que rechaza `UPDATE` y `DELETE` del historial.
- El frontend administrativo permite seleccionar productos, registrar entradas o ajustes y revisar saldo e historial. Un vendedor no recibe acceso a estas operaciones.
- El MVP presenta Inventario como apartado principal para ADMIN. También permite escribir el conteo físico: el frontend calcula la diferencia y el backend conserva el ajuste positivo o negativo como movimiento auditable. Si no existen productos, dirige al apartado Productos.

Los límites aplicados son 1 a 1.000.000 unidades por movimiento, saldo máximo 100.000.000, motivo de hasta 500 caracteres, referencia de hasta 100 y página de hasta 100 movimientos.

## API incorporada

| Método | Ruta | Resultado |
|---|---|---|
| `POST` | `/api/v1/admin/inventory/entries` | Registra entrada y actualiza saldo atómicamente |
| `POST` | `/api/v1/admin/inventory/adjustments` | Registra ajuste positivo o negativo con motivo |
| `GET` | `/api/v1/admin/inventory/products/{productId}` | Devuelve saldo, versión e historial paginado |

## Evidencia ejecutable

- Pruebas unitarias cubren límites, evidencia obligatoria, entradas, ambos ajustes, saldo negativo, saldo máximo, recurso ausente y conflicto de versión.
- `ApplicationContextIT` aplica cinco migraciones sobre PostgreSQL limpio y verifica roles, CSRF, saldo reconciliable con la suma de deltas, rechazo de operaciones inválidas e inmutabilidad en base de datos.
- Una prueba con dos transacciones concurrentes intenta descontar la última unidad: solo una confirma, la otra recibe conflicto, el saldo termina en cero y existe un único movimiento de salida.
- Las pruebas frontend cubren contratos HTTP, visibilidad por rol, validación de evidencia, entradas, ajustes, consulta de saldo e historial sin controles de edición o borrado.

Esta iteración no cambia decisiones aceptadas ni resuelve OQ-01; la duración e inactividad de sesión permanece diferida por decisión del usuario.

# 24 - Iteración 4: caja

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** US-040 a US-043 implementadas y validadas localmente

**Fecha:** 2026-08-04

## Alcance implementado

- US-040: ADMIN y SELLER pueden consultar y abrir la única sesión de caja.
- La apertura conserva monto inicial CLP, usuario, fecha y estado `OPEN`.
- El dinero de caja usa `BigDecimal` de escala cero, `numeric(19,0)` y JSON entero dentro del rango seguro.
- Flyway V6 crea `cash_session`, las restricciones monetarias y un índice único parcial que permite como máximo una fila `OPEN`.
- La aplicación comprueba primero si existe una caja abierta para responder claramente; PostgreSQL resuelve la carrera final.
- El frontend muestra la sesión vigente o el formulario de apertura a ambos roles operativos.
- US-041: ADMIN registra ingresos o retiros manuales con monto positivo, categoría cerrada, motivo y actor. Una caja cerrada rechaza operaciones.
- US-042: el resumen separa apertura, ventas en efectivo, ingresos, retiros, devoluciones, tarjeta y transferencia; solo los conceptos efectivos forman el efectivo esperado.
- US-043: ADMIN y SELLER cierran con efectivo contado. Se guardan esperado, contado, diferencia, usuario y fecha como fotografía histórica inmutable.
- Flyway V7 crea el libro `cash_movement`, aplica las categorías aprobadas y protege movimientos y sesiones cerradas contra actualización o borrado.

## API incorporada

| Método | Ruta | Resultado |
|---|---|---|
| `POST` | `/api/v1/cash/sessions` | Abre una sesión con monto inicial entero |
| `GET` | `/api/v1/cash/sessions/current` | Devuelve la sesión abierta o `null` |
| `POST` | `/api/v1/admin/cash/movements` | Registra ingreso o retiro manual administrativo |
| `GET` | `/api/v1/cash/sessions/current/summary` | Devuelve totales, efectivo esperado y movimientos |
| `POST` | `/api/v1/cash/sessions/current/close` | Cierra con efectivo contado y conserva diferencia |

## Evidencia ejecutable

- Pruebas unitarias cubren dinero CLP exacto, límites, usuario obligatorio, apertura, conflicto y consulta.
- `ApplicationContextIT` aplica siete migraciones sobre PostgreSQL limpio y verifica autenticación, CSRF, roles, categorías, montos, resumen, cierre e inmutabilidad.
- La prueba concurrente sincroniza dos aperturas en transacciones independientes; solo una confirma y la base conserva exactamente una fila `OPEN`.
- Las pruebas verifican la fórmula de efectivo esperado, diferencia negativa conservada, movimientos rechazados tras cerrar y protección directa ante `UPDATE`/`DELETE`.

## Decisión resuelta OQ-03

El usuario aprobó el 2026-08-04:

- ingresos: refuerzo de efectivo y otro ingreso;
- retiros: pago a proveedor, gasto operativo, retiro preventivo y otro retiro;
- motivo obligatorio para todas las categorías.

El backend usa códigos internos en inglés y el frontend presenta estas etiquetas en español. PostgreSQL limita las combinaciones válidas por dirección.

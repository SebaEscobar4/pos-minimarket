# 12 - Plan de pruebas unitarias

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Propuesta para aprobación - Entrega 3

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Objetivo

Validar reglas de negocio de forma rápida, aislada y legible, sin levantar Spring ni conectarse a PostgreSQL cuando la regla puede demostrarse con objetos en memoria.

## 2. Convención

- JUnit 5 y AssertJ.
- Estructura Given/When/Then o Arrange/Act/Assert consistente.
- Nombres que describen escenario y resultado.
- Una razón clara de fallo por prueba.
- `@ParameterizedTest` para conjuntos de límites y equivalencias.
- Reloj, generadores y dependencias externas inyectados.
- Mockito solo para colaboradores externos al objeto probado.

## 3. Qué no es una prueba unitaria

- Levantar el contexto completo de Spring.
- Probar getters, setters o anotaciones sin comportamiento.
- Simular repositorios para afirmar que PostgreSQL bloquea o revierte.
- Verificar llamadas internas que no cambian el resultado observable.
- Duplicar exactamente un E2E sin aislar una regla.

## 4. Catálogo y valores compartidos

| ID base | Componente | Casos mínimos |
|---|---|---|
| UT-MONEY | `Money` | Suma, resta, multiplicación, comparación, escala inválida, negativo y límite |
| UT-QTY | `Quantity` | Positivo, cero permitido como saldo, negativo y overflow |
| UT-BARCODE | `Barcode` | Ceros iniciales, trim externo, vacío, longitud y caracteres permitidos |
| UT-SEARCH | Normalización | Tildes, mayúsculas, `ñ`, espacios y coincidencia parcial |
| UT-PRODUCT | `Product` | Creación, precios, activación, desactivación e invariantes |
| UT-CATEGORY | `Category` | Nombre válido, estado y normalización |

Pruebas críticas:

- Cambiar precio o nombre actual no modifica una fotografía de línea ya creada.
- Un producto inactivo no puede convertirse en línea de una nueva venta.
- Código tratado como texto conserva `001234`.
- `Cafe` encuentra `Café`, sin alterar el nombre visible.

## 5. Inventario

| ID base | Regla | Escenarios |
|---|---|---|
| UT-INV-001 | Aplicar movimiento | Entrada, salida, ajuste y reversión |
| UT-INV-002 | Saldo no negativo | Cero, última unidad y cantidad superior |
| UT-INV-003 | Movimiento consistente | Anterior + delta = resultante |
| UT-INV-004 | Motivo obligatorio | Ajustes, dañado y vencido |
| UT-INV-005 | Inmutabilidad | Movimiento confirmado no se edita |
| UT-INV-006 | Reposición | Cantidad exacta de líneas anuladas |

Cada rechazo verifica que saldo y colección de movimientos permanecen sin cambios.

## 6. Caja

| ID base | Regla | Escenarios |
|---|---|---|
| UT-CASH-001 | Abrir sesión | Monto inicial válido e inválido |
| UT-CASH-002 | Efectivo esperado | Apertura, ventas, ingresos, retiros y devoluciones |
| UT-CASH-003 | Métodos no efectivos | Tarjeta/transferencia no aumentan efectivo |
| UT-CASH-004 | Diferencia | Faltante, sobrante y cuadratura exacta |
| UT-CASH-005 | Sesión cerrada | Rechaza todo nuevo movimiento |
| UT-CASH-006 | Movimiento manual | Motivo y monto válidos |
| UT-CASH-007 | Devolución | Afecta caja abierta indicada, no caja histórica |

Se utilizarán tablas parametrizadas para combinaciones de métodos y movimientos.

## 7. Ventas y pagos

| ID base | Regla | Escenarios |
|---|---|---|
| UT-SALE-001 | Carrito válido | Vacío, una línea y varias líneas |
| UT-SALE-002 | Línea | Cantidad positiva, precio aplicado y subtotal |
| UT-SALE-003 | Total | Suma exacta y límites |
| UT-SALE-004 | Método único | Efectivo, tarjeta, transferencia y pago mixto rechazado |
| UT-SALE-005 | Vuelto | Exacto, sobrante e insuficiente |
| UT-SALE-006 | Fotografía histórica | Nombre, código, precio y costo capturados |
| UT-SALE-007 | Estado | Confirmada no editable; anulada no anulable otra vez |
| UT-SALE-008 | Hash idempotente | Mismo contenido estable; contenido distinto cambia hash |
| UT-SALE-009 | Folio | Formato válido sin significado tributario |

La coordinación transaccional y la restricción única de idempotencia se prueban en integración, no mediante mocks unitarios.

## 8. Anulación

| ID base | Regla | Escenarios |
|---|---|---|
| UT-VOID-001 | Elegibilidad | Confirmada, anulada y estado inválido |
| UT-VOID-002 | Motivo | Vacío, válido y longitud límite |
| UT-VOID-003 | Devolución | Monto total y método permitido |
| UT-VOID-004 | Plan compensatorio | Una reversión por cada línea |
| UT-VOID-005 | Efecto de caja | Solo devolución en efectivo requiere caja abierta |

## 9. Identidad y permisos

| ID base | Regla | Escenarios |
|---|---|---|
| UT-ID-001 | Estado de usuario | Activo e inactivo |
| UT-ID-002 | Matriz de rol | Administrador, vendedor y operación |
| UT-ID-003 | Cambio sensible | Desactivación exige invalidar sesiones mediante evento/comando |

El algoritmo real de contraseña, cookies, CSRF y filtros se comprueba en integración.

## 10. Lector

| ID base | Regla | Escenarios |
|---|---|---|
| UT-SCAN-001 | Vinculación | Pendiente, consumida, expirada y revocada |
| UT-SCAN-002 | Expiración | Antes, exactamente en y después del límite |
| UT-SCAN-003 | Alcance | POS correcto e incorrecto |
| UT-SCAN-004 | Evento | Código válido, ID repetido y longitud inválida |
| UT-SCAN-005 | Cierre de caja | Revoca sesión lectora |

Se inyectará `Clock`; las pruebas no usarán esperas reales.

## 11. Frontend

| ID base | Componente | Escenarios |
|---|---|---|
| FE-UT-CART-001 | Reducer de carrito | Agregar, incrementar, reducir, eliminar y limpiar |
| FE-UT-CART-002 | Totales visuales | Varias líneas y montos grandes seguros |
| FE-UT-SEARCH-001 | Campo unificado | Código exacto, nombre y resultados múltiples |
| FE-UT-SALE-001 | Confirmación | Loading, éxito, error, conflicto y doble clic |
| FE-UT-CASH-001 | Cierre | Esperado, contado y diferencia mostrada |
| FE-UT-AUTH-001 | Permisos visuales | Acciones ocultas según rol |
| FE-UT-ERROR-001 | Errores | Mensaje seguro, campo inválido y reintento |
| FE-UT-A11Y-001 | Accesibilidad | Etiquetas, foco y navegación básica |

Se probará lo que el usuario observa, no estados internos de React.

## 12. Builders y fixtures

Cada módulo tendrá builders con valores válidos por defecto y modificaciones explícitas. Deben evitar constructores gigantes y datos mágicos. Los fixtures compartidos se limitarán para impedir acoplamiento entre pruebas.

## 13. Política de mocks

Se puede simular:

- Reloj.
- Generador de identificadores.
- Interfaz pública de otro módulo.
- Publicador de auditoría o canal externo.

No se simulará para “demostrar”:

- Restricciones SQL.
- Bloqueos y concurrencia.
- Rollback.
- Configuración de Spring Security.
- Serialización HTTP.

## 14. Mutación

PIT se aplicará primero a `Money`, inventario, caja y cálculos de venta. Objetivo inicial de score: 65 %, luego 75 %. Mutantes equivalentes o excluidos se documentan; no se baja el umbral para hacer pasar el pipeline.

La mutación se ejecutará nightly o bajo demanda inicialmente para cuidar tiempo y cuota gratuita de CI.

## 15. Criterio de aceptación

Una regla unitaria crítica queda cubierta cuando tiene caso positivo, negativo, límite y comprobación de que un rechazo no produjo cambios. La revisión confirma además que la prueba fallaría si la regla se elimina o invierte.


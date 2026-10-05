# Matriz de trazabilidad - reglas, aceptación y pruebas

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Propuesta para aprobación - Entrega 3

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Uso de la matriz

Cada regla aprobada de la Entrega 1 tiene un criterio de aceptación identificable, una prueba primaria planificada y una suite de regresión. Los identificadores de prueba son contratos de planificación; al implementarlos se añadirá la ruta exacta de la clase o archivo ejecutable sin cambiar el vínculo con la regla.

## 2. Reglas generales

| Regla | Criterio de aceptación | Prueba primaria planificada | Regresión |
|---|---|---|---|
| BR-GEN-001 | AC-GEN-001: rechazar cantidades fraccionarias | UT-QTY-001; API-VAL-001 | REG-P0-004 |
| BR-GEN-002 | AC-GEN-002: todo cálculo CLP conserva exactitud | UT-MONEY-001; IT-SALE-001 | REG-P0-004 |
| BR-GEN-003 | AC-GEN-003: rangos se interpretan en America/Santiago | UT-TIME-001; API-REPORT-001 | REG-P1-011 |
| BR-GEN-004 | AC-GEN-004: acción crítica registra actor, instante y referencia | `AuditQueryServiceTest`; `ApplicationContextIT`; `AuditWorkspace.test.tsx` | REG-P1-020 |
| BR-GEN-005 | AC-GEN-005: historial se desactiva o compensa, nunca desaparece | IT-HISTORY-001 | REG-P1-010 |
| BR-GEN-006 | AC-GEN-006: backend rechaza entrada manipulada por cliente | API-VAL-002 | REG-P0-010 |

## 3. Identidad y permisos

| Regla | Criterio de aceptación | Prueba primaria planificada | Regresión |
|---|---|---|---|
| BR-ID-001 | AC-ID-001: usuario anónimo o inactivo no opera | IT-AUTH-001 | REG-P0-001 |
| BR-ID-002 | AC-ID-002: vendedor abre/cierra caja y vende | IT-RBAC-001 | REG-P0-002 |
| BR-ID-003 | AC-ID-003: vendedor no gestiona usuarios, ajusta ni anula | IT-RBAC-002 | REG-P0-002 |
| BR-ID-004 | AC-ID-004: API deniega acción aunque la UI sea manipulada | API-AUTHZ-001 | REG-P0-002 |

## 4. Catálogo y búsqueda

| Regla | Criterio de aceptación | Prueba primaria planificada | Regresión |
|---|---|---|---|
| BR-CAT-001 | AC-CAT-001: código vacío es válido y código informado no se duplica | IT-DB-CAT-001; API-CAT-001 | REG-P1-001 |
| BR-CAT-002 | AC-CAT-002: 00123 se guarda y consulta como 00123 | UT-CODE-001; IT-CAT-001 | REG-P1-001 |
| BR-CAT-003 | AC-CAT-003: producto inactivo no entra a venta | API-SALE-001 | REG-P0-003 |
| BR-CAT-004 | AC-CAT-004: editar/desactivar no cambia línea histórica | IT-HISTORY-002 | REG-P1-010 |
| BR-CAT-005 | AC-CAT-005: edición de producto no acepta ni cambia stock | API-CAT-002 | REG-P1-002 |
| BR-CAT-006 | AC-CAT-006: stock inicial crea movimiento explícito | IT-INV-001 | REG-P1-002 |
| BR-SEARCH-001 | AC-SEARCH-001: código coincide exacto tras trim externo | UT-SEARCH-001; IT-SEARCH-001 | REG-P0-003 |
| BR-SEARCH-002 | AC-SEARCH-002: nombre parcial ignora caso y acentos | UT-SEARCH-002; IT-SEARCH-002 | REG-P0-003 |
| BR-SEARCH-003 | AC-SEARCH-003: POS excluye inactivos; admin los incluye con filtro | API-SEARCH-001 | REG-P0-003 |
| BR-SEARCH-004 | AC-SEARCH-004: resultado trae precio y saldo vigentes del servidor | API-SEARCH-002 | REG-P0-003 |
| BR-SEARCH-005 | AC-SEARCH-005: sin stock se muestra no disponible y no se agrega | FE-UT-CART-001; API-SALE-002 | REG-P0-003 |
| BR-SEARCH-006 | AC-SEARCH-006: código desconocido no crea producto ni línea | IT-SEARCH-003 | REG-P0-003 |
| BR-SEARCH-007 | AC-SEARCH-007: dato móvil se resuelve nuevamente en backend | IT-SCAN-001 | REG-P1-030 |

## 5. Inventario

| Regla | Criterio de aceptación | Prueba primaria planificada | Regresión |
|---|---|---|---|
| BR-INV-001 | AC-INV-001: ningún endpoint edita saldo sin movimiento | API-INV-001 | REG-P0-004 |
| BR-INV-002 | AC-INV-002: movimiento conserva todos los campos exigidos | IT-INV-002 | REG-P0-004 |
| BR-INV-003 | AC-INV-003: falla del movimiento revierte también el saldo | IT-TX-INV-001 | REG-P0-004 |
| BR-INV-004 | AC-INV-004: toda operación que dejaría saldo negativo se rechaza | UT-STOCK-001; IT-DB-INV-001 | REG-P0-004 |
| BR-INV-005 | AC-INV-005: venta sobre saldo vigente insuficiente falla sin efectos | IT-SALE-STOCK-001 | REG-P0-004 |
| BR-INV-006 | AC-INV-006: dos ventas por última unidad dejan una confirmada | IT-CONC-INV-001 | REG-P0-005 |
| BR-INV-007 | AC-INV-007: ajuste sin motivo se rechaza | UT-INV-001; API-INV-002 | REG-P1-004 |
| BR-INV-008 | AC-INV-008: movimiento confirmado no se edita ni elimina | API-INV-003; IT-HISTORY-003 | REG-P1-004 |
| BR-INV-009 | AC-INV-009: anulación repone exactamente cada cantidad | IT-VOID-INV-001 | REG-P0-008 |

## 6. Caja

| Regla | Criterio de aceptación | Prueba primaria planificada | Regresión |
|---|---|---|---|
| BR-CASH-001 | AC-CASH-001: aperturas concurrentes dejan una sola caja abierta | IT-CONC-CASH-001; IT-DB-CASH-001 | REG-P0-006 |
| BR-CASH-002 | AC-CASH-002: confirmar sin caja abierta se rechaza sin efectos | IT-SALE-CASH-001 | REG-P0-004 |
| BR-CASH-003 | AC-CASH-003: caja cerrada no reabre ni recibe movimientos | IT-CASH-001 | REG-P0-006 |
| BR-CASH-004 | AC-CASH-004: ingreso/retiro exige motivo y actor | API-CASH-001 | REG-P0-006 |
| BR-CASH-005 | AC-CASH-005: solo efectivo altera efectivo esperado | UT-CASH-001 | REG-P0-007 |
| BR-CASH-006 | AC-CASH-006: fórmula concilia apertura, ventas, ingresos, retiros y devoluciones | UT-CASH-002; IT-CASH-002 | REG-P0-007 |
| BR-CASH-007 | AC-CASH-007: tarjeta/transferencia aparecen sin sumar efectivo | UT-CASH-003; IT-CASH-003 | REG-P0-007 |
| BR-CASH-008 | AC-CASH-008: diferencia es contado menos esperado y se conserva | UT-CASH-004; IT-CASH-004 | REG-P0-007 |
| BR-CASH-009 | AC-CASH-009: cierre es auditable e inmutable | IT-CASH-005 | REG-P0-006 |

## 7. Venta y pago

| Regla | Criterio de aceptación | Prueba primaria planificada | Regresión |
|---|---|---|---|
| BR-SALE-001 | AC-SALE-001: carrito vacío no confirma | UT-SALE-001; API-SALE-003 | REG-P0-004 |
| BR-SALE-002 | AC-SALE-002: cantidad cero, negativa o fraccionaria se rechaza | UT-QTY-002; API-SALE-004 | REG-P0-004 |
| BR-SALE-003 | AC-SALE-003: confirmación acepta exactamente un método de pago | UT-PAY-001; API-SALE-005 | REG-P0-004 |
| BR-SALE-004 | AC-SALE-004: pago conserva total exacto y separa efectivo pagadero | `SaleInputRulesTest`; `ApplicationContextIT` | REG-P0-004 |
| BR-SALE-005 | AC-SALE-005: efectivo insuficiente falla y vuelto usa el total efectivo | `SaleInputRulesTest`; `SaleWorkspace.test.tsx` | REG-P0-004 |
| BR-SALE-006 | AC-SALE-006: servidor ignora precio/total alterado | API-SALE-007 | REG-P0-010 |
| BR-SALE-007 | AC-SALE-007: línea guarda instantánea completa de producto y costo | IT-SALE-003 | REG-P1-010 |
| BR-SALE-008 | AC-SALE-008: cambio de catálogo no modifica venta previa | IT-HISTORY-004 | REG-P1-010 |
| BR-SALE-009 | AC-SALE-009: venta, pago, stock y caja confirman en una transacción | IT-TX-SALE-001 | REG-P0-004 |
| BR-SALE-010 | AC-SALE-010: falla inyectada no deja efectos parciales | IT-TX-SALE-002 | REG-P0-004 |
| BR-SALE-011 | AC-SALE-011: repetir clave retorna una venta y un solo efecto | IT-IDEM-SALE-001 | REG-P0-005 |
| BR-SALE-012 | AC-SALE-012: cancelar carrito no persiste venta ni efectos | FE-UT-CART-002; IT-SALE-004 | REG-P1-012 |
| BR-SALE-013 | AC-SALE-013: venta confirmada no admite edición | API-SALE-008 | REG-P1-010 |
| BR-SALE-014 | AC-SALE-014: folio es único y se identifica como no tributario | IT-DB-SALE-001; E2E-RECEIPT-001 | REG-P1-013 |
| BR-SALE-015 | AC-SALE-015: las diez terminaciones se redondean según Ley 20.956 solo al pagar en efectivo | `CashRoundingTest`; `SaleWorkspace.test.tsx` | REG-P0-004 |
| BR-SALE-016 | AC-SALE-016: se guardan total, ajuste, efectivo pagadero, recibido y vuelto; medios electrónicos quedan exactos | `ApplicationContextIT`; `SaleInputRulesTest` | REG-P0-004 |

## 8. Anulación

| Regla | Criterio de aceptación | Prueba primaria | Regresión |
|---|---|---|---|
| BR-VOID-001 | AC-VOID-001: solo estado confirmado admite anulación | `SaleCancellationServiceTest`; `ApplicationContextIT` | REG-P0-008 |
| BR-VOID-002 | AC-VOID-002: segunda anulación se rechaza sin efectos | `ApplicationContextIT` (secuencial y concurrente) | REG-P0-008 |
| BR-VOID-003 | AC-VOID-003: exige administrador, motivo y método | `SaleCancellationRulesTest`; `ApplicationContextIT`; `SaleWorkspace.test.tsx` | REG-P0-009 |
| BR-VOID-004 | AC-VOID-004: conserva original y registra datos de anulación | `ApplicationContextIT` | REG-P0-008 |
| BR-VOID-005 | AC-VOID-005: movimientos inversos se confirman en la misma transacción | `SaleCancellationServiceTest`; `ApplicationContextIT` | REG-P0-008 |
| BR-VOID-006 | AC-VOID-006: devolución en efectivo crea salida y reduce esperado | `SaleCancellationRulesTest`; `SaleCancellationServiceTest`; `ApplicationContextIT` | REG-P0-009 |
| BR-VOID-007 | AC-VOID-007: devolución no efectiva se registra sin acción bancaria | `SaleCancellationServiceTest`; `ApplicationContextIT` | REG-P0-009 |
| BR-VOID-008 | AC-VOID-008: falla inyectada revierte toda la anulación | `ApplicationContextIT` | REG-P0-008 |
| BR-VOID-009 | AC-VOID-009: efectivo exige caja abierta y afecta la actual | `SaleCancellationServiceTest`; `ApplicationContextIT` | REG-P0-009 |
| BR-VOID-010 | AC-VOID-010: caja histórica permanece cerrada e inmutable | `ApplicationContextIT` | REG-P0-009 |

## 9. Lector móvil

| Regla | Criterio de aceptación | Prueba primaria planificada | Regresión |
|---|---|---|---|
| BR-SCAN-001 | AC-SCAN-001: token es temporal, aleatorio, revocable, único y ligado al POS | IT-SCAN-PAIR-001; SEC-SCAN-001 | REG-P1-030 |
| BR-SCAN-002 | AC-SCAN-002: evento solo contiene código e identificador único | UT-SCAN-001; API-SCAN-001 | REG-P1-030 |
| BR-SCAN-003 | AC-SCAN-003: expirado, repetido o sesión ajena se rechaza | IT-SCAN-002; IT-IDEM-SCAN-001 | REG-P1-030 |
| BR-SCAN-004 | AC-SCAN-004: teléfono no puede imponer datos comerciales | API-SCAN-002 | REG-P1-030 |
| BR-SCAN-005 | AC-SCAN-005: código móvil atraviesa búsqueda y validaciones normales | IT-SCAN-003; E2E-SCAN-001 | REG-P1-030 |

## 10. Regla de mantenimiento

- Toda nueva regla debe añadirse a esta matriz en el mismo cambio.
- Una regla modificada obliga a revisar criterio, pruebas y regresión afectada.
- Una prueba implementada reemplaza su referencia planificada por el identificador ejecutable, conservando el ID estable.
- Una regla crítica sin prueba automatizada requiere justificación, evidencia manual y fecha de automatización.
- El gate de release falla si una regla P0 está sin trazabilidad o con prueba obligatoria fallida.

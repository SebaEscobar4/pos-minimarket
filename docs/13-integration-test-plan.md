# 13 - Plan de pruebas de integración

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Propuesta para aprobación - Entrega 3

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Objetivo

Demostrar que módulos, seguridad, transacciones, JPA, Flyway y PostgreSQL colaboran correctamente. Estas pruebas cubren aquello que una unidad aislada no puede probar de forma honesta.

## 2. Base técnica

- Spring Boot Test con contextos ajustados al objetivo.
- MockMvc para API, sesión, CSRF y autorización.
- Testcontainers con PostgreSQL para repositorios y transacciones.
- Flyway aplicado desde una base vacía.
- Datos sintéticos creados por prueba.
- Sin H2 ni base compartida entre ejecuciones.

## 3. Migraciones y esquema

| ID | Escenario | Resultado esperado |
|---|---|---|
| IT-DB-001 | Aplicar todas las migraciones a base vacía | Éxito sin intervención manual |
| IT-DB-002 | Validar esquema con JPA | Sin divergencias |
| IT-DB-003 | Código de barras duplicado | Restricción rechaza segunda fila |
| IT-DB-004 | Stock negativo directo | `CHECK` rechaza operación |
| IT-DB-005 | Dos cajas `OPEN` | Índice parcial rechaza segunda |
| IT-DB-006 | Idempotencia duplicada | Restricción única actúa |
| IT-DB-007 | Dos pagos por venta | Restricción única actúa |
| IT-DB-008 | Dos anulaciones | Restricción única actúa |
| IT-DB-009 | Evento lector repetido | Restricción compuesta actúa |

Las migraciones liberadas no se editan; cualquier cambio posterior crea una nueva versión.

## 4. Persistencia por módulo

### Catálogo

- Guardar y recuperar código con ceros iniciales.
- Buscar código exacto.
- Buscar nombre normalizado con tildes y mayúsculas.
- Paginación, orden y filtro de inactivos.
- Actualizar datos vigentes sin alterar líneas históricas.

### Inventario

- Bloquear saldo, actualizarlo e insertar movimiento.
- Verificar anterior, delta y resultante.
- Rollback si falla la inserción del movimiento.
- Reconciliación saldo contra movimientos.

### Caja

- Apertura única bajo solicitudes simultáneas.
- Movimientos asociados a sesión abierta.
- Cierre y fotografía histórica.
- Rechazo de operaciones posteriores al cierre.

### Venta

- Guardar cabecera, líneas, pago y referencias.
- Recuperar fotografía histórica tras editar producto.
- Folio e idempotencia únicos.

## 5. API y validación

| ID base | Área | Casos |
|---|---|---|
| API-AUTH | Login/logout | Éxito, genérico inválido, inactivo, rate limit y sesión invalidada |
| API-CAT | Productos | Crear, editar, buscar, duplicado, inactivo y validación |
| API-INV | Inventario | Entrada, ajuste, motivo ausente, stock insuficiente y permiso |
| API-CASH | Caja | Abrir, movimiento, resumen, cerrar, segunda apertura y cerrada |
| API-SALE | Venta | Confirmar por método, insuficiente, sin caja, sin stock y payload alterado |
| API-VOID | Anulación | Éxito, duplicada, vendedor, motivo y devolución sin caja |
| API-SCAN | Lector | Vincular, consumir, publicar, repetir, expirar, revocar y SSE |
| API-REPORT | Reportes | Filtros, zona horaria, permisos y totales |

Cada grupo verifica estados HTTP, `problem+json`, código estable, ausencia de detalles internos y efectos persistidos.

## 6. Seguridad

- Toda ruta es privada por defecto salvo login, salud limitada y consumo controlado del QR.
- Matriz administrador/vendedor para cada operación.
- CSRF ausente, inválido y válido.
- CORS desde origen permitido y no permitido.
- Cookie segura configurada según perfil.
- Logout, cambio de contraseña y desactivación invalidan sesión.
- Identificadores ajenos o inexistentes no filtran datos sensibles.
- Entrada con SQL/XSS se trata como datos y no altera consulta o respuesta.

## 7. Confirmación transaccional

### IT-SALE-TX-001 - Venta en efectivo exitosa

Verifica venta, líneas, pago, movimiento de caja, movimientos y saldos de inventario, folio, auditoría y vuelto.

### IT-SALE-TX-002 - Tarjeta y transferencia

Verifica registro del pago y ausencia de movimiento de efectivo.

### IT-SALE-TX-003 - Falla de inventario

Provoca stock insuficiente en una línea; no quedan venta, pago, movimientos ni cambios de saldo.

### IT-SALE-TX-004 - Falla posterior al descuento

Inyecta una falla controlada antes del commit; todo vuelve al estado inicial.

### IT-SALE-TX-005 - Caja cerrada durante confirmación

Coordina cierre y venta concurrentes. El orden de bloqueos permite solo un resultado consistente: venta antes del cierre o rechazo después del cierre.

## 8. Concurrencia

### IT-CONC-001 - Última unidad

1. Producto con saldo uno.
2. Dos transacciones con claves diferentes sincronizadas mediante barrera.
3. Ambas intentan vender una unidad.
4. Solo una confirma.
5. La otra recibe conflicto de stock.
6. Saldo final cero, una salida y una venta.

### IT-CONC-002 - Dos aperturas

Dos usuarios intentan abrir caja. Solo una sesión queda abierta.

### IT-CONC-003 - Dos anulaciones

Dos administradores anulan la misma venta. Solo una compensación se registra.

### IT-CONC-004 - Orden de productos

Dos ventas con los mismos productos en orden inverso confirman sin deadlock persistente gracias al bloqueo ordenado.

Las pruebas tendrán timeout explícito para detectar bloqueo infinito y se repetirán varias veces en CI/nightly.

## 9. Idempotencia

| ID | Escenario | Resultado |
|---|---|---|
| IT-IDEM-001 | Misma clave y mismo payload secuencial | Misma venta, un solo efecto |
| IT-IDEM-002 | Misma clave simultánea | Una creación y una respuesta equivalente |
| IT-IDEM-003 | Misma clave con payload distinto | Conflicto sin nuevos efectos |
| IT-IDEM-004 | Primera transacción falla | Reintento válido puede confirmar |
| IT-IDEM-005 | Claves distintas, mismo carrito | Dos intenciones independientes si existe stock |

## 10. Anulación

- Reposición exacta de cada línea.
- Registro único de usuario, motivo, fecha, monto y método.
- Devolución en efectivo en caja actualmente abierta.
- Venta antigua con caja original cerrada no altera ese cierre.
- Devolución electrónica no crea movimiento de efectivo.
- Falla en una reversión produce rollback completo.

## 11. Lector y SSE

- Token QR almacenado como hash y consumido una vez.
- Sesión lectora asociada al POS correcto.
- Evento publicado llega al stream autorizado.
- Evento no aparece en otra sesión.
- Reconexión dentro de ventana recupera evento permitido sin duplicarlo.
- Cierre de caja o revocación cierra/rechaza sesión lectora.
- Rate limit responde sin afectar la sesión humana.

Las pruebas de cámara pertenecen a QA manual; integración prueba el protocolo y seguridad.

## 12. Frontend con frontera simulada

- Contrato de errores y loading.
- Conflicto de stock tras una búsqueda previa.
- Reintento idempotente después de timeout.
- Sesión expirada redirige sin perder información sensible.
- Evento lector agrega o incrementa el producto correcto.

No se simulará el backend dentro de los E2E críticos.

## 13. Rendimiento y estabilidad básica

- Búsqueda con catálogo representativo.
- Cien confirmaciones secuenciales sintéticas sin fuga de conexiones.
- Canal SSE mantenido y reconectado.
- Reportes sobre volumen esperado de un año.

Los resultados establecen una línea base; no son una prueba de escala empresarial.

## 14. Evidencia y limpieza

Cada fallo conserva logs redactados, estado esperado/real y datos mínimos para reproducir. Los contenedores y datos son efímeros. Una prueba no puede dejar procesos o puertos ocupados para la siguiente.

## 15. Criterio de aceptación

La suite pasa con cero fallos y cero pruebas inestables conocidas. Las pruebas críticas de concurrencia, rollback e idempotencia deben ejecutarse contra PostgreSQL y no pueden omitirse para liberar.


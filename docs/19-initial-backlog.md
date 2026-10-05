# 19 - Backlog inicial y plan de implementación

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Propuesta para aprobación - Entrega 3

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Objetivo

Ordenar la implementación en incrementos verificables, priorizando primero las bases de seguridad y datos y luego el circuito comercial de mayor riesgo. Este backlog no sustituye el refinamiento: cada historia debe cumplir la definición de preparada antes de entrar a desarrollo.

## 2. Forma de trabajo propuesta

- Iteraciones cortas orientadas a capacidad y resultado, con una demostración al cierre.
- Una historia funcional debe incluir backend, frontend, migración, pruebas, seguridad y documentación cuando corresponda.
- Las reglas críticas se implementan mediante cortes verticales pequeños; no se dejarán transacciones o permisos para una fase final.
- El mismo cambio no se considera revisado solo porque una IA haya generado y luego inspeccionado su propio resultado: se exige evidencia automatizada y, para cambios críticos, revisión humana.
- Se mantiene una sola lista priorizada. Un incidente de severidad crítica desplaza trabajo planificado.
- Se utilizarán capas locales o gratuitas. Cualquier recurso de pago requiere una decisión nueva y aprobación explícita.

## 3. Prioridades

| Prioridad | Significado |
|---|---|
| P0 | Imprescindible para operar o proteger dinero, stock, acceso y recuperación |
| P1 | Necesario para completar el MVP operativo |
| P2 | Mejora posterior que no bloquea el uso manual seguro |

## 4. Épicas

| ID | Épica | Resultado |
|---|---|---|
| E00 | Fundación | Proyecto reproducible, migrable y verificable |
| E01 | Identidad y permisos | Acceso interno protegido por rol |
| E02 | Catálogo y búsqueda | Productos administrables y encontrables por código o nombre |
| E03 | Inventario | Saldo derivado de movimientos auditables |
| E04 | Caja | Apertura, movimientos, cierre y diferencias |
| E05 | Venta manual | Carrito, pago y confirmación transaccional |
| E06 | Historial y anulación | Consulta inmutable y reversión controlada |
| E07 | Lector móvil | Captura remota opcional sin lógica comercial en el teléfono |
| E08 | Reportes | Visibilidad operacional básica |
| E09 | Seguridad y observabilidad | Endurecimiento, auditoría y diagnóstico |
| E10 | Entrega y continuidad | Despliegue gratuito, backup, restauración y rollback |

## 5. Backlog priorizado

### Fundación e identidad

| ID / Pri. | Historia / resultado | Dependencias | Aceptación resumida |
|---|---|---|---|
| US-000 / P0 | Crear estructura del monolito modular y frontend | Ninguna | Backend y frontend compilan; límites base se verifican |
| US-001 / P0 | Levantar PostgreSQL local reproducible | US-000 | Un comando documentado inicia y comprueba la base |
| US-002 / P0 | Incorporar Flyway y migración inicial | US-001 | Base vacía migra; versión incompatible falla de forma visible |
| US-003 / P0 | Configurar controles de PR gratuitos | US-000 | Compilación, formato, pruebas, secretos y dependencias bloquean fallos |
| US-004 / P0 | Definir errores API, correlación, salud y logging seguro | US-000 | Errores no filtran secretos y una operación puede rastrearse |
| US-010 / P0 | Crear administrador inicial de forma segura | US-002 | Credencial temporal no queda en código y exige cambio |
| US-011 / P0 | Iniciar y cerrar sesión | US-010 | Solo usuario activo entra; logout invalida sesión |
| US-012 / P0 | Aplicar autorización por rol en backend | US-011 | Vendedor y administrador cumplen la matriz; manipular UI no evita control |
| US-013 / P0 | Proteger sesión, CSRF y límites de autenticación | US-011 | Peticiones inválidas se rechazan y el abuso queda limitado/auditado |
| US-014 / P1 | Administrar usuarios internos | US-012 | Administrador crea, desactiva y cambia roles sin borrar historial |

### Catálogo, búsqueda e inventario

| ID / Pri. | Historia / resultado | Dependencias | Aceptación resumida |
|---|---|---|---|
| US-020 / P0 | Administrar categorías | US-012 | Altas y ediciones validadas; inactivación no rompe historial |
| US-021 / P0 | Crear y editar productos | US-020 | Código textual opcional/único; dinero exacto; ceros preservados |
| US-022 / P0 | Crear saldo cero al registrar producto | US-021 | La creación no inventa stock; saldo y catálogo quedan consistentes |
| US-023 / P0 | Buscar producto por código exacto | US-021 | Coincidencia exacta, espacios externos normalizados y cero inicial preservado |
| US-024 / P0 | Buscar producto por nombre | US-021 | Coincidencia parcial sin distinguir mayúsculas ni acentos |
| US-025 / P1 | Desactivar y filtrar productos | US-021 | POS excluye inactivos; administración puede incluirlos explícitamente |
| US-030 / P0 | Registrar entrada de inventario | US-022 | Movimiento y saldo se confirman juntos con usuario y motivo/referencia |
| US-031 / P0 | Registrar ajuste positivo o negativo | US-030 | Solo administrador; motivo obligatorio; nunca deja saldo negativo |
| US-032 / P0 | Consultar saldo y libro de movimientos | US-030 | Saldo reconciliable; movimientos confirmados son inmutables |
| US-033 / P0 | Proteger actualizaciones concurrentes de stock | US-030 | Dos consumos de la última unidad no pueden confirmar ambos |

La historia US-022 es una capacidad mínima del módulo de inventario necesaria para entregar catálogo consistente. Los movimientos completos se incorporan desde US-030; la edición del producto nunca modifica stock.

### Caja y venta manual

| ID / Pri. | Historia / resultado | Dependencias | Aceptación resumida |
|---|---|---|---|
| US-040 / P0 | Abrir una única sesión de caja | US-012 | Aperturas concurrentes dejan exactamente una sesión abierta |
| US-041 / P0 | Registrar ingreso o retiro manual | US-040 | Monto exacto, motivo y usuario; sesión cerrada rechaza operaciones |
| US-042 / P0 | Consultar resumen de caja | US-041 | Se separan efectivo, tarjeta y transferencia; cálculo es reconciliable |
| US-043 / P0 | Cerrar caja con efectivo contado | US-042 | Diferencia queda registrada y la sesión no vuelve a abrirse |
| US-050 / P0 | Construir carrito con búsqueda manual | US-023, US-024, US-040 | Producto activo/con stock se agrega; cantidad es entera positiva |
| US-051 / P0 | Recalcular carrito en backend | US-050 | Cliente no puede imponer precio, subtotal ni total |
| US-052 / P0 | Confirmar venta en una transacción | US-033, US-041, US-051 | Venta, líneas, pago, stock y caja confirman juntos o ninguno |
| US-053 / P0 | Hacer idempotente la confirmación | US-052 | Repetir clave devuelve la misma venta sin duplicar efectos |
| US-054 / P0 | Cobrar en efectivo y calcular vuelto | US-052 | Redondeo legal aplicado al total final; recibido no es menor al efectivo pagadero y vuelto es exacto |
| US-055 / P0 | Cobrar con tarjeta o transferencia | US-052 | Pago queda registrado sin aumentar efectivo esperado |
| US-056 / P1 | Emitir comprobante interno | US-052 | Folio único, datos históricos y leyenda no tributaria |

### Historial, reversión, lector y reportes

| ID / Pri. | Historia / resultado | Dependencias | Aceptación resumida |
|---|---|---|---|
| US-060 / P0 | Consultar historial y detalle de venta | US-052 | Filtros operan en zona comercial y datos históricos no cambian |
| US-061 / P0 | Anular venta como administrador | US-060 | Motivo, devolución, stock y caja se registran atómicamente una vez |
| US-062 / P1 | Consultar auditoría de operaciones críticas | US-061 | Se identifica quién, cuándo, qué acción y referencia |
| US-070 / P2 | Vincular teléfono temporalmente | US-050 | Token aleatorio, revocable, de un uso y asociado al POS |
| US-071 / P2 | Enviar código desde cámara al POS | US-070 | Evento mínimo, único y temporal; backend resuelve producto |
| US-072 / P2 | Recuperar conexión y rechazar repeticiones | US-071 | Eventos expirados, duplicados o de otra sesión no alteran carrito |
| US-080 / P1 | Reportar ventas por periodo y método | US-060 | Totales conciliables con ventas y anulaciones |
| US-081 / P1 | Reportar productos y movimientos de inventario | US-032 | Resultado trazable al libro de movimientos |
| US-082 / P1 | Restringir reportes según rol | US-080 | Backend limita alcance; vendedor no obtiene información administrativa |

### Seguridad, entrega y continuidad

| ID / Pri. | Historia / resultado | Dependencias | Aceptación resumida |
|---|---|---|---|
| US-090 / P0 | Aplicar cabeceras, CORS, cookies y secretos seguros | US-013 | Configuración de staging supera checklist de seguridad |
| US-091 / P0 | Automatizar auditoría y alertas mínimas | US-004, US-062 | Fallos críticos son detectables sin registrar datos sensibles |
| US-092 / P1 | Verificar accesibilidad y operación con teclado | US-050 | Flujo manual principal funciona con teclado y mensajes comprensibles |
| US-100 / P0 | Crear staging en capas gratuitas | US-003, US-090 | Despliegue reproducible sin recurso pago ni datos reales |
| US-101 / P0 | Automatizar backup y probar restauración | US-100 | Restauración verificada con evidencia y datos reconciliados |
| US-102 / P0 | Definir rollback de aplicación y migración | US-100 | Ensayo controlado demuestra recuperación del candidato |
| US-103 / P0 | Ejecutar gate de primera salida operativa | US-101, US-102 | Checklist, regresión y aprobación explícita completos |

## 6. Secuencia inicial propuesta

| Iteración | Alcance | Demostración de salida |
|---|---|---|
| 0 - Fundación | US-000 a US-004 | Proyecto local reproducible, migración y controles automáticos |
| 1 - Acceso | US-010 a US-013 | Administrador y vendedor autenticados con permisos efectivos |
| 2 - Catálogo | US-020 a US-025 | Producto creado y encontrado por código o nombre, con saldo cero |
| 3 - Inventario | US-030 a US-033 | Entradas/ajustes auditables y protección de última unidad |
| 4 - Caja | US-040 a US-043 | Jornada de caja abierta, movida y cerrada con diferencia |
| 5 - Venta | US-050 a US-055 | Venta manual completa, atómica e idempotente |
| 6 - Control | US-056, US-060 a US-062 | Comprobante, historial y anulación reconciliables |
| 7 - Operación | US-080 a US-103 según dependencia | Reportes, staging, restauración y salida controlada |
| Posterior | US-070 a US-072 | Lector móvil opcional sin alterar el flujo manual |

La duración no se fija artificialmente: se estima cuando cada historia cumpla la definición de preparada y exista capacidad real. Si una iteración no produce evidencia ejecutable, no se considera terminada.

## 7. Definición de preparada

Una historia puede entrar a desarrollo cuando:

- tiene actor, objetivo, límites y dependencias;
- referencia reglas de negocio y criterios de aceptación verificables;
- identifica permisos, efectos en dinero/stock y necesidad de migración;
- incluye casos positivo, negativo, límite y concurrencia/idempotencia cuando corresponda;
- define evidencia de prueba y riesgos;
- no depende de una decisión abierta que cambie su solución.

## 8. Condiciones para iniciar código

El desarrollo puede comenzar después de aprobar esta entrega y comprobar:

- ningún punto abierto P0 impide la Iteración 0;
- US-000 a US-004 cumplen la definición de preparada;
- estrategia de repositorio y ramas acordada;
- versiones exactas de Java, Spring Boot, Node, React y PostgreSQL se fijarán y verificarán en el primer cambio, usando documentación oficial vigente;
- entorno local puede ejecutarse sin servicios pagos;
- el usuario da aprobación explícita para comenzar la implementación.

## 9. Riesgos de ejecución

| ID | Riesgo | Exposición | Tratamiento |
|---|---|---|---|
| RK-01 | Condición de carrera vende la última unidad dos veces | Media / Crítico | Bloqueo/actualización segura, restricción y prueba concurrente real |
| RK-02 | Falla parcial descoordina venta, stock, pago o caja | Media / Crítico | Una transacción y pruebas de fallas inyectadas |
| RK-03 | Reintento crea ventas duplicadas | Media / Crítico | Clave idempotente única, respuesta estable y regresión |
| RK-04 | Cálculo monetario o de caja pierde exactitud | Baja / Crítico | Tipos exactos, reglas unitarias y reconciliación |
| RK-05 | Cuota o suspensión de capa gratuita bloquea staging/CI | Alta / Alto | Local reproducible, ejecución bajo demanda y proveedor reemplazable |
| RK-06 | Proceso documental supera la capacidad del proyecto | Media / Medio | Evidencia proporcional, plantillas breves y foco en riesgos críticos |
| RK-07 | Implementación y revisión por la misma IA ocultan defectos | Media / Alto | Gates objetivos, revisión humana crítica y pruebas independientes |
| RK-08 | Cámara, permisos o red degradan el lector móvil | Alta / Medio | Flujo manual siempre disponible y pruebas en dispositivos reales |
| RK-09 | Backup existe pero no puede restaurarse | Media / Crítico | Ensayos periódicos y evidencia de reconciliación |
| RK-10 | Límites del proveedor cambian sin aviso | Media / Alto | Evitar acoplamiento, registrar límites y revisar antes de release |
| RK-11 | Datos sensibles aparecen en logs o errores | Baja / Alto | Lista permitida de campos, pruebas y revisión de seguridad |
| RK-12 | Una migración incompatible impide rollback | Media / Alto | Cambios expand/contract y ensayo con backup |

## 10. Decisiones abiertas

| ID | Decisión | Momento límite | Valor inicial sugerido |
|---|---|---|---|
| OQ-01 | Duración de sesión e inactividad | Antes de US-013 | Sesión de jornada con expiración por inactividad medible |
| OQ-02 | Alcance exacto de reportes para vendedor | Antes de US-082 | Solo su sesión y resumen operacional |
| OQ-05 | Vigencia de vinculación del lector | Antes de US-070 | Minutos, revocable y de un uso |
| OQ-06 | Objetivos RPO y RTO | Antes de US-101 | Definir según pérdida tolerable y tiempo de cierre |
| OQ-07 | Proveedor gratuito para staging | Antes de US-100 | Elegir con verificación vigente de cuotas y suspensión |
| OQ-08 | Contingencia manual ante caída | Antes de US-103 | Procedimiento breve de no venta o registro temporal controlado |

Ninguna sugerencia se convierte en requisito sin aprobación durante el refinamiento correspondiente.

### Decisiones resueltas

- OQ-04 fue aprobada el 2026-08-04: código opcional de 1 a 64 caracteres; letras, números y `- . _ /`; sin espacios internos; recorte solo en extremos; mayúsculas y ceros iniciales conservados; unicidad textual.
- OQ-03 fue aprobada el 2026-08-04: ingresos `REFUERZO_DE_EFECTIVO` y `OTRO_INGRESO`; retiros `PAGO_A_PROVEEDOR`, `GASTO_OPERATIVO`, `RETIRO_PREVENTIVO` y `OTRO_RETIRO`; motivo siempre obligatorio.

## 11. Criterio de aprobación de la Entrega 3

Se solicita aprobar la estrategia de calidad, la trazabilidad y este orden inicial. La aprobación habilita el refinamiento de la Iteración 0, pero el inicio de código seguirá requiriendo una instrucción explícita del usuario.

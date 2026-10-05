# 11 - Estrategia de pruebas

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Propuesta para aprobación - Entrega 3

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Objetivo

Demostrar con evidencia repetible que el sistema protege ventas, inventario, caja, permisos e historial ante casos normales, errores, límites, concurrencia y reintentos. Las pruebas deben encontrar defectos antes de producción y permitir cambiar el sistema sin perder confianza.

## 2. Principios

- Probar comportamiento observable y reglas, no detalles internos innecesarios.
- Mantener la mayor cantidad posible de pruebas rápidas y aisladas.
- Usar PostgreSQL real para restricciones, transacciones y concurrencia.
- No sustituir pruebas de dominio por mocks de toda la aplicación.
- Todo defecto corregido incorpora una prueba que habría detectado su causa.
- Los escenarios críticos incluyen caso positivo, negativo, límite y ausencia de efectos secundarios.
- La cobertura no reemplaza la revisión de casos ni la trazabilidad.
- Las pruebas deben ser deterministas, independientes y repetibles.
- Seguridad, accesibilidad y recuperación se verifican durante el desarrollo, no solo al final.

## 3. Pirámide de pruebas

| Nivel | Propósito | Herramientas iniciales | Frecuencia |
|---|---|---|---|
| Unitarias backend | Reglas y objetos de dominio sin Spring | JUnit 5, AssertJ | Cada cambio local y PR |
| Unitarias frontend | Comportamiento de componentes y carrito | Vitest, React Testing Library | Cada cambio local y PR |
| Arquitectura | Límites entre módulos y capas | ArchUnit | PR |
| Integración backend | JPA, PostgreSQL, transacciones, seguridad y API | Spring Boot Test, MockMvc, Testcontainers | PR |
| Integración frontend | Componentes con API simulada en frontera | Vitest, RTL, servidor simulado | PR |
| E2E | Flujos críticos desde navegador | Playwright | PR selectivo y pre-release |
| Seguridad | Autorización, CSRF, CORS, abuso y dependencias | Pruebas automatizadas y escáneres | PR, nightly y pre-release |
| Manuales | Cámara, conectividad, dispositivos y operación real | Checklist con evidencia | Staging y pre-release |
| Recuperación | Backup, migración, rollback y restauración | Scripts y procedimiento controlado | Pre-release y periódico |

La pirámide orienta la distribución, no impone porcentajes artificiales. Un E2E no reemplaza una regla unitaria, y una prueba unitaria no demuestra que la transacción funciona en PostgreSQL.

## 4. Alcance por riesgo

### Riesgo crítico

- Confirmación atómica de venta.
- Stock no negativo y última unidad concurrente.
- Idempotencia.
- Cálculos monetarios.
- Caja esperada, cierre y diferencias.
- Anulación y reposición.
- Autorización administrativa.
- Migraciones y restauración.

Requieren pruebas en más de un nivel y trazabilidad explícita.

### Riesgo alto

- Búsqueda por código/nombre.
- Productos inactivos o sin stock.
- Historial inmutable.
- Sesiones y CSRF.
- Vinculación y repetición del lector.
- Errores y auditoría.

### Riesgo medio o bajo

- Filtros secundarios.
- Textos y presentación.
- Detalles visuales que no cambian reglas.

Se prueban de forma proporcional mediante componentes, exploración y regresión selectiva.

## 5. Entornos

| Entorno | Uso de pruebas | Datos |
|---|---|---|
| Local | Unitarias, integración focalizada y E2E de desarrollo | Sintéticos |
| CI | Suite automatizada reproducible | Efímeros por ejecución |
| Staging gratuito | QA funcional, seguridad, E2E y dispositivos | Sintéticos representativos |
| Producción | Smoke no destructivo y monitoreo | Reales, sin pruebas que alteren negocio |

CI usará Testcontainers para PostgreSQL. No se utilizará H2 para validar comportamiento de persistencia porque no reproduce bloqueos, índices parciales ni semántica PostgreSQL.

## 6. Datos de prueba

- Builders y fixtures legibles por módulo.
- Reloj inyectable para fechas, expiraciones y jornadas.
- UUID predecibles cuando una aserción lo requiera.
- Códigos que incluyan ceros iniciales, duplicados, desconocidos y longitudes límite.
- Nombres con mayúsculas, minúsculas, tildes y `ñ`.
- Montos cero, límite y superiores a límites permitidos.
- Datos independientes por prueba; prohibido depender del orden de ejecución.
- Ningún dato real de clientes, usuarios o ventas.

## 7. Calendario de ejecución

| Momento | Suite obligatoria |
|---|---|
| Antes de commit | Unitarias relacionadas, formato y análisis rápido |
| Pull request | Compilación, unitarias, arquitectura, integración, frontend, cobertura, secretos y dependencias |
| Rama principal | Todo PR más migración desde cero y E2E críticos |
| Nightly o bajo demanda | Mutación crítica, suite ampliada de seguridad y E2E extendidos |
| Candidato a release | Regresión completa, manuales, backup/restauración, cabeceras y smoke de staging |
| Después de desplegar | Smoke no destructivo, salud y monitoreo |

Las cuotas gratuitas de CI se vigilarán. Si se agotan, las suites pesadas se ejecutarán localmente con evidencia o bajo demanda; nunca se eliminará una prueba crítica para ahorrar cuota.

## 8. Pruebas no funcionales

### Rendimiento proporcional

- Búsqueda POS con catálogo representativo.
- Confirmación dentro de un tiempo operacional aceptable.
- Cierre y reportes del rango esperado.
- Canal SSE estable durante una jornada simulada.

No se fijan objetivos de carga masiva porque existe una caja. Los umbrales concretos se medirán en staging y se documentarán antes del uso real.

### Accesibilidad

- Navegación por teclado en venta y formularios.
- Etiquetas, foco y mensajes de error.
- Contraste y estados no comunicados solo mediante color.
- Verificación automatizada básica más prueba manual.

### Compatibilidad

- Navegador principal del computador.
- Teléfonos Android/iOS disponibles para cámara.
- Pantallas pequeñas y permisos rechazados.

## 9. Evidencia

Cada ticket conservará:

- Casos ejecutados y resultado.
- Enlace al reporte CI o registro local equivalente.
- Cobertura relevante.
- Capturas o video solo cuando aporten evidencia visual.
- Logs redactados para fallos complejos.
- Defectos encontrados y resolución.
- Riesgo residual aceptado, si existe.

## 10. Responsabilidades

| Responsabilidad | Responsable principal |
|---|---|
| Prioridad y aceptación funcional | Product Owner |
| Diseño técnico y pruebas | Desarrollador senior/IA bajo revisión |
| Aprobación de arquitectura y riesgo | Responsable técnico usuario |
| QA funcional | Usuario con guía y evidencia preparada |
| Liberación o rollback | Usuario, nunca automático sin aprobación |

La misma IA puede implementar y revisar en pases separados, pero no se tratará como independencia absoluta. La aprobación humana, las pruebas automatizadas y la evidencia compensan parcialmente ese riesgo.

## 11. Entrada y salida de una historia

Una historia entra a desarrollo cuando tiene reglas, criterios de aceptación, permisos, errores, riesgos y pruebas previstas. Sale cuando pasa la Definition of Done, sus pruebas están trazadas, no introduce defectos bloqueantes y el responsable técnico comprende la solución.

## 12. Exclusiones iniciales

- Pruebas de múltiples sucursales o cajas.
- Carga de miles de usuarios concurrentes.
- Integraciones bancarias o tributarias.
- Modo offline.
- Certificación formal de seguridad.


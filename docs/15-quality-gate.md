# 15 - Quality gate

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Propuesta para aprobación - Entrega 3

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Objetivo

Definir condiciones objetivas para integrar y liberar sin convertir métricas en un ejercicio de cumplimiento superficial.

## 2. Gate de pull request

| Indicador | Umbral |
|---|---:|
| Compilación backend/frontend | 100 % exitosa |
| Pruebas fallidas | 0 |
| Pruebas inestables conocidas | 0 |
| Cobertura de líneas en código nuevo | >= 80 % |
| Cobertura de ramas en código nuevo | >= 75 % |
| Duplicación en código nuevo | <= 3 % |
| Violaciones de arquitectura | 0 |
| Secretos detectados | 0 |
| Vulnerabilidades altas/críticas explotables | 0 |
| Migración desde base vacía | Exitosa |
| Análisis estático bloqueante | 0 hallazgos |

## 3. Gate por módulo crítico

Para `sales`, `inventory` y `cash`:

| Indicador | Umbral |
|---|---:|
| Cobertura de líneas | >= 90 % |
| Cobertura de ramas | >= 85 % |
| Reglas críticas trazadas | 100 % |
| Casos positivo/negativo/límite | 100 % de reglas críticas aplicables |
| Concurrencia y rollback aplicables | Aprobados |

La cobertura se calcula sobre código con comportamiento. DTO triviales, clases generadas, configuración declarativa y migraciones se excluyen solo mediante configuración documentada.

## 4. Objetivos globales

| Área | Umbral inicial |
|---|---:|
| Backend total | >= 70 % líneas |
| Frontend total | >= 65 % líneas |
| Mutación en reglas críticas | >= 65 % inicial; >= 75 % posterior |

Durante las primeras historias, un módulo pequeño puede mostrar porcentajes extremos. El gate de código nuevo se aplica desde el primer PR y el objetivo global se vuelve bloqueante cuando exista una base funcional suficiente definida en el pipeline.

## 5. Gate de rama principal

Además del PR:

- Migraciones completas en PostgreSQL limpio.
- Pruebas de integración transaccional.
- E2E críticos seleccionados.
- Contrato OpenAPI generado sin cambio incompatible no aprobado.
- Artefactos identificables y reproducibles.
- Escaneo completo de dependencias.

## 6. Gate de candidato a release

| Indicador | Umbral |
|---|---:|
| Defectos críticos abiertos | 0 |
| Defectos altos abiertos | 0 |
| Vulnerabilidades críticas/altas sin tratamiento | 0 |
| Flujos E2E P0 aprobados | 100 % |
| Regresión seleccionada aprobada | 100 % |
| Migraciones fallidas | 0 |
| Restauración requerida | Demostrada |
| Riesgo de stock negativo | 0 conocido |
| Riesgo de descuadre de caja por software | 0 conocido |
| Acceso no autorizado conocido | 0 |
| Costos no aprobados | 0 |

## 7. Métricas funcionales

- Ventas confirmadas sin movimientos requeridos: 0.
- Movimientos manuales sin motivo: 0.
- Ventas en efectivo sin efecto de caja: 0.
- Stock negativo: 0.
- Anulaciones duplicadas: 0.
- Claves idempotentes con más de una venta: 0.
- Escaneos entregados a otra sesión: 0.
- Diferencias de caja causadas por cálculo del sistema: 0.

Estas métricas pueden implementarse mediante consultas de reconciliación y alertas, no solo pruebas.

## 8. Herramientas gratuitas

- JaCoCo para cobertura backend.
- Reporte de cobertura de Vitest para frontend.
- PIT para mutación.
- ArchUnit para límites.
- SpotBugs/PMD y herramientas de formato según configuración aprobada.
- Gitleaks para secretos.
- OWASP Dependency-Check, auditoría npm o escáner gratuito equivalente.
- Playwright para E2E.

El gate no dependerá obligatoriamente de un servicio SaaS pagado. Debe poder ejecutarse localmente o dentro de cuotas gratuitas de CI.

## 9. Prevención de métricas artificiales

Se prohíbe:

- Añadir aserciones que no prueban comportamiento.
- Excluir paquetes solo para subir porcentaje.
- Crear pruebas que replican implementación sin validar resultado.
- Reducir umbral para aprobar un PR concreto.
- Marcar una prueba inestable como ignorada sin defecto y plan.
- Mockear la base para evitar una prueba crítica de integración.

La revisión de mutación y trazabilidad ayuda a detectar cobertura vacía.

## 10. Excepciones

Una excepción incluye métrica, valor real, causa, riesgo, medida compensatoria, responsable y fecha de vencimiento. No puede exceptuar stock negativo, corrupción de ventas/caja, acceso no autorizado, secreto expuesto o vulnerabilidad crítica explotable.

## 11. Tendencias

Además del umbral, se observarán:

- Tiempo de pipeline.
- Tasa de defectos escapados.
- Reaperturas.
- Pruebas inestables.
- Complejidad y duplicación.
- Tiempo medio de corrección.

Una tendencia negativa sostenida genera una historia de calidad, aunque el gate todavía pase.


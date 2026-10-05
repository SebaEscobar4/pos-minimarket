# 14 - Procedimiento de QA

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Propuesta para aprobación - Entrega 3

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Objetivo

Establecer un flujo repetible desde que una historia está lista hasta que puede liberarse, con criterios claros, evidencia y autoridad de aprobación.

## 2. Flujo por ticket

1. Refinamiento y Definition of Ready.
2. Identificación de reglas, riesgos y permisos.
3. Diseño de casos de prueba.
4. Implementación en cambio pequeño.
5. Pruebas unitarias.
6. Pruebas de integración y arquitectura.
7. Revisión técnica separada.
8. Checks automatizados de pull request.
9. QA funcional en staging cuando corresponda.
10. Regresión afectada.
11. Corrección y reejecución.
12. Aprobación del Product Owner/responsable técnico.
13. Inclusión en candidato a release.
14. Monitoreo posterior.

Ninguna etapa se omite silenciosamente. Si no aplica, el ticket registra la justificación.

## 3. Definition of Ready

Una historia puede comenzar cuando incluye:

- Identificador, épica y prioridad.
- Usuario y problema.
- Resultado de negocio esperado.
- Reglas `BR-*` relacionadas.
- Precondiciones y entradas.
- Flujo principal y alternativas.
- Permisos por rol.
- Errores y mensajes relevantes.
- Criterios de aceptación verificables.
- Pruebas previstas por nivel.
- Riesgos de seguridad/datos.
- Dependencias y migraciones.
- Fuera de alcance.
- Dudas bloqueantes resueltas.

## 4. Plantilla de ticket

| Campo | Contenido esperado |
|---|---|
| Objetivo | Cambio observable y valor |
| Contexto | Problema y usuario |
| Reglas | IDs del documento 05 |
| Criterios | Given/When/Then o lista verificable |
| Permisos | Roles autorizados/no autorizados |
| Datos | Entradas, límites y migración |
| Errores | Conflictos, validación y mensajes |
| Pruebas | Unitarias, integración, E2E y manuales |
| Seguridad | Amenazas/controles afectados |
| Observabilidad | Logs, auditoría o métricas |
| Fuera de alcance | Límites explícitos |
| Evidencia | Reportes requeridos para cerrar |

## 5. Diseño de casos

Por cada criterio se aplicarán cuando correspondan:

- Particiones válidas e inválidas.
- Valores límite.
- Tabla de decisión.
- Transición de estados.
- Combinación de roles.
- Fallo de dependencias.
- Reintento y duplicación.
- Concurrencia.
- Ausencia de efectos secundarios.

Las reglas críticas deben tener caso positivo, negativo y límite. No se acepta “probar que funciona” como descripción suficiente.

## 6. Revisión técnica

El revisor verifica:

- Alcance del ticket y ausencia de cambios laterales innecesarios.
- Respeto de límites de módulos.
- Validaciones críticas en backend.
- Transacción y manejo de concurrencia.
- Autorización y auditoría.
- Nombres, legibilidad y complejidad.
- Pruebas que fallarían ante un defecto real.
- Migraciones reversibles o compatibles con rollback.
- Ausencia de secretos, datos reales y logs sensibles.
- Documentación y ADR cuando cambia una decisión.

La revisión no se limita a aceptar el código generado por IA. Debe explicar por qué la solución preserva las reglas.

## 7. QA funcional en staging

### Preparación

- Versión identificable desplegada.
- Migraciones exitosas.
- Datos sintéticos conocidos.
- Usuario administrador y vendedor de prueba.
- Caja y productos en estados requeridos.
- Navegador/dispositivo registrado.

### Ejecución

- Smoke inicial.
- Criterios de la historia.
- Exploración del área afectada.
- Roles y errores.
- Regresión seleccionada.
- Captura de evidencia útil.

### Cierre

- Resultado por caso.
- Defectos enlazados.
- Riesgos residuales.
- Versión y ambiente.
- Aprobación o rechazo.

## 8. QA exploratorio

Se realizarán sesiones cortas con una misión concreta, por ejemplo:

- Intentar romper el carrito mediante cambios rápidos.
- Operar con stock exactamente uno.
- Alternar usuario/rol y volver con historial del navegador.
- Cerrar caja mientras existe una confirmación lenta.
- Reutilizar código, QR o solicitud anterior.
- Cambiar conectividad del teléfono durante escaneo.

Cada sesión registra duración, misión, datos, hallazgos y preguntas.

## 9. Pruebas manuales del lector

| Área | Variaciones |
|---|---|
| Dispositivo | Android/iOS disponibles, cámara frontal/trasera cuando aplique |
| Código | EAN comunes, pequeño, brillante, curvo, dañado y desconocido |
| Luz | Normal, baja, reflejo y contraluz |
| Ritmo | Lectura única, rápida repetida y dos códigos cercanos |
| Permisos | Aceptado, rechazado, revocado y navegador sin cámara |
| Red | Wi-Fi estable, débil, pérdida, reconexión y cambio a datos |
| Estado | Teléfono bloqueado, pestaña en segundo plano y sesión expirada |
| Pantalla | Pequeña, rotación y zoom |

Se probará que un fallo del lector no confirma ventas ni modifica datos por sí mismo.

## 10. Evidencia mínima

- Resultado CI con commit.
- Reporte de pruebas y cobertura.
- Evidencia de integración para reglas transaccionales.
- Captura solo para comportamiento visual/manual.
- IDs de defectos.
- Checklist firmado digitalmente mediante aprobación del ticket/PR.

No se exigen capturas de cada prueba automática; el reporte reproducible es evidencia superior.

## 11. Gestión de ambientes

- Local y CI pueden recrearse desde cero.
- Staging no comparte datos ni secretos con producción.
- El despliegue gratuito se etiqueta como staging/demo hasta cumplir requisitos productivos.
- Los datos de QA se reinician mediante procedimiento, no edición improvisada.
- Ninguna prueba destructiva se ejecuta contra producción.

## 12. Criterios de suspensión

QA se detiene si:

- La versión no es identificable.
- Migraciones fallan.
- El ambiente es inestable e impide distinguir defectos.
- Falla el smoke principal.
- Aparece corrupción, stock negativo, caja incorrecta o acceso no autorizado.
- Los datos de prueba no permiten reproducir resultados.

La suspensión crea un defecto o tarea de ambiente y evita acumular evidencia inválida.

## 13. Definition of Done

La historia termina cuando:

- Cumple criterios y reglas.
- Tiene validación backend.
- Incluye pruebas unitarias e integración según riesgo.
- Incluye casos de permisos.
- Pasa quality gates.
- No tiene defectos críticos o altos abiertos.
- Migraciones son reproducibles.
- Documentación y trazabilidad están actualizadas.
- Existe evidencia.
- El responsable técnico comprende y aprueba.
- Existe rollback cuando corresponde.

## 14. Aprobación

La IA puede proponer resultado y evidencia, pero no libera. El responsable técnico usuario aprueba el ticket, candidato y despliegue. No se realizan merges o producción sin autorización explícita.


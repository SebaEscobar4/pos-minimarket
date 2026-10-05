# 18 - Gestión de defectos

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Propuesta para aprobación - Entrega 3

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Objetivo

Registrar, priorizar, corregir y aprender de defectos sin perder contexto, evidencia o trazabilidad.

## 2. Información obligatoria

- Identificador y título observable.
- Versión, ambiente y fecha.
- Reportante.
- Precondiciones y datos sintéticos.
- Pasos reproducibles.
- Resultado esperado y real.
- Frecuencia.
- Severidad y prioridad.
- Módulo, regla y caso relacionado.
- Evidencia útil y correlation ID.
- Impacto en datos/seguridad.
- Workaround conocido.

No se adjuntan contraseñas, sesiones, tokens o datos sensibles.

## 3. Severidad

| Severidad | Definición | Ejemplos |
|---|---|---|
| Crítica | Corrupción, pérdida económica, acceso indebido o sistema inutilizable | Venta cobrada no registrada, caja/stock corrupto, control admin perdido |
| Alta | Función principal incorrecta sin alternativa segura | Venta duplicada, anulación incorrecta, método de pago erróneo |
| Media | Error importante con workaround controlado | Filtro incorrecto, flujo secundario bloqueado, mensaje engañoso |
| Baja | Impacto menor visual o textual | Espaciado, copy, alineación y mejora menor |

La severidad mide impacto, no orden de trabajo.

## 4. Prioridad

| Prioridad | Tratamiento |
|---|---|
| P0 | Detener liberación/operación afectada y atender inmediatamente |
| P1 | Corregir antes de la siguiente liberación |
| P2 | Planificar en iteración próxima según valor/riesgo |
| P3 | Backlog y revisión periódica |

Prioridad considera severidad, frecuencia, alcance, exposición, workaround y proximidad de release.

## 5. Matriz inicial

| Severidad | Prioridad habitual | Liberación |
|---|---|---|
| Crítica | P0 | Bloqueada |
| Alta | P1/P0 | Bloqueada |
| Media | P2, ocasional P1 | Requiere decisión |
| Baja | P3 | No bloquea por sí sola |

Una vulnerabilidad alta/crítica se trata conforme a explotabilidad y exposición, pero nunca se oculta como defecto funcional bajo.

## 6. Ciclo de vida

`NUEVO -> TRIAGE -> LISTO -> EN_PROGRESO -> EN_REVISION -> LISTO_QA -> CERRADO`

Estados alternativos:

- `BLOQUEADO`: falta dependencia o ambiente.
- `DUPLICADO`: referencia al defecto principal.
- `NO_REPRODUCIBLE`: evidencia de intentos y condiciones.
- `NO_SE_CORREGIRA`: riesgo aceptado explícitamente.
- `REABIERTO`: la corrección falló o reapareció.

## 7. Triage

El triage confirma:

- Que existe un defecto y no un requisito nuevo.
- Impacto y usuarios afectados.
- Integridad de datos.
- Riesgo de seguridad.
- Reproducibilidad.
- Regla o criterio violado.
- Severidad, prioridad y responsable.

Un defecto crítico activa evaluación de incidente y detiene tareas no esenciales.

## 8. Objetivos de respuesta internos

| Clase | Triage objetivo | Decisión de tratamiento |
|---|---:|---:|
| Crítica/P0 | Inmediato al detectarse | Mismo día |
| Alta/P1 | <= 1 día hábil | Antes de liberar |
| Media/P2 | <= 3 días hábiles | Próxima planificación |
| Baja/P3 | Revisión de backlog | Según prioridad |

Son objetivos de trabajo, no SLA comercial.

## 9. Corrección

Toda corrección incluye:

- Causa, no solo síntoma.
- Prueba automatizada que falla antes y pasa después cuando sea viable.
- Evaluación de casos similares.
- Regresión afectada.
- Revisión de datos existentes.
- Actualización de documentación/regla si existía ambigüedad.
- Evidencia de QA.

## 10. Defectos de datos

Si existe corrupción potencial:

1. Detener operaciones que amplíen el daño.
2. Preservar logs y snapshot/backup.
3. Identificar registros afectados mediante consulta reproducible.
4. Diseñar reparación como migración o comando auditable.
5. Probar en copia aislada.
6. Obtener aprobación antes de modificar datos reales.
7. Reconciliar ventas, inventario y caja después.

No se corrigen registros productivos manualmente sin evidencia y autorización.

## 11. Vulnerabilidades

- Visibilidad restringida mientras exista riesgo de explotación.
- No incluir payload peligroso o secretos en canales públicos.
- Rotar credenciales cuando corresponda.
- Evaluar exposición y datos afectados.
- Crear prueba segura de no regresión.
- Documentar divulgación y cierre.

## 12. Causa raíz

Requerida para defectos críticos, altos repetidos o escapados a producción. Categorías iniciales:

- Requisito ambiguo.
- Diseño incompleto.
- Implementación.
- Prueba ausente/ineficaz.
- Revisión insuficiente.
- Configuración/ambiente.
- Dependencia/proveedor.
- Error operacional.

La causa raíz produce al menos una acción preventiva verificable.

## 13. Métricas

- Defectos por severidad y módulo.
- Escapados a staging/producción.
- Tiempo de triage y corrección.
- Reaperturas.
- Causas raíz recurrentes.
- Defectos encontrados por prueba automática vs manual.
- Edad del backlog.

Las métricas se usan para mejorar el proceso, no para medir productividad individual.

## 14. Cierre

Un defecto se cierra cuando la causa fue tratada, las pruebas pasan, QA reproduce el escenario original, la regresión afectada está aprobada y no quedan efectos de datos sin resolver.


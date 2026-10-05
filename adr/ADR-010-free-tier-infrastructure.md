# ADR-010 - Infraestructura sin costo durante la etapa actual

**Estado:** Aceptado

**Fecha:** 2026-07-31

## Contexto

El proyecto se encuentra en etapa de desarrollo y validación. El responsable técnico no desea contratar servicios ni asumir cobros por infraestructura por el momento. Los planes gratuitos pueden incluir suspensión, cuotas reducidas, falta de SLA, retención limitada y cambios de condiciones.

## Decisión

- Utilizar herramientas locales y planes gratuitos para desarrollo, CI, staging y el primer despliegue.
- No crear recursos con cobro, autoescalado pagado o sobreconsumo facturable sin aprobación explícita.
- Documentar límites, persistencia, backups, suspensión y requisitos de tarjeta antes de elegir proveedor.
- No clasificar como producción real un ambiente gratuito que incumpla controles mínimos de seguridad, backup o restauración.
- Evaluar infraestructura pagada solo mediante una decisión futura separada.

## Consecuencias

### Positivas

- El proyecto puede avanzar sin compromiso financiero.
- Obliga a mantener despliegues simples y portables.
- Evita dependencia temprana de un proveedor.

### Costos y riesgos

- Posibles cold starts, suspensión o límites de recursos.
- Menor disponibilidad y ausencia de SLA.
- Backups o restauración pueden requerir procedimientos manuales.
- La operación real podría necesitar una reevaluación antes de usar datos críticos.

## Alternativas descartadas

- Contratar infraestructura desde el inicio: no se justifica antes de validar el producto.
- Usar recursos pagados sin límite por conveniencia: contradice la restricción presupuestaria.
- Reducir seguridad para caber en un plan gratuito: riesgo inaceptable; se prefiere mantener el sistema local.

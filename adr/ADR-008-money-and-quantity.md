# ADR-008 — Dinero CLP exacto y cantidades enteras

**Estado:** Aceptado

**Fecha:** 2026-07-31

## Contexto

El MVP trabaja en Chile, no incluye monedas extranjeras ni productos vendidos por peso. Los errores de punto flotante pueden generar diferencias de caja.

## Decisión

- Moneda única CLP.
- Java usa objeto de valor `Money` respaldado por `BigDecimal` de escala cero.
- PostgreSQL usa `numeric(19,0)`.
- API representa pesos como enteros JSON dentro de límites seguros.
- Cantidades y saldos usan enteros.
- Multiplicación y suma aplican validaciones de rango exactas.

## Consecuencias

### Positivas

- Sin errores binarios de redondeo.
- Contrato simple y consistente con precios del negocio.
- Invariantes fáciles de probar.

### Costos

- Productos por peso o monedas con centavos requieren una decisión futura.
- Deben validarse límites al convertir JSON/TypeScript.

## Alternativas descartadas

- `double`/`float`: no adecuados para dinero.
- Guardar centavos ficticios: agrega conversión sin beneficio para CLP.
- `long` sin objeto de valor: exacto, pero facilita mezclar montos con otros enteros.

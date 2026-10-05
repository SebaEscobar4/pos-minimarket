# ADR-009 — Búsqueda unificada de productos

**Estado:** Aceptado

**Fecha:** 2026-07-31

## Contexto

El vendedor necesita un campo rápido que acepte código o nombre. Los nombres en español deben encontrarse aunque el usuario omita acentos o cambie mayúsculas. El catálogo esperado es pequeño.

## Decisión

- Código como texto, recortado solo en extremos, con coincidencia exacta e índice único parcial.
- Nombre visible separado de `search_name` normalizado en aplicación: minúsculas y sin diacríticos.
- Primero se prioriza código exacto; si no existe, nombre parcial.
- POS limita resultados a activos y consulta saldo desde inventario.
- Administración puede incluir inactivos.
- Búsqueda inicial con PostgreSQL y paginación; sin motor externo.

## Consecuencias

### Positivas

- Experiencia rápida y predecible.
- Conserva ceros iniciales.
- No depende de extensiones específicas del proveedor.

### Costos

- `LIKE '%texto%'` no usa bien un índice B-tree en catálogos grandes.
- La normalización debe ser idéntica al guardar y buscar.
- Cambios en la regla de normalización requieren migrar `search_name`.

## Alternativas descartadas

- Elasticsearch: infraestructura desproporcionada.
- Solo búsqueda exacta por nombre: mala experiencia.
- `unaccent` obligatorio: dependencia del proveedor sin necesidad actual.
- `pg_trgm`: se reevaluará únicamente con mediciones reales.

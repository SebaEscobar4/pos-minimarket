# ADR-007 — Una aplicación React y mismo origen preferido

**Estado:** Aceptado

**Fecha:** 2026-07-31

## Contexto

El POS, administración y lector comparten contrato, diseño y autenticación, pero presentan interfaces diferentes. Separar aplicaciones duplicaría configuración y despliegues.

## Decisión

- Una aplicación React/Vite/TypeScript organizada por funcionalidades.
- Rutas diferenciadas para POS, administración y lector.
- Frontend y API servidos bajo el mismo origen cuando la plataforma lo permita, usando `/api` para backend.
- El carrito permanece local hasta confirmar.
- No usar Redux inicialmente.

## Consecuencias

### Positivas

- Un solo contrato, pipeline y sistema de componentes.
- Cookies, CSRF y CORS más simples bajo mismo origen.
- Menos duplicación entre escritorio y móvil.

### Costos

- Debe existir carga por rutas para evitar entregar código administrativo innecesario al lector.
- Una liberación actualiza todas las interfaces.

## Alternativas descartadas

- Aplicaciones separadas: costo sin necesidad actual.
- React Native: aplicación móvil nativa fuera de alcance.
- Redux: no existe estado global complejo que lo justifique inicialmente.

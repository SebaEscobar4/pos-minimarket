# ADR-005 — Autenticación mediante sesión revocable

**Estado:** Aceptado

**Fecha:** 2026-07-31

## Contexto

La aplicación es web, tiene pocos usuarios internos y un único backend. Necesita cerrar sesiones, desactivar usuarios y reducir exposición de credenciales en JavaScript.

## Decisión

Usar Spring Security y Spring Session JDBC:

- Identificador opaco en cookie `HttpOnly`.
- Cookie `Secure` en ambientes públicos y `SameSite` apropiado.
- Rotación al autenticar.
- CSRF en operaciones mutables.
- Invalidación en logout, cambio de contraseña o desactivación.
- Argon2id para contraseñas.

Se prefiere desplegar frontend y API bajo el mismo sitio/origen.

## Consecuencias

### Positivas

- Revocación inmediata y modelo simple.
- El token no queda disponible a JavaScript.
- Menos código de emisión y renovación de tokens.

### Costos

- Requiere protección CSRF.
- Despliegue cross-site exige cuidado adicional con cookies y CORS.
- Las tablas de sesión agregan datos operacionales a PostgreSQL.

## Alternativas descartadas

- JWT en `localStorage`: mayor exposición ante XSS y revocación más compleja.
- JWT en cookie: sigue requiriendo CSRF y agrega renovación sin beneficio actual.
- Sesiones solo en memoria: se pierden al reiniciar y dificultan invalidación consistente.

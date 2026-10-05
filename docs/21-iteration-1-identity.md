# 21 - Iteración 1: identidad y permisos

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** US-010 a US-012 implementadas; US-013 implementada salvo OQ-01

**Fecha:** 2026-08-04

## Alcance implementado

- US-010: primer administrador desde variables externas, hash Argon2id y cambio obligatorio de contraseña temporal.
- US-011: login genérico para credencial inválida o usuario inactivo, sesión JDBC revocable y logout de servidor.
- US-012: rutas privadas por defecto, autoridades técnicas `ADMIN`/`SELLER` y control administrativo aplicado en backend. La UI presenta estos roles como Administrador/Vendedor.
- US-013 independiente de OQ-01: cookie opaca `POS_SESSION` HttpOnly/SameSite, Secure configurable, rotación al autenticar, CSRF, verificación configurable de Origin/Referer, límite por cuenta y origen y auditoría de autenticación.

Flyway V2 crea usuarios, auditoría y tablas de Spring Session. Cambiar contraseña invalida todas las sesiones del usuario. La desactivación y administración de usuarios corresponden a US-014; el mecanismo de revocación deberá reutilizarse allí.

## Evidencia ejecutable

- Pruebas unitarias de reglas de entrada, principal autenticado, bootstrap, Argon2id, cambio de contraseña, rate limit, auditoría, respuesta segura y validación de origen.
- `ApplicationContextIT` aplica Flyway desde PostgreSQL vacío y prueba bootstrap, login, usuario inactivo, cambio obligatorio, revocación, logout, CSRF, límite de intentos y autorización positiva/negativa de administrador y vendedor.
- Pruebas frontend cubren sesión inicial, login, error genérico, cambio obligatorio, rol, logout y recuperación ante indisponibilidad.

Los parámetros medidos de Argon2id son 19.456 KiB de memoria, 2 iteraciones y paralelismo 1. La prueba sintética registró entre 50 y 66 ms en el equipo de validación; debe volver a medirse en el hardware objetivo antes de staging.

## Decisión pendiente OQ-01

No se fijó timeout de sesión. Se propone para aprobación una expiración por 30 minutos de inactividad y un máximo absoluto de 12 horas, ambos configurables. Hasta aprobar e implementar estos valores, US-013 y el gate de Iteración 1 permanecen parciales aunque sus demás controles estén validados.

# 28 - Preparación de continuidad local

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** base local implementada y validada; US-100, US-101, US-102 y US-103 continúan pendientes de decisiones y ambiente objetivo

**Fecha:** 2026-08-06

## Alcance incorporado

- `scripts/backup-postgres.ps1` genera un `pg_dump` personalizado desde el PostgreSQL de Docker Compose y un checksum SHA-256.
- `scripts/test-postgres-restore.ps1` exige ese checksum, restaura exclusivamente en una base temporal `pos_restore_*`, reconcilia migraciones, usuarios, inventario, caja, ventas, pagos y anulaciones, y elimina la base temporal.
- `backups/` queda fuera de Git por contener potencialmente datos sensibles.
- `ApplicationContextIT` demuestra el mismo mecanismo sobre PostgreSQL 18.4 aislado mediante Testcontainers, con datos sintéticos producidos por la regresión completa.
- La prueba de reportes dejó de depender de una fecha fija y ahora utiliza el día comercial de `America/Santiago`.

La prueba de restauración nunca apunta a `pos_minimarket` ni sustituye una base existente. Para que la comparación manual sea estable, el respaldo y el ensayo local se ejecutan sin ventas u otras escrituras en curso.

## Evidencia

- Prueba de caja negra del MVP: aprobada por el responsable el 2026-08-06.
- Parser de PowerShell: ambos scripts sin errores sintácticos.
- `mvnw.cmd test`: 98 pruebas aprobadas.
- `mvnw.cmd verify`: 98 pruebas unitarias y 18 pruebas de integración aprobadas; restauración aislada, Flyway v10, cobertura, arquitectura y SpotBugs aprobados.
- `npm audit --audit-level=high` online: bloqueado por `EACCES` al conectar con `https://registry.npmjs.org/-/npm/v1/security/advisories/bulk`; el endpoint no entregó un informe.
- Auditoría npm offline de la validación anterior: 0 vulnerabilidades conocidas en la caché disponible. No reemplaza el control online requerido para liberar.

Los scripts operacionales se validaron sintácticamente. Su ejecución directa contra Docker Compose permanece pendiente porque el proceso de Codex no tiene acceso al socket de Docker; la restauración equivalente sí fue ejecutada mediante el gate Maven autorizado.

## Decisiones abiertas para aprobación

Ninguna de estas propuestas modifica aún los requisitos aceptados:

- **OQ-06:** RPO máximo de 4 horas durante operación, RTO de 2 horas y retención inicial de 14 respaldos diarios más 8 semanales. También se exige respaldo previo a cada despliegue.
- **OQ-07:** usar primero un staging local aislado, con datos sintéticos, puertos, secretos y volumen propios. Un proveedor público gratuito se evaluará por separado y no se creará sin aprobación vigente de sus límites.
- **OQ-08:** durante una caída, usar un registro manual prenumerado y suspender la carga al POS hasta poder reconciliar folios, caja e inventario mediante un procedimiento aprobado. La forma exacta de reingreso requiere refinamiento para no alterar fechas históricas.

Antes de cerrar US-101 también falta elegir y ensayar cifrado del respaldo fuera del volumen de origen. El `.dump` local actual no cumple por sí solo `SEC-DATA-006` para producción.

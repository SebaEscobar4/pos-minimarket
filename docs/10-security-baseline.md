# 10 — Baseline de seguridad

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Aprobado - Entrega 2

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Propósito y referencias

Esta baseline convierte el modelo de amenazas en requisitos mínimos verificables. La referencia técnica principal será [OWASP ASVS 5.0.0](https://owasp.org/www-project-application-security-verification-standard/), versión estable vigente al redactar este documento. Se aplicará el nivel 1 completo que corresponda al sistema y controles seleccionados de nivel 2 para autenticación, sesiones, autorización, lógica de negocio, datos y API.

Para el proceso se utiliza [NIST SP 800-218, SSDF 1.1](https://csrc.nist.gov/pubs/sp/800/218/final), que continúa siendo la publicación final. SSDF 1.2 se encuentra como borrador inicial de revisión y no se usará como requisito normativo hasta existir una versión final.

El proyecto no afirmará “cumplimiento ASVS” o “certificación NIST” sin una evaluación completa y evidencia. Las referencias orientan controles proporcionales al negocio.

## 2. Criterio de obligatoriedad

- **MUST:** obligatorio antes de producción.
- **SHOULD:** obligatorio salvo justificación y aceptación explícita del riesgo.
- **LATER:** control planificado que no bloquea el MVP actual.

## 3. Gobierno y desarrollo seguro

| ID | Nivel | Requisito | Evidencia esperada |
|---|---|---|---|
| SEC-GOV-001 | MUST | Reglas críticas y amenazas tienen identificadores trazables | Documentos y matriz |
| SEC-GOV-002 | MUST | Cambios pequeños mediante pull request y checks obligatorios | Historial del repositorio |
| SEC-GOV-003 | MUST | Rama principal protegida; sin merge o despliegue automático no aprobado | Configuración del repositorio |
| SEC-GOV-004 | MUST | Ningún dato real o secreto se usa en pruebas | Fixtures y revisión |
| SEC-GOV-005 | MUST | Defectos críticos, vulnerabilidades altas o secretos bloquean liberación | Quality gate |
| SEC-GOV-006 | SHOULD | Generar SBOM de backend y frontend en versiones liberables | Artefacto CycloneDX o equivalente |
| SEC-GOV-007 | MUST | El modelo de amenazas se revisa en cambios sensibles | Checklist de PR/release |

Estas prácticas se relacionan con los grupos `PO`, `PS`, `PW` y `RV` de SSDF: preparar la organización, proteger el software, producir software seguro y responder a vulnerabilidades.

## 4. Autenticación

| ID | Nivel | Requisito |
|---|---|---|
| SEC-AUTH-001 | MUST | No existe registro público; usuarios creados por administrador |
| SEC-AUTH-002 | MUST | Contraseñas codificadas con Argon2id y salt individual gestionado por el codificador |
| SEC-AUTH-003 | MUST | Parámetros Argon2id medidos y documentados para lograr costo defensivo sin degradar operación |
| SEC-AUTH-004 | MUST | Contraseñas nunca se registran, retornan ni almacenan en texto claro o cifrado reversible |
| SEC-AUTH-005 | MUST | Mensaje de login genérico para usuario inexistente, inactivo o contraseña incorrecta |
| SEC-AUTH-006 | MUST | Rate limit por cuenta y origen, con cuidado de no permitir bloqueo permanente provocado |
| SEC-AUTH-007 | MUST | Fallos repetidos, login exitoso y desactivación se auditan sin incluir credenciales |
| SEC-AUTH-008 | SHOULD | Longitud mínima inicial de 12 caracteres y longitud máxima segura; sin reglas de composición arbitrarias |
| SEC-AUTH-009 | MUST | El administrador inicial se crea mediante un procedimiento de bootstrap sin credenciales en migraciones o repositorio |
| SEC-AUTH-010 | MUST | Cambiar contraseña o desactivar usuario invalida sus sesiones existentes |
| SEC-AUTH-011 | LATER | MFA se reevaluará ante mayor exposición, usuarios o impacto |

La elección de Argon2id sigue la guía oficial [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). Los parámetros no se copiarán sin medir el hardware real.

## 5. Sesiones y CSRF

| ID | Nivel | Requisito |
|---|---|---|
| SEC-SESS-001 | MUST | Identificadores opacos, aleatorios y gestionados por Spring Session |
| SEC-SESS-002 | MUST | Cookie `HttpOnly`, `Secure` en público, `SameSite` y alcance mínimo de dominio/ruta |
| SEC-SESS-003 | MUST | Rotar identificador después de autenticación para evitar fijación |
| SEC-SESS-004 | MUST | Timeout de inactividad y máximo configurables; valores aprobados antes de staging |
| SEC-SESS-005 | MUST | Logout invalida servidor y cookie; cierre de navegador no es la única defensa |
| SEC-SESS-006 | MUST | Operaciones mutables requieren token CSRF válido |
| SEC-SESS-007 | MUST | Verificar `Origin`/`Referer` cuando corresponda como defensa adicional, no sustituta |
| SEC-SESS-008 | MUST | No almacenar sesión humana en `localStorage` o `sessionStorage` |
| SEC-SESS-009 | MUST | Sesiones y cookies completas nunca aparecen en logs |

Referencias: [OWASP Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html) y [OWASP CSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).

## 6. Autorización

| ID | Nivel | Requisito |
|---|---|---|
| SEC-AUTHZ-001 | MUST | Toda ruta privada exige autenticación por defecto |
| SEC-AUTHZ-002 | MUST | Autorización por rol en endpoint y caso de uso sensible |
| SEC-AUTHZ-003 | MUST | Ajustes, usuarios y anulaciones son exclusivos de administrador |
| SEC-AUTHZ-004 | MUST | Los identificadores recibidos no conceden acceso por sí solos |
| SEC-AUTHZ-005 | MUST | Pruebas positivas y negativas para cada rol y endpoint crítico |
| SEC-AUTHZ-006 | MUST | La interfaz no es una frontera de autorización |
| SEC-AUTHZ-007 | MUST | Fallos de autorización no revelan datos del recurso solicitado |

## 7. Entrada, salida y API

| ID | Nivel | Requisito |
|---|---|---|
| SEC-API-001 | MUST | HTTPS obligatorio en staging y producción |
| SEC-API-002 | MUST | Validación de tipos, rangos, longitudes, formatos y enumeraciones en backend |
| SEC-API-003 | MUST | Códigos de barra como texto con longitud máxima; montos/cantidades con límites |
| SEC-API-004 | MUST | Consultas parametrizadas; prohibido concatenar entrada en SQL |
| SEC-API-005 | MUST | Aceptar únicamente métodos y tipos de contenido esperados |
| SEC-API-006 | MUST | Límite de tamaño de request y paginación acotada |
| SEC-API-007 | MUST | Errores `problem+json` sin stack, SQL, clases internas o secretos |
| SEC-API-008 | MUST | CORS deshabilitado por defecto o allowlist exacta sin comodín con credenciales |
| SEC-API-009 | MUST | React escapa contenido; prohibido HTML inseguro salvo revisión explícita |
| SEC-API-010 | MUST | No se aceptan costo, rol, usuario auditor o total definitivo desde el cliente |

La API sigue la exigencia de HTTPS y control de acceso por endpoint descrita en [OWASP REST Security](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html).

## 8. Cabeceras del navegador

En ambientes públicos se configurarán y probarán:

- `Strict-Transport-Security` después de confirmar que el dominio funciona completamente por HTTPS.
- `Content-Security-Policy` restrictiva y compatible con Vite/React desplegado.
- `frame-ancestors 'none'` o equivalente para evitar framing.
- `X-Content-Type-Options: nosniff`.
- `Referrer-Policy` restrictiva.
- `Permissions-Policy`, habilitando cámara únicamente donde sea necesaria.

La configuración seguirá [OWASP HTTP Security Response Headers](https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html) y será verificada en staging, no copiada sin validar el hosting.

## 9. Reglas transaccionales y antifraude

| ID | Nivel | Requisito |
|---|---|---|
| SEC-BUS-001 | MUST | Backend resuelve precio, costo, estado y stock al confirmar |
| SEC-BUS-002 | MUST | Confirmación completa dentro de una sola transacción |
| SEC-BUS-003 | MUST | Bloqueos de inventario en orden estable y prueba concurrente real |
| SEC-BUS-004 | MUST | Clave de idempotencia única y hash de contenido |
| SEC-BUS-005 | MUST | Restricciones DB para stock no negativo y caja única |
| SEC-BUS-006 | MUST | Anulación solo compensatoria, administrativa, motivada y única |
| SEC-BUS-007 | MUST | Caja cerrada y movimientos históricos son inmutables |
| SEC-BUS-008 | MUST | Diferencias de caja se conservan y no se ocultan mediante edición |
| SEC-BUS-009 | MUST | La devolución en efectivo afecta únicamente una caja actualmente abierta |

## 10. Lector móvil

| ID | Nivel | Requisito |
|---|---|---|
| SEC-SCAN-001 | MUST | Token de QR con al menos 128 bits de entropía, corto, de un solo uso y almacenado como hash |
| SEC-SCAN-002 | MUST | Expiración inicial de solicitud de vinculación no superior a cinco minutos |
| SEC-SCAN-003 | MUST | Al consumir el QR se crea una sesión lectora distinta y el token se retira de la URL |
| SEC-SCAN-004 | MUST | Sesión lectora limitada a publicar códigos en una sesión POS concreta |
| SEC-SCAN-005 | MUST | Cierre de caja, revocación o expiración termina la sesión lectora |
| SEC-SCAN-006 | MUST | `eventId` único, rate limit, longitud máxima y rechazo de repetidos |
| SEC-SCAN-007 | MUST | Canal SSE disponible solo para el POS autenticado y vinculado |
| SEC-SCAN-008 | MUST | Teléfono nunca envía nombre, precio, stock, total o permisos confiables |
| SEC-SCAN-009 | MUST | Denegar framing y minimizar referrer en la página de vinculación |

## 11. Datos, base y backups

| ID | Nivel | Requisito |
|---|---|---|
| SEC-DATA-001 | MUST | Credenciales diferentes por ambiente |
| SEC-DATA-002 | MUST | Rol runtime sin permisos DDL; rol de migración separado cuando el hosting lo permita |
| SEC-DATA-003 | MUST | Base no expuesta públicamente salvo allowlist estrictamente necesaria |
| SEC-DATA-004 | MUST | TLS hacia PostgreSQL en staging/producción cuando sea remoto |
| SEC-DATA-005 | MUST | Migraciones Flyway revisadas, inmutables tras liberar y probadas desde base vacía |
| SEC-DATA-006 | MUST | Backups cifrados, con acceso mínimo y retención aprobada |
| SEC-DATA-007 | MUST | Restauración probada sin sobrescribir producción |
| SEC-DATA-008 | MUST | Datos de staging sintéticos y separados |
| SEC-DATA-009 | SHOULD | Prueba periódica de reconciliación entre saldo y movimientos de inventario |

## 12. Secretos y configuración

| ID | Nivel | Requisito |
|---|---|---|
| SEC-SECRET-001 | MUST | Secretos fuera del repositorio y de imágenes construidas |
| SEC-SECRET-002 | MUST | `.env.example` solo contiene nombres y valores ficticios |
| SEC-SECRET-003 | MUST | Escaneo de secretos en CI y, si es posible, pre-commit |
| SEC-SECRET-004 | MUST | Cada secreto tiene propósito, propietario y procedimiento de rotación |
| SEC-SECRET-005 | MUST | Una filtración obliga a revocar/rotar; borrar el commit no es tratamiento suficiente |
| SEC-SECRET-006 | MUST | Variables y perfiles de producción fallan de forma segura si falta configuración crítica |

## 13. Logs, auditoría y privacidad

| ID | Nivel | Requisito |
|---|---|---|
| SEC-LOG-001 | MUST | Correlation ID en solicitudes y eventos relevantes |
| SEC-LOG-002 | MUST | Auditar login, ajustes, anulaciones, caja, usuarios y vinculaciones |
| SEC-LOG-003 | MUST | No registrar contraseñas, cookies, tokens, hashes completos ni datos de cámara |
| SEC-LOG-004 | MUST | Acceso a logs limitado y retención definida |
| SEC-LOG-005 | MUST | Errores productivos no contienen stack en respuesta; stack interno se controla y redacta |
| SEC-LOG-006 | SHOULD | Alertar sobre patrones anómalos: múltiples fallos, anulaciones o rechazos de lector |

## 14. Dependencias y CI/CD

| ID | Nivel | Requisito |
|---|---|---|
| SEC-SUP-001 | MUST | Maven Wrapper y lockfile npm versionados |
| SEC-SUP-002 | MUST | Dependencias altas/críticas bloquean liberación salvo excepción documentada |
| SEC-SUP-003 | MUST | SAST, escaneo de dependencias y secretos en pull request |
| SEC-SUP-004 | MUST | Acciones de CI fijadas a versiones confiables, preferentemente commit SHA |
| SEC-SUP-005 | MUST | Tokens de CI con mínimo privilegio y sin acceso productivo innecesario |
| SEC-SUP-006 | MUST | Artefactos se construyen desde código revisado, no desde estaciones personales |
| SEC-SUP-007 | SHOULD | Actualizaciones automatizadas generan PR; nunca merge automático a principal |

## 15. Pruebas mínimas de seguridad

Antes de liberar:

- Matriz de autorización completa con casos negativos.
- CSRF en cada operación mutable y CORS desde un origen no autorizado.
- Manipulación de precio, total, rol y usuario auditor.
- SQL injection básica y límites de entrada.
- XSS en nombres, motivos y mensajes mostrados.
- Sesión expirada, logout y usuario desactivado.
- Fuerza bruta y rate limiting.
- Concurrencia, rollback e idempotencia.
- QR consumido, expirado, revocado y de otra sesión.
- Verificación de cabeceras, TLS y ausencia de secretos en logs.
- Escaneo de dependencias y restauración de backup.

## 16. Respuesta a incidentes mínima

Debe existir un procedimiento para:

1. Desactivar usuario e invalidar sesiones.
2. Revocar sesiones lectoras.
3. Rotar secretos expuestos.
4. Detener un despliegue o volver a una versión segura.
5. Preservar logs relevantes.
6. Evaluar integridad de ventas, caja e inventario.
7. Restaurar en ambiente aislado.
8. Documentar causa, impacto, corrección y prevención.

## 17. Infraestructura y control de costos

| ID | Nivel | Requisito |
|---|---|---|
| SEC-COST-001 | MUST | Desarrollo, CI, staging y primer despliegue usan recursos locales o planes gratuitos |
| SEC-COST-002 | MUST | Ningún recurso con cobro, autoescalado pagado o sobreconsumo facturable se habilita sin aprobación |
| SEC-COST-003 | MUST | Documentar cuotas, suspensión, caducidad, persistencia, backups y SLA del proveedor gratuito elegido |
| SEC-COST-004 | MUST | Si se exige medio de pago, solicitar aprobación y comprobar un límite efectivo de gasto antes de configurarlo |
| SEC-COST-005 | MUST | La limitación gratuita no permite omitir HTTPS, secretos seguros, separación de ambientes o restauración |
| SEC-COST-006 | MUST | Un despliegue gratuito que no cumpla controles mínimos se etiqueta como demo o staging, no producción |
| SEC-COST-007 | SHOULD | Alertar antes de agotar cuotas gratuitas cuando el proveedor entregue métricas apropiadas |

## 18. Excepciones

Toda excepción a un control `MUST` requiere:

- Control afectado.
- Motivo.
- Riesgo resultante.
- Medida compensatoria.
- Responsable.
- Fecha de expiración o revisión.
- Aprobación explícita del responsable técnico.

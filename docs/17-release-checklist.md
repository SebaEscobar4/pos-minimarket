# 17 - Checklist de liberación

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Propuesta para aprobación - Entrega 3

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Tipos de liberación

- **Interna:** desarrollo integrado, no desplegado públicamente.
- **Staging/demo:** ambiente gratuito con datos sintéticos.
- **Candidato productivo:** cumple controles y espera aprobación.
- **Producción:** uso real, únicamente después de aprobación explícita.
- **Hotfix:** corrección urgente y acotada con regresión mínima obligatoria.

## 2. Identificación

- [ ] Versión semántica definida.
- [ ] Commit/tag exacto registrado.
- [ ] Notas de versión redactadas.
- [ ] Cambios incluidos y fuera de alcance enumerados.
- [ ] Artefactos construidos por CI o procedimiento reproducible.

## 3. Calidad

- [ ] Compilación backend/frontend exitosa.
- [ ] Quality gate aprobado.
- [ ] Pruebas unitarias e integración aprobadas.
- [ ] E2E P0 aprobados.
- [ ] Regresión requerida aprobada.
- [ ] Cero pruebas inestables conocidas.
- [ ] Evidencia vinculada a la versión.

## 4. Defectos y riesgos

- [ ] Cero defectos críticos o altos abiertos.
- [ ] Defectos medios/bajos revisados y aceptados.
- [ ] Riesgos residuales documentados.
- [ ] Modelo de amenazas actualizado si cambió una frontera.
- [ ] Excepciones tienen responsable y vencimiento.

## 5. Seguridad

- [ ] Secret scan limpio.
- [ ] Dependencias sin vulnerabilidades altas/críticas sin tratamiento.
- [ ] Matriz de autorización aprobada.
- [ ] CSRF, CORS y cookies verificados en ambiente objetivo.
- [ ] HTTPS y cabeceras verificadas.
- [ ] Logs no exponen secretos/tokens.
- [ ] Credenciales rotadas si existió exposición.

## 6. Base de datos

- [ ] Flyway parte desde base vacía.
- [ ] Migración probada sobre copia representativa.
- [ ] Tiempo y locks evaluados.
- [ ] Backup previo disponible cuando corresponda.
- [ ] Restauración ensayada o vigente según calendario.
- [ ] Cambios compatibles con rollback de aplicación.
- [ ] No se editó una migración ya liberada.

## 7. Infraestructura gratuita y costos

- [ ] Recurso pertenece a plan gratuito documentado.
- [ ] No existe autoescalado o sobreconsumo pagado.
- [ ] No se agregó medio de pago sin aprobación.
- [ ] Cuotas, suspensión, persistencia y caducidad revisadas.
- [ ] Alertas de cuota configuradas si están disponibles.
- [ ] El ambiente se etiqueta demo/staging si no cumple producción.
- [ ] Ningún costo inesperado detectado.

## 8. Configuración

- [ ] Variables requeridas presentes.
- [ ] Secretos fuera de repositorio.
- [ ] Perfiles de ambiente correctos.
- [ ] Orígenes permitidos exactos.
- [ ] Zona horaria `America/Santiago` validada.
- [ ] Rate limits y timeouts revisados.
- [ ] Salud/observabilidad disponibles sin filtrar datos.

## 9. Plan de despliegue

- [ ] Responsable y ventana definidos.
- [ ] Orden de migración/backend/frontend documentado.
- [ ] Duración esperada.
- [ ] Smoke posterior preparado.
- [ ] Comunicación de indisponibilidad si aplica.
- [ ] Aprobación explícita obtenida.

## 10. Rollback

- [ ] Versión anterior disponible.
- [ ] Criterios para abortar definidos.
- [ ] Responsable autorizado.
- [ ] Compatibilidad de esquema confirmada.
- [ ] Procedimiento probado en staging.
- [ ] Datos creados durante versión fallida tratados explícitamente.

Se preferirán migraciones expand/contract. Una migración destructiva no puede depender únicamente de “volver a desplegar la versión anterior”.

## 11. Verificación posterior

- [ ] Salud backend/frontend.
- [ ] Login controlado.
- [ ] Búsqueda no destructiva.
- [ ] Consulta de caja/reportes autorizada.
- [ ] Sin errores anómalos en logs.
- [ ] Métricas y conexiones normales.
- [ ] Versión visible/identificable.

Una venta real de prueba solo se realiza si el negocio la autoriza y existe un procedimiento claro de anulación; se prefiere smoke no destructivo.

## 12. Monitoreo inicial

- [ ] Periodo de observación definido.
- [ ] Errores, latencia y cuota revisados.
- [ ] Rechazos de stock/idempotencia observados.
- [ ] Diferencias o reconciliaciones revisadas.
- [ ] Decisión de mantener o revertir registrada.

## 13. Cierre

- [ ] Release marcado como exitoso, fallido o revertido.
- [ ] Evidencia y notas actualizadas.
- [ ] Incidentes/defectos creados.
- [ ] Próximas acciones asignadas.

## 14. Autoridad

Solo el responsable técnico usuario aprueba producción, rollback destructivo o recursos pagados. La automatización prepara y verifica, pero no toma esa decisión.


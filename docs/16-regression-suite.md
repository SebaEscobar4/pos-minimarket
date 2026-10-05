# 16 - Suite de regresión

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** Propuesta para aprobación - Entrega 3

**Versión:** 0.1

**Fecha:** 2026-07-31

## 1. Objetivo

Proteger los comportamientos ya aprobados cuando cambie un módulo. La suite se organiza por criticidad y se selecciona según impacto, sin perder un núcleo obligatorio.

## 2. Niveles

- **Smoke:** confirma que la versión puede evaluarse.
- **P0 crítico:** ventas, inventario, caja, seguridad y restauración.
- **P1 principal:** catálogo, historial, anulaciones, reportes y lector.
- **P2 ampliado:** compatibilidad, visual y casos poco frecuentes.

## 3. Smoke

| ID | Caso | Automatización |
|---|---|---|
| SMK-001 | Backend y frontend saludables | Automática |
| SMK-002 | Migraciones aplicadas | Automática |
| SMK-003 | Login administrador | E2E |
| SMK-004 | Buscar producto | E2E |
| SMK-005 | Consultar caja activa | API/E2E |
| SMK-006 | Abrir página de nueva venta | E2E |
| SMK-007 | Error seguro y correlacionado | API |
| SMK-008 | Sin costos/recursos inesperados en despliegue | Manual checklist |

Si falla un smoke, la regresión se suspende.

## 4. Regresión P0

| ID | Flujo | Resultado clave |
|---|---|---|
| REG-P0-001 | Login/logout | Sesión creada e invalidada |
| REG-P0-002 | Vendedor intenta acción admin | Acceso denegado sin efecto |
| REG-P0-003 | Apertura única | Solo una caja abierta |
| REG-P0-004 | Venta efectivo | Venta, inventario, caja y vuelto correctos |
| REG-P0-005 | Venta tarjeta | Sin aumento de efectivo |
| REG-P0-006 | Venta transferencia | Sin aumento de efectivo |
| REG-P0-007 | Venta sin caja | Rechazada sin efectos |
| REG-P0-008 | Última unidad concurrente | Una venta, stock cero |
| REG-P0-009 | Doble confirmación | Una venta |
| REG-P0-010 | Payload con precio alterado | Backend usa precio vigente |
| REG-P0-011 | Falla intermedia | Rollback completo |
| REG-P0-012 | Cierre de caja | Esperado, contado y diferencia correctos |
| REG-P0-013 | Anulación efectiva | Reposición y salida de caja actual |
| REG-P0-014 | Segunda anulación | Rechazada sin efectos |
| REG-P0-015 | Caja cerrada inmutable | No acepta cambios |
| REG-P0-016 | Migración base vacía | Exitosa |
| REG-P0-017 | Restauración | Datos críticos recuperados |
| REG-P0-018 | CSRF/CORS | Solicitudes indebidas rechazadas |

## 5. Regresión P1

| ID | Área | Caso |
|---|---|---|
| REG-P1-001 | Catálogo | Crear producto sin código |
| REG-P1-002 | Catálogo | Código único y ceros iniciales |
| REG-P1-003 | Búsqueda | Código exacto |
| REG-P1-004 | Búsqueda | Nombre parcial sin tildes/mayúsculas |
| REG-P1-005 | Catálogo | Inactivo excluido del POS |
| REG-P1-006 | Inventario | Entrada y saldo |
| REG-P1-007 | Inventario | Ajuste con motivo |
| REG-P1-008 | Inventario | Dañado/vencido |
| REG-P1-009 | Caja | Ingreso y retiro manual |
| REG-P1-010 | Historial | Precio/nombre históricos estables |
| REG-P1-011 | Historial | Filtros y zona horaria |
| REG-P1-012 | Reporte | Totales por método |
| REG-P1-013 | Reporte | Stock bajo |
| REG-P1-014 | Reporte | Ganancia estimada |
| REG-P1-015 | Lector | Vinculación válida |
| REG-P1-016 | Lector | QR expirado/consumido |
| REG-P1-017 | Lector | Evento repetido |
| REG-P1-018 | Lector | Evento solo al POS vinculado |
| REG-P1-019 | Sesión | Usuario desactivado pierde acceso |
| REG-P1-020 | Auditoría | Actor/fecha en operación crítica |

## 6. Regresión manual P2

- Navegadores/dispositivos disponibles.
- Cámara con diferentes condiciones.
- Conectividad inestable y reconexión.
- Pantallas pequeñas, zoom y rotación.
- Teclado, foco y lector de pantalla básico.
- Textos, alineación y estados visuales.
- Jornada completa simulada.

## 7. Selección por impacto

| Cambio | Suite mínima adicional |
|---|---|
| Catálogo/búsqueda | P1 catálogo + venta P0 |
| Inventario | Todo P0 de venta/anulación + P1 inventario |
| Caja | Todo P0 caja/venta/anulación |
| Seguridad | Matriz de roles, sesión, CSRF/CORS y P0 |
| Persistencia/migración | Integración completa, base vacía y restauración |
| Lector | P1 lector + búsqueda/venta |
| Frontend compartido | Smoke, accesibilidad y flujos afectados |

El núcleo P0 nunca se omite en un candidato a release.

## 8. Mantenimiento

- Caso obsoleto se actualiza junto con la regla aprobada.
- Caso duplicado se consolida sin perder cobertura.
- Caso inestable abre defecto y bloquea hasta estabilizar o reemplazar.
- Defecto productivo agrega caso a P0/P1 según impacto.
- Tiempo de suite se mide para mantener feedback útil.

## 9. Entrada y salida

Entrada: candidato identificable, smoke aprobado, ambiente estable y datos preparados. Salida: 100 % de suite requerida aprobada, defectos clasificados y evidencia asociada a versión.


# 22 - Iteración 2: catálogo y búsqueda

**Proyecto:** Sistema POS e inventario para minimarket familiar

**Estado:** US-020 a US-025 implementadas y validadas

**Fecha:** 2026-08-04

## Alcance implementado

- US-020: alta, edición, activación, desactivación y filtro explícito de categorías inactivas.
- Solo un administrador autenticado y con contraseña definitiva puede usar la administración de categorías.
- Flyway V3 crea `catalog_category` con estado lógico, fechas y usuario creador/modificador. No existe una operación de borrado físico.
- El frontend administrativo permite crear, renombrar, activar, desactivar y consultar categorías inactivas.
- Los errores de validación conservan el contrato común `application/problem+json` con código y correlación.
- US-021: productos con código opcional y único, precios decimales exactos, stock mínimo y estado lógico.
- US-022: cada producto crea un único `inventory_balance` con cantidad y versión cero en la misma transacción. Editar el producto no acepta ni modifica stock.
- US-023: búsqueda activa por código textual exacto después de recortar únicamente sus extremos.
- US-024: búsqueda parcial por nombre sin distinguir mayúsculas ni acentos.
- US-025: las búsquedas de venta excluyen inactivos; administración puede incluirlos explícitamente.
- Flyway V4 crea productos, restricciones de código/precio/cantidad e inventario inicial.
- El frontend permite administrar productos y buscar por código o nombre. El resultado de venta obtiene precio y saldo del backend; saldo cero se muestra no disponible.

## API incorporada

| Método | Ruta | Resultado |
|---|---|---|
| `POST` | `/api/v1/admin/catalog/categories` | Crea una categoría activa |
| `GET` | `/api/v1/admin/catalog/categories?includeInactive=false` | Lista categorías y excluye inactivas por defecto |
| `PUT` | `/api/v1/admin/catalog/categories/{id}` | Renombra una categoría |
| `PATCH` | `/api/v1/admin/catalog/categories/{id}/status` | Activa o desactiva una categoría |
| `POST` | `/api/v1/admin/catalog/products` | Crea producto y saldo cero atómicamente |
| `GET` | `/api/v1/admin/catalog/products` | Busca y filtra productos para administración |
| `PUT` | `/api/v1/admin/catalog/products/{id}` | Edita datos comerciales sin aceptar stock |
| `PATCH` | `/api/v1/admin/catalog/products/{id}/status` | Activa o desactiva un producto |
| `GET` | `/api/v1/catalog/products/by-code?code=...` | Resuelve un producto activo por código exacto |
| `GET` | `/api/v1/catalog/products/search?name=...` | Busca productos activos por nombre parcial |

El nombre se recorta en sus extremos, es obligatorio y admite hasta 100 caracteres. No se añadió unicidad de nombre porque las reglas aceptadas no la establecen.

## Evidencia ejecutable

- Pruebas unitarias de normalización, validación, creación, edición, estado, filtro y recursos inexistentes.
- `ApplicationContextIT` aplica cuatro migraciones sobre PostgreSQL limpio y prueba autorización ADMIN/SELLER, CSRF, altas, edición sin cambio de stock, duplicados, saldo cero, código desconocido sin efectos, búsquedas, desactivación, filtros y límites de página.
- Pruebas frontend cubren los contratos HTTP, administración, búsqueda de venta y disponibilidad por saldo.

## Decisión resuelta OQ-04

El usuario aprobó el 2026-08-04:

- longitud de 1 a 64 caracteres cuando el código esté informado;
- letras, números y los separadores `-`, `.`, `_` y `/`;
- sin espacios internos ni caracteres de control;
- recorte únicamente de espacios externos;
- conservación exacta de mayúsculas y ceros iniciales;
- unicidad sobre el valor textual almacenado.

La migración, el backend, el frontend y las pruebas aplican esta decisión sin convertir el código a número.

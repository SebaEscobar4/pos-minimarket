# Límites de funcionalidades

Cada funcionalidad tendrá su propia carpeta: `auth`, `catalog`, `inventory`, `cash`, `sales`, `scanner` y `reporting`.

Una funcionalidad puede importar desde `shared`, pero no desde la implementación interna de otra. La composición entre funcionalidades vive en `src/app`. Las carpetas se crearán al implementar su primera historia para evitar estructura vacía y abstracciones anticipadas.

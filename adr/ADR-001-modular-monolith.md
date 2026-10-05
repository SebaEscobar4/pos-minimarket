# ADR-001 — Monolito modular

**Estado:** Aceptado

**Fecha:** 2026-07-31

## Contexto

El sistema opera una sucursal, una caja y pocos usuarios. Ventas, stock y caja deben confirmarse atómicamente. Separarlos en servicios desplegables aumentaría fallos distribuidos, operación y pruebas sin resolver una necesidad actual.

## Decisión

Construir un backend Spring Boot único, organizado en módulos `identity`, `catalog`, `inventory`, `cash`, `sales`, `scanner` y `reporting`, con una base PostgreSQL.

Los límites se protegerán mediante paquetes, interfaces de aplicación, visibilidad de Java y pruebas ArchUnit. No se compartirán repositorios, controladores o entidades mutables entre módulos.

## Consecuencias

### Positivas

- Transacciones locales simples y confiables.
- Despliegue, observabilidad y respaldo sencillos.
- Menor costo operacional.
- Posibilidad de separar un módulo en el futuro si existe evidencia.

### Costos

- La disciplina modular depende de reglas y pruebas internas.
- Un despliegue actualiza todo el backend.
- Reporting debe evitar consultas que acoplen indebidamente módulos.

## Alternativas descartadas

- Microservicios: complejidad y consistencia distribuida innecesarias.
- Monolito sin módulos: más rápido al inicio, pero propenso a acoplamiento y difícil aprendizaje.
- Hexagonal estricta en todo: exceso de adaptadores y modelos duplicados para CRUD simple.

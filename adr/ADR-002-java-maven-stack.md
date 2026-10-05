# ADR-002 — Java 21 y Maven

**Estado:** Aceptado

**Fecha:** 2026-07-31

## Contexto

Se necesita una base reproducible, ampliamente documentada y adecuada para Spring Boot. El usuario quiere comprender y explicar cada módulo, por lo que la previsibilidad del build tiene mayor valor que una DSL flexible.

## Decisión

- Java 21 LTS.
- Spring Boot estable y soportado al comenzar la implementación.
- Maven mediante Maven Wrapper.
- PostgreSQL y Flyway.
- Sin Lombok inicialmente, para mantener explícitos constructores, invariantes y comportamiento.

Las versiones exactas se fijarán en la primera historia técnica y se actualizarán mediante PR.

## Consecuencias

### Positivas

- Build convencional y fácil de reproducir en CI.
- Compatibilidad LTS y acceso a características modernas de Java.
- Menor variación entre entorno local y pipeline.

### Costos

- XML de Maven más verboso que Gradle.
- Algo más de código explícito al no usar Lombok.

## Alternativas descartadas

- Gradle: válido, pero su flexibilidad no aporta una ventaja concreta aquí.
- Java no LTS: ciclo de actualización innecesario.
- Lombok desde el inicio: reduce texto, pero oculta comportamiento útil durante aprendizaje y revisión.

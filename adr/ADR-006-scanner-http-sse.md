# ADR-006 — Lector mediante HTTP y SSE

**Estado:** Aceptado

**Fecha:** 2026-07-31

## Contexto

El teléfono únicamente envía códigos; el computador necesita recibir eventos en tiempo real. No existe comunicación bidireccional compleja ni necesidad de broker.

## Decisión

- El teléfono envía cada evento mediante `POST` HTTPS.
- El POS recibe eventos mediante Server-Sent Events.
- QR con token de un solo uso intercambiado por sesión lectora restringida.
- `eventId` único, expiración, revocación y rate limiting.
- El lector nunca resuelve datos de producto ni confirma ventas.

## Consecuencias

### Positivas

- Protocolo simple de depurar y asegurar.
- Reconexión SSE incorporada en navegadores.
- Compatible con infraestructura HTTP común.

### Costos

- Canal solo servidor hacia cliente; cambios futuros bidireccionales podrían requerir WebSocket.
- Deben probarse timeouts y buffering del proveedor de hosting.
- Requiere heartbeats y control de reconexión.

## Alternativas descartadas

- WebSocket: posible, pero agrega complejidad que el flujo actual no utiliza.
- Polling: mayor latencia y tráfico innecesario.
- Bluetooth o aplicación nativa: fuera del alcance web.

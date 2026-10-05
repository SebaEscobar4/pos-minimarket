package com.minimarket.pos.shared.application.error;

public enum ProblemType {
    VALIDATION("validation", "La solicitud contiene datos inválidos."),
    AUTHENTICATION("authentication-failed", "Usuario o contraseña inválidos."),
    RATE_LIMIT("authentication-rate-limited", "Demasiados intentos. Intenta nuevamente más tarde."),
    NOT_FOUND("not-found", "El recurso solicitado no existe."),
    CONFLICT("conflict", "La operación entra en conflicto con el estado actual."),
    FORBIDDEN("forbidden", "No tienes permiso para ejecutar esta operación.");

    private final String code;
    private final String defaultMessage;

    ProblemType(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}

package com.minimarket.pos.shared.application.error;

import java.util.Objects;

public final class ApplicationException extends RuntimeException {

    private final ProblemType problemType;

    public ApplicationException(ProblemType problemType) {
        this(problemType, problemType.defaultMessage());
    }

    public ApplicationException(ProblemType problemType, String safeMessage) {
        super(requireMessage(safeMessage));
        this.problemType = Objects.requireNonNull(problemType, "problemType is required");
    }

    public ProblemType problemType() {
        return problemType;
    }

    private static String requireMessage(String safeMessage) {
        if (safeMessage == null || safeMessage.isBlank()) {
            throw new IllegalArgumentException("safeMessage is required");
        }
        return safeMessage;
    }
}

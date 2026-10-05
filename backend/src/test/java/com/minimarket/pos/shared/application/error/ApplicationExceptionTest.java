package com.minimarket.pos.shared.application.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ApplicationExceptionTest {

    @Test
    void usesProblemDefaultMessage() {
        ApplicationException exception = new ApplicationException(ProblemType.CONFLICT);

        assertThat(exception.problemType()).isEqualTo(ProblemType.CONFLICT);
        assertThat(exception.getMessage()).isEqualTo(ProblemType.CONFLICT.defaultMessage());
    }

    @Test
    void rejectsMissingProblemType() {
        assertThatThrownBy(() -> new ApplicationException(null, "Mensaje seguro"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("problemType is required");
    }

    @Test
    void rejectsBlankSafeMessage() {
        assertThatThrownBy(() -> new ApplicationException(ProblemType.VALIDATION, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("safeMessage is required");
    }
}

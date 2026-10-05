package com.minimarket.pos.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import org.junit.jupiter.api.Test;

class CategoryInputRulesTest {

    @Test
    void trimsExternalWhitespaceFromAValidName() {
        assertThat(CategoryInputRules.normalizeName("  Bebidas frías  ")).isEqualTo("Bebidas frías");
    }

    @Test
    void rejectsNullBlankAndOversizedNames() {
        assertInvalid(null);
        assertInvalid("   ");
        assertInvalid("a".repeat(101));
    }

    private void assertInvalid(String value) {
        assertThatThrownBy(() -> CategoryInputRules.normalizeName(value))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).problemType())
                .isEqualTo(ProblemType.VALIDATION);
    }
}

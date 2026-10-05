package com.minimarket.pos.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minimarket.pos.shared.application.error.ApplicationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductInputRulesTest {

    @Test
    void preservesTextualCodesAndLeadingZeroesAfterExternalTrim() {
        assertThat(ProductInputRules.normalizeCode(" 00123 ")).isEqualTo("00123");
        assertThat(ProductInputRules.normalizeCode("Ab-1._/Z")).isEqualTo("Ab-1._/Z");
        assertThat(ProductInputRules.normalizeCode("a".repeat(64))).hasSize(64);
        assertThat(ProductInputRules.normalizeCode(null)).isNull();
        assertThat(ProductInputRules.normalizeCode("   ")).isNull();
    }

    @Test
    void rejectsInvalidCodeCharactersAndLength() {
        assertInvalid(() -> ProductInputRules.normalizeCode("ABC 123"));
        assertInvalid(() -> ProductInputRules.normalizeCode("á123"));
        assertInvalid(() -> ProductInputRules.normalizeCode("a".repeat(65)));
    }

    @Test
    void normalizesNamesForAccentInsensitivePartialSearch() {
        assertThat(ProductInputRules.normalizeName("  Café   Molido  ")).isEqualTo("Café Molido");
        assertThat(ProductInputRules.searchName("CAFÉ molido")).isEqualTo("cafe molido");
        assertThat(ProductInputRules.searchName("Piñata")).isEqualTo("pinata");
    }

    @Test
    void validatesNamesMoneyStockAndPaginationBoundaries() {
        assertThat(ProductInputRules.money("Precio", BigDecimal.ZERO))
                .isEqualByComparingTo("0.00");
        assertThat(ProductInputRules.money("Precio", new BigDecimal("9999999999.99")))
                .isEqualByComparingTo("9999999999.99");
        assertThat(ProductInputRules.minimumStock(1_000_000)).isEqualTo(1_000_000);
        ProductInputRules.validatePage(0, 100);

        assertInvalid(() -> ProductInputRules.normalizeName(null));
        assertInvalid(() -> ProductInputRules.normalizeName(" "));
        assertInvalid(() -> ProductInputRules.normalizeName("a".repeat(151)));
        assertInvalid(() -> ProductInputRules.money("Precio", null));
        assertInvalid(() -> ProductInputRules.money("Precio", new BigDecimal("-0.01")));
        assertInvalid(() -> ProductInputRules.money("Precio", new BigDecimal("1.001")));
        assertInvalid(() -> ProductInputRules.money("Precio", new BigDecimal("10000000000")));
        assertInvalid(() -> ProductInputRules.minimumStock(null));
        assertInvalid(() -> ProductInputRules.minimumStock(-1));
        assertInvalid(() -> ProductInputRules.minimumStock(1_000_001));
        assertInvalid(() -> ProductInputRules.validatePage(-1, 20));
        assertInvalid(() -> ProductInputRules.validatePage(0, 0));
        assertInvalid(() -> ProductInputRules.validatePage(0, 101));
    }

    private void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(ApplicationException.class);
    }
}

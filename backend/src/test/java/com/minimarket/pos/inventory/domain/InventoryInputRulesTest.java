package com.minimarket.pos.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minimarket.pos.shared.application.error.ApplicationException;
import org.junit.jupiter.api.Test;

class InventoryInputRulesTest {

    @Test
    void validatesQuantityBoundariesAndMovementDirections() {
        assertThat(InventoryInputRules.quantity(1)).isEqualTo(1);
        assertThat(InventoryInputRules.quantity(1_000_000)).isEqualTo(1_000_000);
        assertThat(InventoryMovementType.ENTRY.deltaFor(3)).isEqualTo(3);
        assertThat(InventoryMovementType.ADJUSTMENT_OUT.deltaFor(3)).isEqualTo(-3);

        assertInvalid(() -> InventoryInputRules.quantity(null));
        assertInvalid(() -> InventoryInputRules.quantity(0));
        assertInvalid(() -> InventoryInputRules.quantity(1_000_001));
    }

    @Test
    void requiresReasonOrReferenceForEntriesAndReasonForAdjustments() {
        assertThat(InventoryInputRules.entryEvidence(" Recepción ", null).reason())
                .isEqualTo("Recepción");
        assertThat(InventoryInputRules.entryEvidence(null, " FAC-001 ").reference())
                .isEqualTo("FAC-001");
        assertThat(InventoryInputRules.requiredReason(" Conteo físico "))
                .isEqualTo("Conteo físico");

        assertInvalid(() -> InventoryInputRules.entryEvidence(" ", null));
        assertInvalid(() -> InventoryInputRules.requiredReason(null));
        assertInvalid(() -> InventoryInputRules.requiredReason("a".repeat(501)));
        assertInvalid(() -> InventoryInputRules.entryEvidence(null, "a".repeat(101)));
    }

    @Test
    void capsLedgerPagination() {
        InventoryInputRules.validatePage(0, 100);
        assertInvalid(() -> InventoryInputRules.validatePage(-1, 20));
        assertInvalid(() -> InventoryInputRules.validatePage(0, 0));
        assertInvalid(() -> InventoryInputRules.validatePage(0, 101));
    }

    private void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(ApplicationException.class);
    }
}

package com.minimarket.pos.sales.application;

import com.minimarket.pos.sales.domain.PaymentMethod;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

final class SaleRequestHasher {

    private SaleRequestHasher() {}

    static String hash(
            List<NormalizedLine> lines, PaymentMethod method, BigDecimal normalizedCashReceived) {
        StringBuilder canonical = new StringBuilder("v1|")
                .append(method.name())
                .append('|')
                .append(normalizedCashReceived == null ? "-" : normalizedCashReceived.toPlainString())
                .append('|');
        for (NormalizedLine line : lines) {
            canonical.append(line.productId()).append(':').append(line.quantity()).append(';');
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    record NormalizedLine(java.util.UUID productId, int quantity) {}
}

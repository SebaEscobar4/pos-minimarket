package com.minimarket.pos.cash.application;

import com.minimarket.pos.cash.domain.CashMovement;
import com.minimarket.pos.cash.domain.CashSession;
import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CashSessionRepository {

    Optional<CashSession> findOpen();

    Optional<CashSession> lockOpen();

    boolean create(CashSession session);

    void append(CashMovement movement);

    CashTotals totals(UUID sessionId);

    CashPage<CashMovement> movements(UUID sessionId, int page, int size);

    boolean close(
            UUID sessionId,
            UUID actorId,
            Instant closedAt,
            Money countedCash,
            Money expectedCash,
            BigDecimal difference);
}

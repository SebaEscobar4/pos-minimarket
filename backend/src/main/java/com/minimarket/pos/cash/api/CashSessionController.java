package com.minimarket.pos.cash.api;

import com.minimarket.pos.cash.api.CashMovementController.CashMovementResponse;
import com.minimarket.pos.cash.application.CashCloseResult;
import com.minimarket.pos.cash.application.CashPage;
import com.minimarket.pos.cash.application.CashSessionService;
import com.minimarket.pos.cash.application.CashSummary;
import com.minimarket.pos.cash.application.CashTotals;
import com.minimarket.pos.cash.domain.CashMovement;
import com.minimarket.pos.cash.domain.CashSession;
import com.minimarket.pos.cash.domain.CashSessionStatus;
import com.minimarket.pos.identity.application.IdentityPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cash/sessions")
public class CashSessionController {

    private final CashSessionService service;

    public CashSessionController(CashSessionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CashSessionResponse open(
            @Valid @RequestBody OpenCashSessionRequest request, Authentication authentication) {
        IdentityPrincipal actor = actor(authentication);
        return CashSessionResponse.from(
                service.open(request.openingAmount(), actor.id(), actor.displayName()));
    }

    @GetMapping("/current")
    public CurrentCashSessionResponse current() {
        return new CurrentCashSessionResponse(
                service.current().map(CashSessionResponse::from).orElse(null));
    }

    @GetMapping("/current/summary")
    public CashSummaryResponse summary(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return CashSummaryResponse.from(service.summary(page, size));
    }

    @PostMapping("/current/close")
    public CashCloseResponse close(
            @Valid @RequestBody CloseCashSessionRequest request,
            Authentication authentication) {
        IdentityPrincipal actor = actor(authentication);
        return CashCloseResponse.from(
                service.close(request.countedCash(), actor.id(), actor.displayName()));
    }

    private IdentityPrincipal actor(Authentication authentication) {
        return (IdentityPrincipal) authentication.getPrincipal();
    }

    public record OpenCashSessionRequest(
            @NotNull(message = "El monto inicial es obligatorio.") BigDecimal openingAmount) {}

    public record CloseCashSessionRequest(
            @NotNull(message = "El efectivo contado es obligatorio.") BigDecimal countedCash) {}

    public record CurrentCashSessionResponse(CashSessionResponse session) {}

    public record CashSessionResponse(
            UUID id,
            UUID openedBy,
            String openedByDisplayName,
            Instant openedAt,
            BigDecimal openingAmount,
            CashSessionStatus status) {

        static CashSessionResponse from(CashSession session) {
            return new CashSessionResponse(
                    session.id(),
                    session.openedBy(),
                    session.openedByDisplayName(),
                    session.openedAt(),
                    session.openingAmount().amount(),
                    session.status());
        }
    }

    public record CashTotalsResponse(
            BigDecimal cashSales,
            BigDecimal manualIncome,
            BigDecimal manualWithdrawals,
            BigDecimal cashRefunds,
            BigDecimal cardSales,
            BigDecimal transferSales) {

        static CashTotalsResponse from(CashTotals totals) {
            return new CashTotalsResponse(
                    totals.cashSales().amount(),
                    totals.manualIncome().amount(),
                    totals.manualWithdrawals().amount(),
                    totals.cashRefunds().amount(),
                    totals.cardSales().amount(),
                    totals.transferSales().amount());
        }
    }

    public record CashMovementPageResponse(
            java.util.List<CashMovementResponse> items,
            int page,
            int size,
            long totalElements,
            long totalPages) {

        static CashMovementPageResponse from(CashPage<CashMovement> movements) {
            return new CashMovementPageResponse(
                    movements.items().stream().map(CashMovementResponse::from).toList(),
                    movements.page(),
                    movements.size(),
                    movements.totalElements(),
                    movements.totalPages());
        }
    }

    public record CashSummaryResponse(
            CashSessionResponse session,
            CashTotalsResponse totals,
            BigDecimal expectedCash,
            CashMovementPageResponse movements) {

        static CashSummaryResponse from(CashSummary summary) {
            return new CashSummaryResponse(
                    CashSessionResponse.from(summary.session()),
                    CashTotalsResponse.from(summary.totals()),
                    summary.expectedCash().amount(),
                    CashMovementPageResponse.from(summary.movements()));
        }
    }

    public record CashCloseResponse(
            UUID sessionId,
            UUID closedBy,
            String closedByDisplayName,
            Instant closedAt,
            BigDecimal openingAmount,
            BigDecimal expectedCash,
            BigDecimal countedCash,
            BigDecimal difference,
            CashTotalsResponse totals) {

        static CashCloseResponse from(CashCloseResult result) {
            return new CashCloseResponse(
                    result.sessionId(),
                    result.closedBy(),
                    result.closedByDisplayName(),
                    result.closedAt(),
                    result.openingAmount().amount(),
                    result.expectedCash().amount(),
                    result.countedCash().amount(),
                    result.difference(),
                    CashTotalsResponse.from(result.totals()));
        }
    }
}

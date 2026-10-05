package com.minimarket.pos.cash.api;

import com.minimarket.pos.cash.application.CashSessionService;
import com.minimarket.pos.cash.domain.CashMovement;
import com.minimarket.pos.cash.domain.CashMovementCategory;
import com.minimarket.pos.cash.domain.CashMovementType;
import com.minimarket.pos.cash.domain.ManualCashDirection;
import com.minimarket.pos.identity.application.IdentityPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/cash/movements")
public class CashMovementController {

    private final CashSessionService service;

    public CashMovementController(CashSessionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CashMovementResponse create(
            @Valid @RequestBody ManualCashMovementRequest request,
            Authentication authentication) {
        IdentityPrincipal actor = (IdentityPrincipal) authentication.getPrincipal();
        return CashMovementResponse.from(service.recordManualMovement(
                request.direction(),
                request.category(),
                request.amount(),
                request.reason(),
                actor.id(),
                actor.displayName()));
    }

    public record ManualCashMovementRequest(
            @NotNull(message = "El tipo de movimiento es obligatorio.")
                    ManualCashDirection direction,
            @NotNull(message = "La categoría es obligatoria.") CashMovementCategory category,
            @NotNull(message = "El monto es obligatorio.") BigDecimal amount,
            @NotBlank(message = "El motivo es obligatorio.")
                    @Size(max = 500, message = "El motivo admite hasta 500 caracteres.")
                    String reason) {}

    public record CashMovementResponse(
            UUID id,
            UUID cashSessionId,
            CashMovementType type,
            CashMovementCategory category,
            BigDecimal amount,
            String reason,
            String reference,
            UUID actorId,
            String actorDisplayName,
            Instant occurredAt) {

        static CashMovementResponse from(CashMovement movement) {
            return new CashMovementResponse(
                    movement.id(),
                    movement.cashSessionId(),
                    movement.type(),
                    movement.category(),
                    movement.amount().amount(),
                    movement.reason(),
                    movement.reference(),
                    movement.actorId(),
                    movement.actorDisplayName(),
                    movement.occurredAt());
        }
    }
}

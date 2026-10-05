package com.minimarket.pos.inventory.api;

import com.minimarket.pos.identity.application.IdentityPrincipal;
import com.minimarket.pos.inventory.application.InventoryPage;
import com.minimarket.pos.inventory.application.InventoryService;
import com.minimarket.pos.inventory.application.InventorySnapshot;
import com.minimarket.pos.inventory.domain.AdjustmentDirection;
import com.minimarket.pos.inventory.domain.InventoryMovement;
import com.minimarket.pos.inventory.domain.InventoryMovementType;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse entry(
            @Valid @RequestBody EntryRequest request, Authentication authentication) {
        IdentityPrincipal actor = actor(authentication);
        return MovementResponse.from(service.recordEntry(
                request.productId(),
                request.quantity(),
                request.reason(),
                request.reference(),
                actor.id(),
                actor.displayName()));
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse adjustment(
            @Valid @RequestBody AdjustmentRequest request, Authentication authentication) {
        IdentityPrincipal actor = actor(authentication);
        return MovementResponse.from(service.recordAdjustment(
                request.productId(),
                request.direction(),
                request.quantity(),
                request.reason(),
                actor.id(),
                actor.displayName()));
    }

    @GetMapping("/products/{productId}")
    public SnapshotResponse snapshot(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return SnapshotResponse.from(service.snapshot(productId, page, size));
    }

    private IdentityPrincipal actor(Authentication authentication) {
        return (IdentityPrincipal) authentication.getPrincipal();
    }

    public record EntryRequest(
            UUID productId, Integer quantity, String reason, String reference) {}

    public record AdjustmentRequest(
            UUID productId, AdjustmentDirection direction, Integer quantity, String reason) {}

    public record MovementResponse(
            UUID id,
            UUID productId,
            InventoryMovementType type,
            int quantity,
            int delta,
            int previousBalance,
            int resultingBalance,
            String reason,
            String reference,
            UUID actorId,
            String actorDisplayName,
            Instant occurredAt) {

        static MovementResponse from(InventoryMovement movement) {
            return new MovementResponse(
                    movement.id(),
                    movement.productId(),
                    movement.type(),
                    movement.quantity(),
                    movement.delta(),
                    movement.previousBalance(),
                    movement.resultingBalance(),
                    movement.reason(),
                    movement.reference(),
                    movement.actorId(),
                    movement.actorDisplayName(),
                    movement.occurredAt());
        }
    }

    public record MovementPageResponse(
            List<MovementResponse> items,
            int page,
            int size,
            long totalElements,
            long totalPages) {

        static MovementPageResponse from(InventoryPage<InventoryMovement> movements) {
            return new MovementPageResponse(
                    movements.items().stream().map(MovementResponse::from).toList(),
                    movements.page(),
                    movements.size(),
                    movements.totalElements(),
                    movements.totalPages());
        }
    }

    public record SnapshotResponse(
            UUID productId,
            String productCode,
            String productName,
            int currentBalance,
            long version,
            MovementPageResponse movements) {

        static SnapshotResponse from(InventorySnapshot snapshot) {
            return new SnapshotResponse(
                    snapshot.productId(),
                    snapshot.productCode(),
                    snapshot.productName(),
                    snapshot.currentBalance(),
                    snapshot.version(),
                    MovementPageResponse.from(snapshot.movements()));
        }
    }
}

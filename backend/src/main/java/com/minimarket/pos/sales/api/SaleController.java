package com.minimarket.pos.sales.api;

import com.minimarket.pos.identity.application.IdentityPrincipal;
import com.minimarket.pos.sales.application.SaleCommand;
import com.minimarket.pos.sales.application.SaleConfirmation;
import com.minimarket.pos.sales.application.SalePage;
import com.minimarket.pos.sales.application.SaleService;
import com.minimarket.pos.sales.domain.Payment;
import com.minimarket.pos.sales.domain.PaymentMethod;
import com.minimarket.pos.sales.domain.Sale;
import com.minimarket.pos.sales.domain.SaleCancellation;
import com.minimarket.pos.sales.domain.SaleLine;
import com.minimarket.pos.sales.domain.SaleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {

    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SaleResponse> confirm(
            @Valid @RequestBody ConfirmSaleRequest request, Authentication authentication) {
        IdentityPrincipal actor = (IdentityPrincipal) authentication.getPrincipal();
        SaleConfirmation confirmation = service.confirm(
                request.toCommand(), actor.id(), actor.displayName());
        SaleResponse response = SaleResponse.from(confirmation.sale());
        if (!confirmation.created()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.created(URI.create("/api/v1/sales/" + response.id())).body(response);
    }

    @GetMapping
    public SalePageResponse recent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return SalePageResponse.from(service.recent(page, size));
    }

    @GetMapping("/{id}")
    public SaleResponse detail(@PathVariable UUID id) {
        return SaleResponse.from(service.detail(id));
    }

    public record ConfirmSaleRequest(
            @NotNull(message = "La clave de confirmación es obligatoria.") UUID idempotencyKey,
            @NotEmpty(message = "La venta debe contener al menos un producto.")
                    @Size(max = 100, message = "La venta admite hasta 100 líneas.")
                    List<@Valid SaleLineRequest> lines,
            @NotNull(message = "El pago es obligatorio.") @Valid PaymentRequest payment) {

        SaleCommand toCommand() {
            return new SaleCommand(
                    idempotencyKey,
                    lines.stream()
                            .map(line -> new SaleCommand.Line(line.productId(), line.quantity()))
                            .toList(),
                    payment.method(),
                    payment.cashReceived());
        }
    }

    public record SaleLineRequest(
            @NotNull(message = "El producto es obligatorio.") UUID productId,
            @NotNull(message = "La cantidad es obligatoria.")
                    @Min(value = 1, message = "La cantidad debe ser positiva.")
                    @Max(value = 1_000_000, message = "La cantidad es demasiado alta.")
                    Integer quantity) {}

    public record PaymentRequest(
            @NotNull(message = "El método de pago es obligatorio.") PaymentMethod method,
            BigDecimal cashReceived) {}

    public record SalePageResponse(
            List<SaleResponse> items,
            int page,
            int size,
            long totalElements,
            long totalPages) {

        static SalePageResponse from(SalePage<Sale> sales) {
            return new SalePageResponse(
                    sales.items().stream().map(SaleResponse::from).toList(),
                    sales.page(),
                    sales.size(),
                    sales.totalElements(),
                    sales.totalPages());
        }
    }

    public record SaleResponse(
            UUID id,
            String folio,
            UUID cashSessionId,
            UUID actorId,
            String actorDisplayName,
            Instant confirmedAt,
            SaleStatus status,
            BigDecimal total,
            List<SaleLineResponse> lines,
            PaymentResponse payment,
            CancellationResponse cancellation,
            String notice) {

        static SaleResponse from(Sale sale) {
            return new SaleResponse(
                    sale.id(),
                    sale.displayFolio(),
                    sale.cashSessionId(),
                    sale.actorId(),
                    sale.actorDisplayName(),
                    sale.confirmedAt(),
                    sale.status(),
                    sale.total().amount(),
                    sale.lines().stream().map(SaleLineResponse::from).toList(),
                    PaymentResponse.from(sale.payment()),
                    sale.cancellation() == null
                            ? null
                            : CancellationResponse.from(sale.cancellation()),
                    "Comprobante interno no tributario.");
        }
    }

    public record SaleLineResponse(
            UUID productId,
            String productName,
            String productCode,
            int quantity,
            BigDecimal unitSalePrice,
            BigDecimal subtotal) {

        static SaleLineResponse from(SaleLine line) {
            return new SaleLineResponse(
                    line.productId(),
                    line.productName(),
                    line.productCode(),
                    line.quantity(),
                    line.unitSalePrice().amount(),
                    line.subtotal().amount());
        }
    }

    public record PaymentResponse(
            PaymentMethod method,
            BigDecimal amount,
            BigDecimal cashPayable,
            BigDecimal roundingAdjustment,
            BigDecimal cashReceived,
            BigDecimal change) {

        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(
                    payment.method(),
                    payment.amount().amount(),
                    payment.cashPayable() == null ? null : payment.cashPayable().amount(),
                    payment.roundingAdjustment(),
                    payment.cashReceived() == null ? null : payment.cashReceived().amount(),
                    payment.change() == null ? null : payment.change().amount());
        }
    }

    public record CancellationResponse(
            PaymentMethod refundMethod,
            BigDecimal amount,
            BigDecimal cashPayable,
            BigDecimal roundingAdjustment,
            String reason,
            UUID actorId,
            String actorDisplayName,
            Instant occurredAt) {

        static CancellationResponse from(SaleCancellation cancellation) {
            return new CancellationResponse(
                    cancellation.refundMethod(),
                    cancellation.amount().amount(),
                    cancellation.cashPayable() == null
                            ? null
                            : cancellation.cashPayable().amount(),
                    cancellation.roundingAdjustment(),
                    cancellation.reason(),
                    cancellation.actorId(),
                    cancellation.actorDisplayName(),
                    cancellation.occurredAt());
        }
    }
}

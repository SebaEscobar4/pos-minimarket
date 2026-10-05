package com.minimarket.pos.sales.api;

import com.minimarket.pos.identity.application.IdentityPrincipal;
import com.minimarket.pos.sales.application.CancelSaleCommand;
import com.minimarket.pos.sales.application.SaleCancellationService;
import com.minimarket.pos.sales.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/sales")
public class SaleAdminController {

    private final SaleCancellationService service;

    public SaleAdminController(SaleCancellationService service) {
        this.service = service;
    }

    @PostMapping("/{saleId}/cancellations")
    public ResponseEntity<SaleController.SaleResponse> cancel(
            @PathVariable UUID saleId,
            @Valid @RequestBody CancelSaleRequest request,
            Authentication authentication) {
        IdentityPrincipal actor = (IdentityPrincipal) authentication.getPrincipal();
        var sale = service.cancel(saleId, request.toCommand(), actor.id(), actor.displayName());
        return ResponseEntity.created(URI.create("/api/v1/sales/" + sale.id()))
                .body(SaleController.SaleResponse.from(sale));
    }

    public record CancelSaleRequest(
            @NotNull(message = "El método de devolución es obligatorio.")
                    PaymentMethod refundMethod,
            @NotBlank(message = "El motivo de la anulación es obligatorio.")
                    @Size(max = 500, message = "El motivo admite hasta 500 caracteres.")
                    String reason) {

        CancelSaleCommand toCommand() {
            return new CancelSaleCommand(refundMethod, reason);
        }
    }
}

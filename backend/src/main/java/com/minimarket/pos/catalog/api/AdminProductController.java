package com.minimarket.pos.catalog.api;

import com.minimarket.pos.catalog.api.ProductResponses.AdminProductResponse;
import com.minimarket.pos.catalog.api.ProductResponses.PageResponse;
import com.minimarket.pos.catalog.application.PageResult;
import com.minimarket.pos.catalog.application.ProductCommand;
import com.minimarket.pos.catalog.application.ProductService;
import com.minimarket.pos.catalog.domain.Product;
import com.minimarket.pos.catalog.domain.ProductStatus;
import com.minimarket.pos.identity.application.IdentityPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/catalog/products")
public class AdminProductController {

    private final ProductService service;

    public AdminProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductResponse create(
            @Valid @RequestBody ProductRequest request, Authentication authentication) {
        return ProductResponses.admin(service.create(request.toCommand(), actor(authentication).id()));
    }

    @PutMapping("/{id}")
    public AdminProductResponse edit(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        return ProductResponses.admin(service.edit(id, request.toCommand(), actor(authentication).id()));
    }

    @PatchMapping("/{id}/status")
    public AdminProductResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ProductStatusRequest request,
            Authentication authentication) {
        return ProductResponses.admin(
                service.changeStatus(id, request.status(), actor(authentication).id()));
    }

    @GetMapping
    public PageResponse<AdminProductResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Product> result =
                service.searchForAdministration(name, includeInactive, page, size);
        return ProductResponses.page(
                result, result.items().stream().map(ProductResponses::admin).toList());
    }

    private IdentityPrincipal actor(Authentication authentication) {
        return (IdentityPrincipal) authentication.getPrincipal();
    }

    public record ProductRequest(
            String code,
            String name,
            UUID categoryId,
            BigDecimal purchasePrice,
            BigDecimal salePrice,
            Integer minimumStock) {

        ProductCommand toCommand() {
            return new ProductCommand(
                    code, name, categoryId, purchasePrice, salePrice, minimumStock);
        }
    }

    public record ProductStatusRequest(
            @NotNull(message = "El estado es obligatorio.") ProductStatus status) {}
}

package com.minimarket.pos.catalog.api;

import com.minimarket.pos.catalog.application.CategoryService;
import com.minimarket.pos.catalog.domain.Category;
import com.minimarket.pos.catalog.domain.CategoryStatus;
import com.minimarket.pos.identity.application.IdentityPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
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
@RequestMapping("/api/v1/admin/catalog/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(
            @Valid @RequestBody CategoryRequest request, Authentication authentication) {
        return CategoryResponse.from(service.create(request.name(), actor(authentication).id()));
    }

    @GetMapping
    public List<CategoryResponse> list(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.list(includeInactive).stream().map(CategoryResponse::from).toList();
    }

    @PutMapping("/{id}")
    public CategoryResponse rename(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request,
            Authentication authentication) {
        return CategoryResponse.from(service.rename(id, request.name(), actor(authentication).id()));
    }

    @PatchMapping("/{id}/status")
    public CategoryResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryStatusRequest request,
            Authentication authentication) {
        return CategoryResponse.from(
                service.changeStatus(id, request.status(), actor(authentication).id()));
    }

    private IdentityPrincipal actor(Authentication authentication) {
        return (IdentityPrincipal) authentication.getPrincipal();
    }

    public record CategoryRequest(
            @NotBlank(message = "El nombre es obligatorio.")
                    @Size(max = 100, message = "El nombre admite hasta 100 caracteres.")
                    String name) {}

    public record CategoryStatusRequest(
            @NotNull(message = "El estado es obligatorio.") CategoryStatus status) {}

    public record CategoryResponse(
            UUID id, String name, CategoryStatus status, Instant createdAt, Instant updatedAt) {

        static CategoryResponse from(Category category) {
            return new CategoryResponse(
                    category.id(),
                    category.name(),
                    category.status(),
                    category.createdAt(),
                    category.updatedAt());
        }
    }
}

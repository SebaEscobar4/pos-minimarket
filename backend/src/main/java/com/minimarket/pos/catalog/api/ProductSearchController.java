package com.minimarket.pos.catalog.api;

import com.minimarket.pos.catalog.api.ProductResponses.PageResponse;
import com.minimarket.pos.catalog.api.ProductResponses.SaleProductResponse;
import com.minimarket.pos.catalog.application.PageResult;
import com.minimarket.pos.catalog.application.ProductService;
import com.minimarket.pos.catalog.domain.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/products")
public class ProductSearchController {

    private final ProductService service;

    public ProductSearchController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/by-code")
    public SaleProductResponse byCode(@RequestParam String code) {
        return ProductResponses.sale(service.findActiveByCode(code));
    }

    @GetMapping("/search")
    public PageResponse<SaleProductResponse> byName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Product> result = service.searchForPointOfSale(name, page, size);
        return ProductResponses.page(
                result, result.items().stream().map(ProductResponses::sale).toList());
    }
}

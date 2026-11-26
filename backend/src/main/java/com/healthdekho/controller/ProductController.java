package com.healthdekho.controller;

import com.healthdekho.dto.AvailableProductResponse;
import com.healthdekho.dto.PageResponse;
import com.healthdekho.service.ProductCatalogService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCatalogService productCatalogService;

    public ProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @GetMapping("/available")
    public List<AvailableProductResponse> available(@AuthenticationPrincipal Jwt jwt) {
        return productCatalogService.findAvailableForCustomer(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/search")
    public PageResponse<AvailableProductResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String policyName,
            @RequestParam(required = false) String policyType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productCatalogService.searchAvailableForCustomer(
                UUID.fromString(jwt.getSubject()),
                productCode,
                policyName,
                policyType,
                page,
                size
        );
    }
}

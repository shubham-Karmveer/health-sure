package com.healthdekho.controller;

import com.healthdekho.dto.AvailableProductResponse;
import com.healthdekho.service.ProductCatalogService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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
}

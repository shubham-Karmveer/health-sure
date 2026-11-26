package com.healthdekho.service;

import com.healthdekho.dto.AvailableProductResponse;
import com.healthdekho.repository.InsuranceProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductCatalogService {

    private final InsuranceProductRepository productRepository;

    public ProductCatalogService(InsuranceProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<AvailableProductResponse> findAvailableForCustomer(UUID customerId) {
        return productRepository.findAvailableForCustomer(customerId)
                .stream()
                .map(AvailableProductResponse::from)
                .toList();
    }
}

package com.healthdekho.service;

import com.healthdekho.dto.AvailableProductResponse;
import com.healthdekho.dto.PageResponse;
import com.healthdekho.repository.InsuranceProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional(readOnly = true)
    public PageResponse<AvailableProductResponse> searchAvailableForCustomer(
            UUID customerId,
            String productCode,
            String policyName,
            String policyType,
            int page,
            int size
    ) {
        String normalizedCode = normalize(productCode);
        String normalizedName = normalize(policyName);
        String normalizedType = normalize(policyType);
        if (normalizedCode == null && normalizedName == null && normalizedType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide a product code, policy name, or policy type to search."
            );
        }
        if (page < 0 || size < 1 || size > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and page size must be between 1 and 50."
            );
        }
        if (tooLong(normalizedCode, 40) || tooLong(normalizedName, 160) || tooLong(normalizedType, 40)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search criteria exceed the allowed length.");
        }
        Page<AvailableProductResponse> results = productRepository.searchAvailableForCustomer(
                        customerId,
                        normalizedCode == null ? "" : normalizedCode,
                        normalizedName == null ? "" : normalizedName,
                        normalizedType == null ? "" : normalizedType,
                        PageRequest.of(page, size)
                )
                .map(AvailableProductResponse::from);
        return PageResponse.from(results);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean tooLong(String value, int maxLength) {
        return value != null && value.length() > maxLength;
    }
}

package com.healthdekho.dto;

import com.healthdekho.entity.InsuranceProduct;

import java.math.BigDecimal;

public record AvailableProductResponse(
        Long productId,
        String productCode,
        String policyName,
        String policyType,
        int termMonths,
        String premiumFrequency,
        BigDecimal premiumAmount,
        BigDecimal sumInsured,
        int minEntryAge,
        int maxEntryAge
) {
    public static AvailableProductResponse from(InsuranceProduct product) {
        return new AvailableProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getPolicyName(),
                product.getPolicyType(),
                product.getTermMonths(),
                product.getPremiumFrequency(),
                product.getPremiumAmount(),
                product.getSumInsured(),
                product.getMinEntryAge(),
                product.getMaxEntryAge()
        );
    }
}

package com.healthdekho.dto;

import com.healthdekho.entity.PolicyRecommendation;
import com.healthdekho.entity.InsuranceProduct;

import java.math.BigDecimal;

public record RecommendationItemResponse(
        Long productId,
        String productCode,
        String policyName,
        String policyType,
        int termMonths,
        String premiumFrequency,
        BigDecimal premiumAmount,
        BigDecimal sumInsured,
        int rankOrder,
        String recommendationReason,
        String suitability,
        boolean selectedForRegistration
) {
    public static RecommendationItemResponse from(PolicyRecommendation item, int rankOrder) {
        InsuranceProduct product = item.getProduct();
        return new RecommendationItemResponse(
                product.getId(),
                product.getProductCode(),
                product.getPolicyName(),
                product.getPolicyType(),
                product.getTermMonths(),
                product.getPremiumFrequency(),
                product.getPremiumAmount(),
                product.getSumInsured(),
                rankOrder,
                item.getRecommendationReason(),
                item.getSuitabilityDescription(),
                false
        );
    }
}

package com.healthdekho.dto;

import com.healthdekho.entity.AiRecommendationItem;
import com.healthdekho.entity.InsuranceProduct;

import java.math.BigDecimal;
import java.util.UUID;

public record RecommendationItemResponse(
        UUID productId,
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
    public static RecommendationItemResponse from(AiRecommendationItem item) {
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
                item.getRankOrder(),
                item.getRecommendationReason(),
                item.getSuitability(),
                item.isSelectedForRegistration()
        );
    }
}

package com.healthdekho.dto;

import com.healthdekho.entity.CustomerPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicySummaryResponse(
        String policyNumber,
        String policyName,
        String policyType,
        int termMonths,
        LocalDate startDate,
        LocalDate maturityDate,
        String premiumFrequency,
        BigDecimal premiumAmount,
        BigDecimal sumInsured,
        String status
) {
    public static PolicySummaryResponse from(CustomerPolicy policy) {
        return new PolicySummaryResponse(
                policy.getPolicyNumber(),
                policy.getProduct().getPolicyName(),
                policy.getProduct().getPolicyType(),
                policy.getTermMonths(),
                policy.getStartDate(),
                policy.getMaturityDate(),
                policy.getPremiumFrequency(),
                policy.getPremiumAmount(),
                policy.getSumInsured(),
                policy.getStatus()
        );
    }
}

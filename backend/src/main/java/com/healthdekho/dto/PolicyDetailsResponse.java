package com.healthdekho.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyDetailsResponse(
        String policyNumber,
        String customerName,
        LocalDate dateOfBirth,
        String phone,
        String policyName,
        String policyType,
        int termMonths,
        LocalDate startDate,
        LocalDate maturityDate,
        String premiumFrequency,
        BigDecimal premiumAmount,
        BigDecimal sumInsured,
        BigDecimal totalPremiumPayable,
        BigDecimal totalPremiumPaid,
        BigDecimal remainingPremium,
        BigDecimal totalPenaltyAssessed,
        BigDecimal totalPenaltyPaid,
        BigDecimal remainingPenalty,
        BigDecimal remainingBalance,
        BigDecimal estimatedPenalty,
        BigDecimal estimatedBalance,
        String status
) {
}

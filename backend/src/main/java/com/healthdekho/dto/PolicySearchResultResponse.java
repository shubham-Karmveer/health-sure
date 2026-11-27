package com.healthdekho.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicySearchResultResponse(
        String policyNumber,
        String customerName,
        LocalDate dateOfBirth,
        String policyName,
        String policyType,
        LocalDate startDate,
        LocalDate maturityDate,
        BigDecimal totalPremiumPayable,
        String premiumPaymentStatus,
        BigDecimal totalPremiumPaid,
        BigDecimal remainingPremium
) {
}

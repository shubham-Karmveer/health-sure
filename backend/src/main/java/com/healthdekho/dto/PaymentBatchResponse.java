package com.healthdekho.dto;

import java.math.BigDecimal;
import java.util.List;

public record PaymentBatchResponse(
        String policyNumber,
        String confirmation,
        BigDecimal totalPremiumPayable,
        BigDecimal totalPremiumPaid,
        BigDecimal remainingPremium,
        BigDecimal totalPenaltyAssessed,
        BigDecimal totalPenaltyPaid,
        BigDecimal remainingPenalty,
        BigDecimal remainingBalance,
        List<PaymentReceipt> payments
) {
}

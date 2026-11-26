package com.healthdekho.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PolicyRegistrationResponse(
        String policyNumber,
        String confirmation,
        String customerName,
        LocalDate dateOfBirth,
        String phone,
        String productCode,
        String policyName,
        String policyType,
        int termMonths,
        LocalDate startDate,
        LocalDate maturityDate,
        String premiumFrequency,
        BigDecimal premiumAmount,
        BigDecimal sumInsured,
        BigDecimal totalPremiumPayable,
        List<InstallmentResponse> installments
) {
}

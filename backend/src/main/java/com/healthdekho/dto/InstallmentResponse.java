package com.healthdekho.dto;

import com.healthdekho.entity.PremiumInstallment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResponse(
        int installmentNumber,
        LocalDate dueDate,
        LocalDate gracePeriodEndDate,
        BigDecimal scheduledAmount,
        BigDecimal paidAmount,
        String paymentStatus
) {
    public static InstallmentResponse from(PremiumInstallment installment) {
        return new InstallmentResponse(
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                installment.getGracePeriodEndDate(),
                installment.getScheduledAmount(),
                installment.getPaidAmount(),
                installment.getPaymentStatus()
        );
    }
}

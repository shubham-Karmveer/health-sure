package com.healthdekho.dto;

import com.healthdekho.entity.PremiumInstallment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record InstallmentDetailResponse(
        int installmentNumber,
        LocalDate dueDate,
        LocalDate gracePeriodEndDate,
        BigDecimal scheduledAmount,
        BigDecimal paidAmount,
        BigDecimal remainingPremium,
        BigDecimal penaltyAmount,
        BigDecimal paidPenaltyAmount,
        BigDecimal remainingPenalty,
        BigDecimal estimatedPenalty,
        BigDecimal remainingBalance,
        BigDecimal estimatedBalance,
        String paymentStatus
) {
    public static InstallmentDetailResponse from(PremiumInstallment installment, LocalDate today) {
        BigDecimal remainingPremium = installment.getScheduledAmount().subtract(installment.getPaidAmount());
        BigDecimal remainingPenalty = installment.getPenaltyAmount().subtract(installment.getPaidPenaltyAmount());
        BigDecimal estimatedPenalty = BigDecimal.ZERO.setScale(2);
        if (today.isAfter(installment.getGracePeriodEndDate())
                && remainingPremium.signum() > 0
                && installment.getPenaltyAmount().signum() == 0) {
            estimatedPenalty = remainingPremium.multiply(new BigDecimal("0.10"))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal remainingBalance = remainingPremium.add(remainingPenalty);
        return new InstallmentDetailResponse(
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                installment.getGracePeriodEndDate(),
                installment.getScheduledAmount(),
                installment.getPaidAmount(),
                remainingPremium,
                installment.getPenaltyAmount(),
                installment.getPaidPenaltyAmount(),
                remainingPenalty,
                estimatedPenalty,
                remainingBalance,
                remainingBalance.add(estimatedPenalty),
                installment.getPaymentStatus()
        );
    }
}

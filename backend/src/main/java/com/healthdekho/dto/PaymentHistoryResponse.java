package com.healthdekho.dto;

import com.healthdekho.entity.PremiumPayment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

public record PaymentHistoryResponse(
        String transactionReference,
        Instant paymentDate,
        int installmentNumber,
        BigDecimal amountPaid,
        BigDecimal premiumAmount,
        BigDecimal penaltyAmount,
        String paymentMethod
) {
    public static PaymentHistoryResponse from(PremiumPayment payment) {
        return new PaymentHistoryResponse(
                payment.getTransactionReference(),
                payment.getPaymentDate().atStartOfDay().toInstant(ZoneOffset.UTC),
                payment.getInstallmentNumber(),
                payment.getAmountPaid(),
                payment.getPremiumAmount(),
                payment.getPenaltyAmount(),
                payment.getPaymentMethod()
        );
    }
}

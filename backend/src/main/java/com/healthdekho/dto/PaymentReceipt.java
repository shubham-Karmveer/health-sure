package com.healthdekho.dto;

import com.healthdekho.entity.PremiumPayment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentReceipt(
        String transactionReference,
        int installmentNumber,
        Instant paymentDate,
        BigDecimal amountPaid,
        BigDecimal premiumAmount,
        BigDecimal penaltyAmount,
        String paymentMethod
) {
    public static PaymentReceipt from(PremiumPayment payment) {
        return new PaymentReceipt(
                payment.getTransactionReference(),
                payment.getInstallmentNumber(),
                payment.getPaymentDate(),
                payment.getAmountPaid(),
                payment.getPremiumAmount(),
                payment.getPenaltyAmount(),
                payment.getPaymentMethod()
        );
    }
}

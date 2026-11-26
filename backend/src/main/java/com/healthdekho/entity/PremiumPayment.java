package com.healthdekho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "premium_payment")
public class PremiumPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "installment_id", nullable = false)
    private PremiumInstallment installment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idempotency_id", nullable = false)
    private PaymentIdempotencyRecord idempotencyRecord;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 60)
    private String transactionReference;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "premium_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal premiumAmount;

    @Column(name = "penalty_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "payment_method", nullable = false, length = 40)
    private String paymentMethod;

    @Column(name = "payment_date", nullable = false)
    private Instant paymentDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PremiumPayment() {
    }

    public PremiumPayment(
            PremiumInstallment installment,
            PaymentIdempotencyRecord idempotencyRecord,
            String transactionReference,
            BigDecimal amountPaid,
            BigDecimal premiumAmount,
            BigDecimal penaltyAmount,
            String paymentMethod
    ) {
        this.installment = installment;
        this.idempotencyRecord = idempotencyRecord;
        this.transactionReference = transactionReference;
        this.amountPaid = amountPaid;
        this.premiumAmount = premiumAmount;
        this.penaltyAmount = penaltyAmount;
        this.paymentMethod = paymentMethod;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (paymentDate == null) {
            paymentDate = createdAt;
        }
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getPremiumAmount() {
        return premiumAmount;
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public Instant getPaymentDate() {
        return paymentDate;
    }

    public int getInstallmentNumber() {
        return installment.getInstallmentNumber();
    }
}

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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "premium_payments")
public class PremiumPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "installment_id", nullable = false)
    private PremiumInstallment installment;

    @Column(name = "receipt_number", nullable = false, length = 50)
    private String receiptNumber;

    @Column(name = "transaction_reference", unique = true, length = 100)
    private String transactionReference;

    @Column(name = "premium_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal premiumAmount;

    @Column(name = "penalty_paid", precision = 15, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PremiumPayment() {
    }

    public PremiumPayment(
            PremiumInstallment installment,
            String receiptNumber,
            String transactionReference,
            BigDecimal premiumAmount,
            BigDecimal penaltyAmount,
            String paymentMethod
    ) {
        this.installment = installment;
        this.receiptNumber = receiptNumber;
        this.transactionReference = transactionReference;
        this.premiumAmount = premiumAmount;
        this.penaltyAmount = penaltyAmount;
        this.paymentMethod = paymentMethod;
    }

    @PrePersist
    void onCreate() {
        if (paymentDate == null) {
            paymentDate = LocalDate.now();
        }
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public BigDecimal getAmountPaid() {
        return premiumAmount.add(penaltyAmount == null ? BigDecimal.ZERO : penaltyAmount);
    }

    public BigDecimal getPremiumAmount() {
        return premiumAmount;
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount == null ? BigDecimal.ZERO.setScale(2) : penaltyAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public int getInstallmentNumber() {
        return installment.getInstallmentNumber();
    }

    public Long getInstallmentPolicyId() {
        return installment.getPolicy().getId();
    }
}

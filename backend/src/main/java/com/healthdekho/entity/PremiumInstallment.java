package com.healthdekho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "premium_installment")
public class PremiumInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private CustomerPolicy policy;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "grace_period_end_date", nullable = false)
    private LocalDate gracePeriodEndDate;

    @Column(name = "scheduled_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal scheduledAmount;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "penalty_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "paid_penalty_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidPenaltyAmount;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;

    protected PremiumInstallment() {
    }

    public PremiumInstallment(
            CustomerPolicy policy,
            int installmentNumber,
            LocalDate dueDate,
            LocalDate gracePeriodEndDate,
            BigDecimal scheduledAmount
    ) {
        this.policy = policy;
        this.installmentNumber = installmentNumber;
        this.dueDate = dueDate;
        this.gracePeriodEndDate = gracePeriodEndDate;
        this.scheduledAmount = scheduledAmount;
        this.paidAmount = BigDecimal.ZERO.setScale(2);
        this.penaltyAmount = BigDecimal.ZERO.setScale(2);
        this.paidPenaltyAmount = BigDecimal.ZERO.setScale(2);
        this.paymentStatus = "PENDING";
    }

    public void assessPenalty(BigDecimal amount) {
        if (penaltyAmount.signum() != 0) {
            throw new IllegalStateException("A penalty has already been assessed for this installment.");
        }
        penaltyAmount = amount;
    }

    public void applyPayment(BigDecimal premiumAmount, BigDecimal penaltyAmount) {
        paidAmount = paidAmount.add(premiumAmount);
        paidPenaltyAmount = paidPenaltyAmount.add(penaltyAmount);
    }

    public void updatePaymentStatus(LocalDate today) {
        if (paidAmount.compareTo(scheduledAmount) >= 0
                && paidPenaltyAmount.compareTo(penaltyAmount) >= 0) {
            paymentStatus = "PAID";
        } else if (today.isAfter(gracePeriodEndDate)) {
            paymentStatus = "OVERDUE";
        } else if (paidAmount.signum() > 0 || paidPenaltyAmount.signum() > 0) {
            paymentStatus = "PARTIALLY_PAID";
        } else {
            paymentStatus = "PENDING";
        }
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getGracePeriodEndDate() {
        return gracePeriodEndDate;
    }

    public BigDecimal getScheduledAmount() {
        return scheduledAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount;
    }

    public BigDecimal getPaidPenaltyAmount() {
        return paidPenaltyAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }
}

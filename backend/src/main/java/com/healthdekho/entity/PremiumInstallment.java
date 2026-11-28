package com.healthdekho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "premium_installments")
public class PremiumInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "installment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private CustomerPolicy policy;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "grace_period_end_date", nullable = false)
    private LocalDate gracePeriodEndDate;

    @Column(name = "premium_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal scheduledAmount;

    @Column(name = "amount_paid", precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;

    @OneToMany(mappedBy = "installment")
    private List<PremiumPayment> payments = new ArrayList<>();

    @Transient
    private BigDecimal pendingPremiumPaid = BigDecimal.ZERO.setScale(2);

    @Transient
    private BigDecimal pendingPenaltyPaid = BigDecimal.ZERO.setScale(2);

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
        this.paymentStatus = "PENDING";
    }

    public void assessPenalty(BigDecimal amount) {
        if (getPenaltyAmount().signum() != 0) {
            throw new IllegalStateException("A penalty has already been assessed for this installment.");
        }
        penaltyAmount = amount;
    }

    public void applyPayment(BigDecimal premiumAmount, BigDecimal penaltyAmount) {
        paidAmount = getTotalPaidAmount().add(premiumAmount).add(penaltyAmount);
        pendingPremiumPaid = pendingPremiumPaid.add(premiumAmount);
        pendingPenaltyPaid = pendingPenaltyPaid.add(penaltyAmount);
    }

    public void updatePaymentStatus(LocalDate today) {
        if (getPaidAmount().compareTo(scheduledAmount) >= 0
                && getPaidPenaltyAmount().compareTo(getPenaltyAmount()) >= 0) {
            paymentStatus = "PAID";
        } else if (today.isAfter(gracePeriodEndDate)) {
            paymentStatus = "OVERDUE";
        } else if (getPaidAmount().signum() > 0 || getPaidPenaltyAmount().signum() > 0) {
            paymentStatus = "PARTIALLY_PAID";
        } else {
            paymentStatus = "PENDING";
        }
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public CustomerPolicy getPolicy() {
        return policy;
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
        return payments.stream()
                .map(PremiumPayment::getPremiumAmount)
                .reduce(pendingPremiumPaid, BigDecimal::add);
    }

    public BigDecimal getTotalPaidAmount() {
        return paidAmount == null ? BigDecimal.ZERO.setScale(2) : paidAmount;
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount == null ? BigDecimal.ZERO.setScale(2) : penaltyAmount;
    }

    public BigDecimal getPaidPenaltyAmount() {
        return payments.stream()
                .map(PremiumPayment::getPenaltyAmount)
                .reduce(pendingPenaltyPaid, BigDecimal::add);
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }
}

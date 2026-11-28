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
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "customer_policies")
public class CustomerPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @Column(name = "policy_number", nullable = false, unique = true, length = 40)
    private String policyNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct product;

    @Column(name = "policy_term_years", nullable = false)
    private int policyTermYears;

    @Column(name = "policy_start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "policy_maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(name = "premium_frequency", nullable = false, length = 20)
    private String premiumFrequency;

    @Column(name = "premium_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal premiumAmount;

    @Column(name = "sum_insured", nullable = false, precision = 15, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "total_premium_payable", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPremiumPayable;

    @Column(name = "policy_status", length = 20)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected CustomerPolicy() {
    }

    public CustomerPolicy(String policyNumber, Customer customer, InsuranceProduct product, int termMonths,
                          LocalDate startDate, LocalDate maturityDate, String premiumFrequency,
                          BigDecimal premiumAmount, BigDecimal sumInsured, BigDecimal totalPremiumPayable,
                          String status) {
        this.policyNumber = policyNumber;
        this.customer = customer;
        this.product = product;
        if (termMonths <= 0 || termMonths % 12 != 0) {
            throw new IllegalArgumentException("Policy terms must be a positive whole number of years.");
        }
        this.policyTermYears = termMonths / 12;
        this.startDate = startDate;
        this.maturityDate = maturityDate;
        this.premiumFrequency = premiumFrequency;
        this.premiumAmount = premiumAmount;
        this.sumInsured = sumInsured;
        this.totalPremiumPayable = totalPremiumPayable;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public InsuranceProduct getProduct() {
        return product;
    }

    public int getTermMonths() {
        return policyTermYears * 12;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public String getPremiumFrequency() {
        return premiumFrequency;
    }

    public BigDecimal getPremiumAmount() {
        return premiumAmount;
    }

    public BigDecimal getSumInsured() {
        return sumInsured;
    }

    public BigDecimal getTotalPremiumPayable() {
        return totalPremiumPayable;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

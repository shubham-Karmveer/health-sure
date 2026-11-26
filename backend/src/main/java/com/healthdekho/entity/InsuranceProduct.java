package com.healthdekho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "insurance_product")
public class InsuranceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_code", nullable = false, unique = true, length = 40)
    private String productCode;

    @Column(name = "policy_name", nullable = false, length = 160)
    private String policyName;

    @Column(name = "policy_type", nullable = false, length = 40)
    private String policyType;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "premium_frequency", nullable = false, length = 20)
    private String premiumFrequency;

    @Column(name = "premium_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal premiumAmount;

    @Column(name = "sum_insured", nullable = false, precision = 19, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "min_entry_age", nullable = false)
    private int minEntryAge;

    @Column(name = "max_entry_age", nullable = false)
    private int maxEntryAge;

    @Column(nullable = false)
    private boolean active;

    protected InsuranceProduct() {
    }

    public InsuranceProduct(String productCode, String policyName, String policyType, int termMonths,
                            String premiumFrequency, BigDecimal premiumAmount, BigDecimal sumInsured,
                            int minEntryAge, int maxEntryAge, boolean active) {
        this.productCode = productCode;
        this.policyName = policyName;
        this.policyType = policyType;
        this.termMonths = termMonths;
        this.premiumFrequency = premiumFrequency;
        this.premiumAmount = premiumAmount;
        this.sumInsured = sumInsured;
        this.minEntryAge = minEntryAge;
        this.maxEntryAge = maxEntryAge;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getPolicyType() {
        return policyType;
    }

    public int getTermMonths() {
        return termMonths;
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

    public boolean isActive() {
        return active;
    }

    public int getMinEntryAge() {
        return minEntryAge;
    }

    public int getMaxEntryAge() {
        return maxEntryAge;
    }
}

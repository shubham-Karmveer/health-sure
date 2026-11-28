package com.healthdekho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "insurance_products")
public class InsuranceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true, length = 30)
    private String productCode;

    @Column(name = "policy_name", nullable = false, length = 150)
    private String policyName;

    @Column(name = "policy_type", nullable = false, length = 50)
    private String policyType;

    @Column(name = "policy_term_years", nullable = false)
    private int policyTermYears;

    @Column(name = "premium_frequency", nullable = false, length = 20)
    private String premiumFrequency;

    @Column(name = "premium_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal premiumAmount;

    @Column(name = "sum_insured", nullable = false, precision = 15, scale = 2)
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
        if (termMonths <= 0 || termMonths % 12 != 0) {
            throw new IllegalArgumentException("Product terms must be a positive whole number of years.");
        }
        this.policyTermYears = termMonths / 12;
        this.premiumFrequency = premiumFrequency;
        this.premiumAmount = premiumAmount;
        this.sumInsured = sumInsured;
        this.minEntryAge = minEntryAge;
        this.maxEntryAge = maxEntryAge;
        this.active = active;
    }

    public Long getId() {
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
        return policyTermYears * 12;
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

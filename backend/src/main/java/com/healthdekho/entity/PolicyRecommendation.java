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

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_recommendations")
public class PolicyRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct product;

    @Column(name = "customer_goal", length = 500)
    private String customerGoal;

    @Column(name = "customer_age", nullable = false)
    private int customerAge;

    @Column(name = "recommendation_reason", nullable = false, columnDefinition = "TEXT")
    private String recommendationReason;

    @Column(name = "suitability_description", columnDefinition = "TEXT")
    private String suitabilityDescription;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PolicyRecommendation() {
    }

    public PolicyRecommendation(
            Customer customer,
            InsuranceProduct product,
            String customerGoal,
            int customerAge,
            String recommendationReason,
            String suitabilityDescription,
            String aiModel,
            LocalDateTime createdAt
    ) {
        this.customer = customer;
        this.product = product;
        this.customerGoal = customerGoal;
        this.customerAge = customerAge;
        this.recommendationReason = recommendationReason;
        this.suitabilityDescription = suitabilityDescription;
        this.aiModel = aiModel;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public InsuranceProduct getProduct() {
        return product;
    }

    public String getCustomerGoal() {
        return customerGoal;
    }

    public int getCustomerAge() {
        return customerAge;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public String getSuitabilityDescription() {
        return suitabilityDescription;
    }

    public String getAiModel() {
        return aiModel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

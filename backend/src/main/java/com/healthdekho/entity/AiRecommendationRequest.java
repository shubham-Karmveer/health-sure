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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendation_request")
public class AiRecommendationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "insurance_goals", columnDefinition = "TEXT")
    private String insuranceGoals;

    @Column(name = "preferred_term", length = 100)
    private String preferredTerm;

    @Column(name = "preferred_policy_type", length = 40)
    private String preferredPolicyType;

    @Column(name = "calculated_age", nullable = false)
    private int calculatedAge;

    @Column(name = "model_name", nullable = false, length = 120)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    @Column(name = "prompt_version", nullable = false, length = 100)
    private String promptVersion;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    protected AiRecommendationRequest() {
    }

    public AiRecommendationRequest(
            Customer customer,
            String insuranceGoals,
            String preferredTerm,
            String preferredPolicyType,
            int calculatedAge,
            String modelName,
            String modelVersion,
            String promptVersion
    ) {
        this.customer = customer;
        this.insuranceGoals = insuranceGoals;
        this.preferredTerm = preferredTerm;
        this.preferredPolicyType = preferredPolicyType;
        this.calculatedAge = calculatedAge;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.promptVersion = promptVersion;
        this.requestedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getInsuranceGoals() {
        return insuranceGoals;
    }

    public String getPreferredTerm() {
        return preferredTerm;
    }

    public String getPreferredPolicyType() {
        return preferredPolicyType;
    }

    public int getCalculatedAge() {
        return calculatedAge;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
}

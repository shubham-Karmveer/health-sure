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
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "ai_recommendation_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_recommendation_request_product",
                        columnNames = {"request_id", "product_id"}
                ),
                @UniqueConstraint(
                        name = "uq_recommendation_request_rank",
                        columnNames = {"request_id", "rank_order"}
                )
        }
)
public class AiRecommendationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private AiRecommendationRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct product;

    @Column(name = "rank_order", nullable = false)
    private int rankOrder;

    @Column(name = "recommendation_reason", nullable = false, columnDefinition = "TEXT")
    private String recommendationReason;

    @Column(nullable = false, length = 20)
    private String suitability;

    @Column(name = "selected_for_registration", nullable = false)
    private boolean selectedForRegistration;

    protected AiRecommendationItem() {
    }

    public AiRecommendationItem(
            AiRecommendationRequest request,
            InsuranceProduct product,
            int rankOrder,
            String recommendationReason,
            String suitability,
            boolean selectedForRegistration
    ) {
        this.request = request;
        this.product = product;
        this.rankOrder = rankOrder;
        this.recommendationReason = recommendationReason;
        this.suitability = suitability;
        this.selectedForRegistration = selectedForRegistration;
    }

    public UUID getId() {
        return id;
    }

    public AiRecommendationRequest getRequest() {
        return request;
    }

    public InsuranceProduct getProduct() {
        return product;
    }

    public int getRankOrder() {
        return rankOrder;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public String getSuitability() {
        return suitability;
    }

    public boolean isSelectedForRegistration() {
        return selectedForRegistration;
    }
}

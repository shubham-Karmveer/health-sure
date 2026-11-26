package com.healthdekho.repository;

import com.healthdekho.entity.AiRecommendationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiRecommendationRequestRepository extends JpaRepository<AiRecommendationRequest, UUID> {

    List<AiRecommendationRequest> findAllByCustomer_IdOrderByRequestedAtDesc(UUID customerId);
}

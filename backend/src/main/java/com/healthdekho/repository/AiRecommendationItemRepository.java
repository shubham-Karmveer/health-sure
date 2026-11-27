package com.healthdekho.repository;

import com.healthdekho.entity.AiRecommendationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiRecommendationItemRepository extends JpaRepository<AiRecommendationItem, UUID> {

    List<AiRecommendationItem> findAllByRequest_IdOrderByRankOrderAsc(UUID requestId);

    long countByRequest_Customer_Id(UUID customerId);
}

package com.healthdekho.repository;

import com.healthdekho.entity.PolicyRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PolicyRecommendationRepository extends JpaRepository<PolicyRecommendation, Long> {

    Optional<PolicyRecommendation> findByIdAndCustomer_Id(Long id, Long customerId);

    long countByCustomer_Id(Long customerId);

    Optional<PolicyRecommendation> findFirstByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    List<PolicyRecommendation> findAllByCustomer_IdAndCustomerGoalAndCustomerAgeAndAiModelAndCreatedAtOrderByIdAsc(
            Long customerId,
            String customerGoal,
            int customerAge,
            String aiModel,
            LocalDateTime createdAt
    );
}

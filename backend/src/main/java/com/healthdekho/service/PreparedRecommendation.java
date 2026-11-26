package com.healthdekho.service;

import com.healthdekho.recommendation.RecommendationProvider;

import java.time.LocalDate;
import java.util.List;

public record PreparedRecommendation(
        LocalDate dateOfBirth,
        int calculatedAge,
        String insuranceGoals,
        String preferredTerm,
        String preferredPolicyType,
        List<RecommendationProvider.Candidate> candidates
) {
}

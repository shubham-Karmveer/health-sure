package com.healthdekho.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RecommendationResponse(
        UUID requestId,
        LocalDate dateOfBirth,
        int calculatedAge,
        String insuranceGoals,
        String preferredTerm,
        String preferredPolicyType,
        String modelName,
        String modelVersion,
        String promptVersion,
        Instant requestedAt,
        List<RecommendationItemResponse> recommendations
) {
}

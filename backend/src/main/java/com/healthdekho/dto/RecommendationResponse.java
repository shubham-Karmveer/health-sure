package com.healthdekho.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RecommendationResponse(
        Long requestId,
        LocalDate dateOfBirth,
        int calculatedAge,
        String insuranceGoals,
        String modelName,
        Instant requestedAt,
        List<RecommendationItemResponse> recommendations
) {
}

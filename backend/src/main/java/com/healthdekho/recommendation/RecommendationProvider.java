package com.healthdekho.recommendation;

import java.math.BigDecimal;
import java.util.List;

public interface RecommendationProvider {

    List<Suggestion> recommend(Context context);

    Metadata metadata();

    record Context(
            int customerAge,
            String insuranceGoals,
            String preferredTerm,
            String preferredPolicyType,
            List<Candidate> candidates
    ) {
    }

    record Candidate(
            String productCode,
            String policyName,
            String policyType,
            int termMonths,
            String premiumFrequency,
            BigDecimal premiumAmount,
            BigDecimal sumInsured,
            int minEntryAge,
            int maxEntryAge
    ) {
    }

    record Suggestion(String productCode, String recommendationReason, String suitability) {
    }

    record Metadata(String modelName, String modelVersion, String promptVersion) {
    }
}

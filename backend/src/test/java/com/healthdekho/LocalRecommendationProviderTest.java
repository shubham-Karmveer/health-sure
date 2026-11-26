package com.healthdekho;

import com.healthdekho.recommendation.LocalRecommendationProvider;
import com.healthdekho.recommendation.RecommendationProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalRecommendationProviderTest {

    @Test
    void returnsAtMostThreeCandidatesAndPrioritizesPreferredType() {
        LocalRecommendationProvider provider =
                new LocalRecommendationProvider("local-test", "1", "test-prompt");
        List<RecommendationProvider.Candidate> candidates = List.of(
                candidate("HD-HEALTH-001", "HEALTH"),
                candidate("HD-TERM-001", "TERM"),
                candidate("HD-SAV-001", "SAVINGS"),
                candidate("HD-HEALTH-002", "HEALTH"),
                candidate("HD-TERM-240", "TERM", 240)
        );

        List<RecommendationProvider.Suggestion> result = provider.recommend(
                new RecommendationProvider.Context(35, null, "20 years", "HEALTH", candidates)
        );

        assertThat(result).hasSize(3);
        assertThat(result.get(0).productCode()).isEqualTo("HD-HEALTH-001");
        assertThat(result).extracting(RecommendationProvider.Suggestion::productCode)
                .contains("HD-HEALTH-002", "HD-TERM-240")
                .doesNotContain("UNKNOWN");
        assertThat(provider.metadata().modelName()).isEqualTo("local-test");
    }

    private RecommendationProvider.Candidate candidate(String code, String type) {
        return candidate(code, type, 120);
    }

    private RecommendationProvider.Candidate candidate(String code, String type, int termMonths) {
        return new RecommendationProvider.Candidate(
                code,
                code,
                type,
                termMonths,
                "MONTHLY",
                new BigDecimal("1000.00"),
                new BigDecimal("100000.00"),
                18,
                60
        );
    }
}

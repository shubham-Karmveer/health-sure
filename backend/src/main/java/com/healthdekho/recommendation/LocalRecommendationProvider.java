package com.healthdekho.recommendation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "stub", matchIfMissing = true)
public class LocalRecommendationProvider implements RecommendationProvider {

    private static final Pattern TERM_PATTERN = Pattern.compile(
            "(?i)(\\d+)\\s*(years?|yrs?|months?|mos?)?"
    );
    private final Metadata metadata;

    public LocalRecommendationProvider(
            @Value("${app.ai.model-name:healthdekho-local-stub}") String modelName,
            @Value("${app.ai.model-version:v1}") String modelVersion,
            @Value("${app.ai.prompt-version:recommendations-v1}") String promptVersion
    ) {
        this.metadata = new Metadata(modelName, modelVersion, promptVersion);
    }

    @Override
    public List<Suggestion> recommend(Context context) {
        return context.candidates().stream()
                .sorted(Comparator
                        .comparing((Candidate candidate) -> !matchesType(candidate, context.preferredPolicyType()))
                        .thenComparing(candidate -> !matchesTerm(candidate, context.preferredTerm()))
                        .thenComparing(Candidate::productCode))
                .limit(3)
                .map(candidate -> new Suggestion(
                        candidate.productCode(),
                        reason(candidate, context),
                        matchesType(candidate, context.preferredPolicyType()) ? "HIGH" : "MODERATE"
                ))
                .toList();
    }

    @Override
    public Metadata metadata() {
        return metadata;
    }

    private boolean matchesType(Candidate candidate, String preferredType) {
        return preferredType != null && candidate.policyType().equalsIgnoreCase(preferredType);
    }

    private boolean matchesTerm(Candidate candidate, String preferredTerm) {
        if (preferredTerm == null) {
            return false;
        }
        Matcher matcher = TERM_PATTERN.matcher(preferredTerm.trim());
        if (!matcher.find()) {
            return false;
        }
        int amount;
        try {
            amount = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            return false;
        }
        String unit = matcher.group(2);
        boolean years = unit != null && unit.toLowerCase(Locale.ROOT).startsWith("y");
        if (years && amount > Integer.MAX_VALUE / 12) {
            return false;
        }
        int preferredMonths = years ? amount * 12 : amount;
        return Objects.equals(preferredMonths, candidate.termMonths());
    }

    private String reason(Candidate candidate, Context context) {
        if (matchesType(candidate, context.preferredPolicyType())) {
            return "Matches your preferred " + candidate.policyType().toLowerCase(Locale.ROOT) + " insurance type.";
        }
        if (matchesTerm(candidate, context.preferredTerm())) {
            return "Matches your preferred policy term.";
        }
        return "Available in the catalog and within your product entry-age eligibility.";
    }
}

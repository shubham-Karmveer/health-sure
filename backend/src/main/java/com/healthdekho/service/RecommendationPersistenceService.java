package com.healthdekho.service;

import com.healthdekho.dto.RecommendationItemResponse;
import com.healthdekho.dto.RecommendationResponse;
import com.healthdekho.entity.Customer;
import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.entity.PolicyRecommendation;
import com.healthdekho.repository.CustomerRepository;
import com.healthdekho.repository.InsuranceProductRepository;
import com.healthdekho.repository.PolicyRecommendationRepository;
import com.healthdekho.recommendation.RecommendationProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationPersistenceService {

    private static final Set<String> ALLOWED_SUITABILITY = Set.of("HIGH", "MODERATE", "LOW");

    private final CustomerRepository customerRepository;
    private final InsuranceProductRepository productRepository;
    private final PolicyRecommendationRepository recommendationRepository;

    public RecommendationPersistenceService(
            CustomerRepository customerRepository,
            InsuranceProductRepository productRepository,
            PolicyRecommendationRepository recommendationRepository
    ) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional
    public RecommendationResponse save(
            Long customerId,
            PreparedRecommendation prepared,
            RecommendationProvider.Metadata metadata,
            List<RecommendationProvider.Suggestion> suggestions
    ) {
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!customer.getDateOfBirth().equals(prepared.dateOfBirth())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer profile changed; request recommendations again.");
        }

        int age = Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears();
        if (age != prepared.calculatedAge()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Customer age changed while recommendations were being generated; retry the request."
            );
        }
        List<InsuranceProduct> currentCandidates =
                productRepository.findEligibleAvailableForCustomer(customerId, age);
        Map<String, InsuranceProduct> currentByCode = currentCandidates.stream()
                .collect(Collectors.toMap(InsuranceProduct::getProductCode, Function.identity()));
        Set<String> preparedCodes = prepared.candidates().stream()
                .map(RecommendationProvider.Candidate::productCode)
                .collect(Collectors.toSet());
        List<RecommendationProvider.Suggestion> validatedSuggestions = validateSuggestions(
                suggestions,
                preparedCodes,
                currentByCode
        );
        if (metadata.modelName() == null || metadata.modelName().isBlank() || metadata.modelName().length() > 100) {
            throw new IllegalStateException("Recommendation model name must fit the supplied schema's 100-character limit.");
        }

        LocalDateTime requestedAt = nextRequestTimestamp(customerId);
        List<PolicyRecommendation> persisted = validatedSuggestions.stream()
                .map(suggestion -> new PolicyRecommendation(
                        customer,
                        currentByCode.get(suggestion.productCode()),
                        prepared.insuranceGoals(),
                        age,
                        suggestion.recommendationReason().trim(),
                        suggestion.suitability().toUpperCase(Locale.ROOT),
                        metadata.modelName(),
                        requestedAt
                ))
                .toList();
        List<PolicyRecommendation> saved = recommendationRepository.saveAllAndFlush(persisted);
        return response(
                saved.isEmpty() ? null : saved.get(0).getId(),
                customer.getDateOfBirth(),
                age,
                prepared.insuranceGoals(),
                metadata.modelName(),
                requestedAt.toInstant(ZoneOffset.UTC),
                saved
        );
    }

    @Transactional(readOnly = true)
    public RecommendationResponse get(Long customerId, Long recommendationId) {
        PolicyRecommendation first = recommendationRepository.findByIdAndCustomer_Id(recommendationId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<PolicyRecommendation> group = recommendationRepository
                .findAllByCustomer_IdAndCustomerGoalAndCustomerAgeAndAiModelAndCreatedAtOrderByIdAsc(
                        customerId,
                        first.getCustomerGoal(),
                        first.getCustomerAge(),
                        first.getAiModel(),
                        first.getCreatedAt()
                );
        return response(
                first.getId(),
                first.getCustomer().getDateOfBirth(),
                first.getCustomerAge(),
                first.getCustomerGoal(),
                first.getAiModel(),
                first.getCreatedAt().toInstant(ZoneOffset.UTC),
                group
        );
    }

    private List<RecommendationProvider.Suggestion> validateSuggestions(
            List<RecommendationProvider.Suggestion> suggestions,
            Set<String> preparedCodes,
            Map<String, InsuranceProduct> currentByCode
    ) {
        if (suggestions == null || suggestions.size() > 3) {
            throw invalidProviderResponse();
        }
        Set<String> seen = new HashSet<>();
        for (RecommendationProvider.Suggestion suggestion : suggestions) {
            if (suggestion == null
                    || suggestion.productCode() == null
                    || !preparedCodes.contains(suggestion.productCode())
                    || !seen.add(suggestion.productCode())
                    || suggestion.recommendationReason() == null
                    || suggestion.recommendationReason().isBlank()
                    || suggestion.recommendationReason().length() > 4000
                    || suggestion.suitability() == null
                    || !ALLOWED_SUITABILITY.contains(suggestion.suitability().toUpperCase(Locale.ROOT))) {
                throw invalidProviderResponse();
            }
            if (!currentByCode.containsKey(suggestion.productCode())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Product availability changed while recommendations were being generated; retry the request."
                );
            }
        }
        return suggestions;
    }

    private ResponseStatusException invalidProviderResponse() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "The recommendation provider returned results that could not be verified."
        );
    }

    private LocalDateTime nextRequestTimestamp(Long customerId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        return recommendationRepository.findFirstByCustomer_IdOrderByCreatedAtDesc(customerId)
                .map(last -> now.isAfter(last.getCreatedAt()) ? now : last.getCreatedAt().plusNanos(1_000))
                .orElse(now);
    }

    private RecommendationResponse response(
            Long requestId,
            LocalDate dateOfBirth,
            int age,
            String goals,
            String modelName,
            Instant requestedAt,
            List<PolicyRecommendation> items
    ) {
        List<RecommendationItemResponse> recommendationItems = java.util.stream.IntStream.range(0, items.size())
                .mapToObj(index -> RecommendationItemResponse.from(items.get(index), index + 1))
                .toList();
        return new RecommendationResponse(
                requestId,
                dateOfBirth,
                age,
                goals,
                modelName,
                requestedAt,
                recommendationItems
        );
    }
}

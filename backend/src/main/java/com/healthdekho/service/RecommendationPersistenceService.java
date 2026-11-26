package com.healthdekho.service;

import com.healthdekho.dto.RecommendationItemResponse;
import com.healthdekho.dto.RecommendationResponse;
import com.healthdekho.entity.AiRecommendationItem;
import com.healthdekho.entity.AiRecommendationRequest;
import com.healthdekho.entity.Customer;
import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.repository.AiRecommendationItemRepository;
import com.healthdekho.repository.AiRecommendationRequestRepository;
import com.healthdekho.repository.CustomerRepository;
import com.healthdekho.repository.InsuranceProductRepository;
import com.healthdekho.recommendation.RecommendationProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationPersistenceService {

    private static final Set<String> ALLOWED_SUITABILITY = Set.of("HIGH", "MODERATE", "LOW");

    private final CustomerRepository customerRepository;
    private final InsuranceProductRepository productRepository;
    private final AiRecommendationRequestRepository requestRepository;
    private final AiRecommendationItemRepository itemRepository;

    public RecommendationPersistenceService(
            CustomerRepository customerRepository,
            InsuranceProductRepository productRepository,
            AiRecommendationRequestRepository requestRepository,
            AiRecommendationItemRepository itemRepository
    ) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.requestRepository = requestRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public RecommendationResponse save(
            UUID customerId,
            PreparedRecommendation prepared,
            RecommendationProvider.Metadata metadata,
            List<RecommendationProvider.Suggestion> suggestions
    ) {
        Customer customer = customerRepository.findById(customerId)
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

        AiRecommendationRequest request = requestRepository.saveAndFlush(new AiRecommendationRequest(
                customer,
                prepared.dateOfBirth(),
                prepared.insuranceGoals(),
                prepared.preferredTerm(),
                prepared.preferredPolicyType(),
                age,
                metadata.modelName(),
                metadata.modelVersion(),
                metadata.promptVersion()
        ));
        List<AiRecommendationItem> persistedItems = java.util.stream.IntStream.range(0, validatedSuggestions.size())
                .mapToObj(index -> {
                    RecommendationProvider.Suggestion suggestion = validatedSuggestions.get(index);
                    return new AiRecommendationItem(
                        request,
                        currentByCode.get(suggestion.productCode()),
                        index + 1,
                        suggestion.recommendationReason().trim(),
                        suggestion.suitability().toUpperCase(Locale.ROOT),
                        false
                    );
                })
                .toList();
        List<AiRecommendationItem> savedItems = itemRepository.saveAll(persistedItems);
        return response(request, savedItems);
    }

    @Transactional(readOnly = true)
    public RecommendationResponse get(UUID customerId, UUID requestId) {
        AiRecommendationRequest request = requestRepository.findByIdAndCustomer_Id(requestId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return response(request, itemRepository.findAllByRequest_IdOrderByRankOrderAsc(requestId));
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

    private RecommendationResponse response(
            AiRecommendationRequest request,
            List<AiRecommendationItem> items
    ) {
        List<RecommendationItemResponse> recommendationItems = items.stream()
                .map(RecommendationItemResponse::from)
                .toList();
        return new RecommendationResponse(
                request.getId(),
                request.getDateOfBirth(),
                request.getCalculatedAge(),
                request.getInsuranceGoals(),
                request.getPreferredTerm(),
                request.getPreferredPolicyType(),
                request.getModelName(),
                request.getModelVersion(),
                request.getPromptVersion(),
                request.getRequestedAt(),
                recommendationItems
        );
    }
}

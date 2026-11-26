package com.healthdekho.service;

import com.healthdekho.dto.CreateRecommendationRequest;
import com.healthdekho.dto.RecommendationResponse;
import com.healthdekho.recommendation.InvalidRecommendationProviderResponseException;
import com.healthdekho.recommendation.RecommendationProvider;
import com.healthdekho.recommendation.RecommendationProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class RecommendationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationService.class);

    private final RecommendationPreparationService preparationService;
    private final RecommendationProvider provider;
    private final RecommendationPersistenceService persistenceService;

    public RecommendationService(
            RecommendationPreparationService preparationService,
            RecommendationProvider provider,
            RecommendationPersistenceService persistenceService
    ) {
        this.preparationService = preparationService;
        this.provider = provider;
        this.persistenceService = persistenceService;
    }

    public RecommendationResponse create(UUID customerId, CreateRecommendationRequest request) {
        PreparedRecommendation prepared = preparationService.prepare(customerId, request);
        List<RecommendationProvider.Suggestion> suggestions;
        if (prepared.candidates().isEmpty()) {
            suggestions = List.of();
        } else {
            try {
                suggestions = provider.recommend(new RecommendationProvider.Context(
                        prepared.calculatedAge(),
                        prepared.insuranceGoals(),
                        prepared.preferredTerm(),
                        prepared.preferredPolicyType(),
                        prepared.candidates()
                ));
            } catch (RecommendationProviderUnavailableException exception) {
                LOGGER.warn("Configured recommendation provider is unavailable.", exception);
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Recommendations are temporarily unavailable."
                );
            } catch (InvalidRecommendationProviderResponseException exception) {
                LOGGER.warn("Configured recommendation provider returned an invalid response.", exception);
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "The recommendation provider returned an invalid response."
                );
            }
        }
        return persistenceService.save(customerId, prepared, provider.metadata(), suggestions);
    }

    public RecommendationResponse get(UUID customerId, UUID requestId) {
        return persistenceService.get(customerId, requestId);
    }
}

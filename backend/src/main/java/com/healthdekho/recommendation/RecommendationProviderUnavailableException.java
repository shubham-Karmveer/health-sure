package com.healthdekho.recommendation;

public class RecommendationProviderUnavailableException extends RuntimeException {

    public RecommendationProviderUnavailableException(Throwable cause) {
        super("The configured recommendation provider is unavailable.", cause);
    }
}

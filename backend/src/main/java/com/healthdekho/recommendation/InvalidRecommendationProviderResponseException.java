package com.healthdekho.recommendation;

public class InvalidRecommendationProviderResponseException extends RuntimeException {

    public InvalidRecommendationProviderResponseException(Throwable cause) {
        super("The recommendation provider returned an invalid response.", cause);
    }

    public InvalidRecommendationProviderResponseException(String message) {
        super(message);
    }
}

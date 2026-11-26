package com.healthdekho.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateRecommendationRequest(
        @Past(message = "Date of birth must be in the past.")
        LocalDate dateOfBirth,
        @Size(max = 2000, message = "Insurance goals must be 2000 characters or fewer.")
        String insuranceGoals,
        @Size(max = 100, message = "Preferred term must be 100 characters or fewer.")
        String preferredTerm,
        @Size(max = 40, message = "Insurance type must be 40 characters or fewer.")
        String preferredPolicyType
) {
}

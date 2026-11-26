package com.healthdekho.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PolicyRegistrationRequest(
        @NotBlank @Size(max = 40) String productCode,
        @NotNull @FutureOrPresent(message = "Start date must be today or a future date.") LocalDate startDate
) {
}

package com.healthdekho.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PaymentRequest(
        @NotBlank @Size(max = 40) String paymentMethod,
        @NotEmpty List<@NotNull @Valid PaymentLineRequest> payments
) {
}

package com.healthdekho.controller;

import com.healthdekho.dto.InstallmentDetailResponse;
import com.healthdekho.dto.PaymentBatchResponse;
import com.healthdekho.dto.PaymentHistoryResponse;
import com.healthdekho.dto.PaymentRequest;
import com.healthdekho.dto.PolicyDetailsResponse;
import com.healthdekho.dto.PolicyRegistrationRequest;
import com.healthdekho.dto.PolicyRegistrationResponse;
import com.healthdekho.service.PolicyPaymentService;
import com.healthdekho.service.PolicyRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyRegistrationService policyRegistrationService;
    private final PolicyPaymentService policyPaymentService;

    public PolicyController(
            PolicyRegistrationService policyRegistrationService,
            PolicyPaymentService policyPaymentService
    ) {
        this.policyRegistrationService = policyRegistrationService;
        this.policyPaymentService = policyPaymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyRegistrationResponse register(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PolicyRegistrationRequest request
    ) {
        return policyRegistrationService.register(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping("/{policyNumber}")
    public PolicyDetailsResponse details(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String policyNumber
    ) {
        return policyPaymentService.getPolicyDetails(UUID.fromString(jwt.getSubject()), policyNumber);
    }

    @GetMapping("/{policyNumber}/installments")
    public List<InstallmentDetailResponse> installments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String policyNumber
    ) {
        return policyPaymentService.getInstallments(UUID.fromString(jwt.getSubject()), policyNumber);
    }

    @GetMapping("/{policyNumber}/payments")
    public List<PaymentHistoryResponse> payments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String policyNumber
    ) {
        return policyPaymentService.getPaymentHistory(UUID.fromString(jwt.getSubject()), policyNumber);
    }

    @PostMapping("/{policyNumber}/payments")
    public PaymentBatchResponse recordPayments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String policyNumber,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request
    ) {
        return policyPaymentService.recordPayments(
                UUID.fromString(jwt.getSubject()),
                policyNumber,
                idempotencyKey,
                request
        );
    }
}

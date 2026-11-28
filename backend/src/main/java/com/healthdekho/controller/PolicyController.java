package com.healthdekho.controller;

import com.healthdekho.dto.InstallmentDetailResponse;
import com.healthdekho.dto.PageResponse;
import com.healthdekho.dto.PaymentBatchResponse;
import com.healthdekho.dto.PaymentHistoryResponse;
import com.healthdekho.dto.PaymentRequest;
import com.healthdekho.dto.PolicyDetailsResponse;
import com.healthdekho.dto.PolicyRegistrationRequest;
import com.healthdekho.dto.PolicyRegistrationResponse;
import com.healthdekho.dto.PolicySearchResultResponse;
import com.healthdekho.service.PolicyPaymentService;
import com.healthdekho.service.PolicyRegistrationService;
import com.healthdekho.service.PolicySearchService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyRegistrationService policyRegistrationService;
    private final PolicyPaymentService policyPaymentService;
    private final PolicySearchService policySearchService;

    public PolicyController(
            PolicyRegistrationService policyRegistrationService,
            PolicyPaymentService policyPaymentService,
            PolicySearchService policySearchService
    ) {
        this.policyRegistrationService = policyRegistrationService;
        this.policyPaymentService = policyPaymentService;
        this.policySearchService = policySearchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyRegistrationResponse register(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PolicyRegistrationRequest request
    ) {
        return policyRegistrationService.register(Long.valueOf(jwt.getSubject()), request);
    }

    @GetMapping("/{policyNumber}")
    public PolicyDetailsResponse details(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String policyNumber
    ) {
        return policyPaymentService.getPolicyDetails(Long.valueOf(jwt.getSubject()), policyNumber);
    }

    @GetMapping("/search")
    public PageResponse<PolicySearchResultResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String policyNumber,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return policySearchService.search(
                Long.valueOf(jwt.getSubject()),
                policyNumber,
                customerName,
                dateOfBirth,
                page,
                size
        );
    }

    @GetMapping("/{policyNumber}/installments")
    public List<InstallmentDetailResponse> installments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String policyNumber
    ) {
        return policyPaymentService.getInstallments(Long.valueOf(jwt.getSubject()), policyNumber);
    }

    @GetMapping("/{policyNumber}/payments")
    public List<PaymentHistoryResponse> payments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String policyNumber
    ) {
        return policyPaymentService.getPaymentHistory(Long.valueOf(jwt.getSubject()), policyNumber);
    }

    @PostMapping("/{policyNumber}/payments")
    public PaymentBatchResponse recordPayments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String policyNumber,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request
    ) {
        return policyPaymentService.recordPayments(
                Long.valueOf(jwt.getSubject()),
                policyNumber,
                idempotencyKey,
                request
        );
    }
}

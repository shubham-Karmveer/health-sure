package com.healthdekho.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdekho.dto.InstallmentDetailResponse;
import com.healthdekho.dto.PaymentBatchResponse;
import com.healthdekho.dto.PaymentHistoryResponse;
import com.healthdekho.dto.PaymentLineRequest;
import com.healthdekho.dto.PaymentReceipt;
import com.healthdekho.dto.PaymentRequest;
import com.healthdekho.dto.PolicyDetailsResponse;
import com.healthdekho.entity.Customer;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.PaymentIdempotencyRecord;
import com.healthdekho.entity.PremiumInstallment;
import com.healthdekho.entity.PremiumPayment;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.CustomerRepository;
import com.healthdekho.repository.PaymentIdempotencyRepository;
import com.healthdekho.repository.PremiumInstallmentRepository;
import com.healthdekho.repository.PremiumPaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PolicyPaymentService {

    private static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.10");
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final CustomerRepository customerRepository;
    private final CustomerPolicyRepository policyRepository;
    private final PremiumInstallmentRepository installmentRepository;
    private final PremiumPaymentRepository paymentRepository;
    private final PaymentIdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public PolicyPaymentService(
            CustomerRepository customerRepository,
            CustomerPolicyRepository policyRepository,
            PremiumInstallmentRepository installmentRepository,
            PremiumPaymentRepository paymentRepository,
            PaymentIdempotencyRepository idempotencyRepository,
            ObjectMapper objectMapper
    ) {
        this.customerRepository = customerRepository;
        this.policyRepository = policyRepository;
        this.installmentRepository = installmentRepository;
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentBatchResponse recordPayments(
            UUID customerId,
            String policyNumber,
            String idempotencyKey,
            PaymentRequest request
    ) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String paymentMethod = request.paymentMethod().trim();
        String requestHash = requestHash(policyNumber, paymentMethod, request.payments());

        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var existingRequest = idempotencyRepository
                .findByCustomer_IdAndIdempotencyKey(customerId, normalizedKey);
        if (existingRequest.isPresent()) {
            PaymentIdempotencyRecord record = existingRequest.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This idempotency key was already used for a different payment request."
                );
            }
            return readCachedResponse(record);
        }

        CustomerPolicy policy = policyRepository
                .findForUpdateByPolicyNumberAndCustomerId(policyNumber, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Set<Integer> installmentNumbers = request.payments().stream()
                .map(PaymentLineRequest::installmentNumber)
                .collect(Collectors.toSet());
        if (installmentNumbers.size() != request.payments().size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Each installment may appear only once in a payment request."
            );
        }

        List<Integer> sortedNumbers = installmentNumbers.stream().sorted().toList();
        List<PremiumInstallment> installments = installmentRepository
                .findAllByPolicy_IdAndInstallmentNumberInOrderByInstallmentNumberAsc(
                        policy.getId(),
                        sortedNumbers
                );
        if (installments.size() != sortedNumbers.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "One or more installments are not part of this policy."
            );
        }
        Map<Integer, PremiumInstallment> installmentByNumber = installments.stream()
                .collect(Collectors.toMap(PremiumInstallment::getInstallmentNumber, Function.identity()));

        LocalDate today = LocalDate.now();
        List<PaymentPlan> paymentPlans = request.payments().stream()
                .sorted(Comparator.comparingInt(PaymentLineRequest::installmentNumber))
                .map(line -> planPayment(installmentByNumber.get(line.installmentNumber()), line, today))
                .toList();

        PaymentIdempotencyRecord idempotencyRecord = idempotencyRepository.save(
                new PaymentIdempotencyRecord(customer, policy, normalizedKey, requestHash)
        );
        List<PremiumPayment> payments = new ArrayList<>(paymentPlans.size());
        for (PaymentPlan plan : paymentPlans) {
            if (plan.penaltyToAssess().signum() > 0) {
                plan.installment().assessPenalty(plan.penaltyToAssess());
            }
            plan.installment().applyPayment(plan.premiumAmount(), plan.penaltyAmount());
            plan.installment().updatePaymentStatus(today);
            payments.add(new PremiumPayment(
                    plan.installment(),
                    idempotencyRecord,
                    "HD-TXN-" + UUID.randomUUID(),
                    plan.amountPaid(),
                    plan.premiumAmount(),
                    plan.penaltyAmount(),
                    paymentMethod
            ));
        }
        installmentRepository.saveAll(installments);
        List<PremiumPayment> savedPayments = paymentRepository.saveAll(payments);
        PaymentBatchResponse response = paymentResponse(policy, installments, savedPayments);
        idempotencyRecord.setResponseJson(writeCachedResponse(response));
        idempotencyRepository.save(idempotencyRecord);
        return response;
    }

    @Transactional
    public PolicyDetailsResponse getPolicyDetails(UUID customerId, String policyNumber) {
        CustomerPolicy policy = ownedPolicy(customerId, policyNumber);
        LocalDate today = LocalDate.now();
        List<PremiumInstallment> installments = updateAndLoadInstallments(policy, today);
        Totals totals = calculateTotals(policy, installments);
        BigDecimal estimatedPenalty = estimatePenalty(installments, today);
        return new PolicyDetailsResponse(
                policy.getPolicyNumber(),
                policy.getCustomer().getFullName(),
                policy.getCustomer().getDateOfBirth(),
                policy.getCustomer().getPhone(),
                policy.getProduct().getPolicyName(),
                policy.getProduct().getPolicyType(),
                policy.getTermMonths(),
                policy.getStartDate(),
                policy.getMaturityDate(),
                policy.getPremiumFrequency(),
                policy.getPremiumAmount(),
                policy.getSumInsured(),
                policy.getTotalPremiumPayable(),
                totals.premiumPaid(),
                totals.remainingPremium(),
                totals.penaltyAssessed(),
                totals.penaltyPaid(),
                totals.remainingPenalty(),
                totals.remainingBalance(),
                estimatedPenalty,
                totals.remainingBalance().add(estimatedPenalty),
                policy.getStatus()
        );
    }

    @Transactional
    public List<InstallmentDetailResponse> getInstallments(UUID customerId, String policyNumber) {
        CustomerPolicy policy = ownedPolicy(customerId, policyNumber);
        LocalDate today = LocalDate.now();
        return updateAndLoadInstallments(policy, today).stream()
                .map(installment -> InstallmentDetailResponse.from(installment, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPaymentHistory(UUID customerId, String policyNumber) {
        CustomerPolicy policy = ownedPolicy(customerId, policyNumber);
        return paymentRepository.findAllByInstallment_Policy_IdOrderByPaymentDateDesc(policy.getId())
                .stream()
                .map(PaymentHistoryResponse::from)
                .toList();
    }

    private PaymentPlan planPayment(
            PremiumInstallment installment,
            PaymentLineRequest line,
            LocalDate today
    ) {
        BigDecimal principalOutstanding = installment.getScheduledAmount().subtract(installment.getPaidAmount());
        BigDecimal penaltyToAssess = ZERO;
        if (today.isAfter(installment.getGracePeriodEndDate())
                && principalOutstanding.signum() > 0
                && installment.getPenaltyAmount().signum() == 0) {
            penaltyToAssess = principalOutstanding.multiply(LATE_PENALTY_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal penaltyOutstanding = installment.getPenaltyAmount()
                .add(penaltyToAssess)
                .subtract(installment.getPaidPenaltyAmount());
        BigDecimal totalOutstanding = principalOutstanding.add(penaltyOutstanding);
        if (line.amountPaid().compareTo(totalOutstanding) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The payment exceeds the outstanding principal and penalty."
            );
        }

        BigDecimal penaltyPaid = line.amountPaid().min(penaltyOutstanding);
        BigDecimal premiumPaid = line.amountPaid().subtract(penaltyPaid);
        return new PaymentPlan(
                installment,
                line.amountPaid(),
                premiumPaid,
                penaltyPaid,
                penaltyToAssess
        );
    }

    private List<PremiumInstallment> updateAndLoadInstallments(CustomerPolicy policy, LocalDate today) {
        List<PremiumInstallment> installments =
                installmentRepository.findAllByPolicy_IdOrderByInstallmentNumberAsc(policy.getId());
        installments.forEach(installment -> installment.updatePaymentStatus(today));
        return installments;
    }

    private BigDecimal estimatePenalty(List<PremiumInstallment> installments, LocalDate today) {
        return installments.stream()
                .map(installment -> estimatedPenalty(installment, today))
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal estimatedPenalty(PremiumInstallment installment, LocalDate today) {
        BigDecimal principalOutstanding = installment.getScheduledAmount().subtract(installment.getPaidAmount());
        if (today.isAfter(installment.getGracePeriodEndDate())
                && principalOutstanding.signum() > 0
                && installment.getPenaltyAmount().signum() == 0) {
            return principalOutstanding.multiply(LATE_PENALTY_RATE).setScale(2, RoundingMode.HALF_UP);
        }
        return ZERO;
    }

    private CustomerPolicy ownedPolicy(UUID customerId, String policyNumber) {
        return policyRepository.findByPolicyNumberAndCustomer_Id(policyNumber, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private PaymentBatchResponse paymentResponse(
            CustomerPolicy policy,
            List<PremiumInstallment> installments,
            List<PremiumPayment> payments
    ) {
        Totals totals = calculateTotals(policy, installments);
        return new PaymentBatchResponse(
                policy.getPolicyNumber(),
                "Payment recorded successfully.",
                policy.getTotalPremiumPayable(),
                totals.premiumPaid(),
                totals.remainingPremium(),
                totals.penaltyAssessed(),
                totals.penaltyPaid(),
                totals.remainingPenalty(),
                totals.remainingBalance(),
                payments.stream().map(PaymentReceipt::from).toList()
        );
    }

    private Totals calculateTotals(CustomerPolicy policy, List<PremiumInstallment> installments) {
        BigDecimal premiumPaid = installments.stream()
                .map(PremiumInstallment::getPaidAmount)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal penaltyAssessed = installments.stream()
                .map(PremiumInstallment::getPenaltyAmount)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal penaltyPaid = installments.stream()
                .map(PremiumInstallment::getPaidPenaltyAmount)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal remainingPremium = policy.getTotalPremiumPayable().subtract(premiumPaid);
        BigDecimal remainingPenalty = penaltyAssessed.subtract(penaltyPaid);
        return new Totals(
                premiumPaid,
                remainingPremium,
                penaltyAssessed,
                penaltyPaid,
                remainingPenalty,
                remainingPremium.add(remainingPenalty)
        );
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A non-empty Idempotency-Key header of at most 128 characters is required."
            );
        }
        return idempotencyKey.trim();
    }

    private String requestHash(String policyNumber, String paymentMethod, List<PaymentLineRequest> payments) {
        String paymentLines = payments.stream()
                .sorted(Comparator.comparingInt(PaymentLineRequest::installmentNumber))
                .map(line -> line.installmentNumber() + ":"
                        + line.amountPaid().stripTrailingZeros().toPlainString())
                .collect(Collectors.joining("|"));
        String canonicalRequest = policyNumber + "\n"
                + paymentMethod.toUpperCase(Locale.ROOT) + "\n"
                + paymentLines;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for payment idempotency.", exception);
        }
    }

    private PaymentBatchResponse readCachedResponse(PaymentIdempotencyRecord record) {
        try {
            return objectMapper.readValue(record.getResponseJson(), PaymentBatchResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("A stored payment idempotency response could not be read.", exception);
        }
    }

    private String writeCachedResponse(PaymentBatchResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The payment idempotency response could not be stored.", exception);
        }
    }

    private record PaymentPlan(
            PremiumInstallment installment,
            BigDecimal amountPaid,
            BigDecimal premiumAmount,
            BigDecimal penaltyAmount,
            BigDecimal penaltyToAssess
    ) {
    }

    private record Totals(
            BigDecimal premiumPaid,
            BigDecimal remainingPremium,
            BigDecimal penaltyAssessed,
            BigDecimal penaltyPaid,
            BigDecimal remainingPenalty,
            BigDecimal remainingBalance
    ) {
    }
}

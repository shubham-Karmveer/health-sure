package com.healthdekho.service;

import com.healthdekho.dto.InstallmentDetailResponse;
import com.healthdekho.dto.PaymentBatchResponse;
import com.healthdekho.dto.PaymentHistoryResponse;
import com.healthdekho.dto.PaymentLineRequest;
import com.healthdekho.dto.PaymentReceipt;
import com.healthdekho.dto.PaymentRequest;
import com.healthdekho.dto.PolicyDetailsResponse;
import com.healthdekho.entity.Customer;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.PremiumInstallment;
import com.healthdekho.entity.PremiumPayment;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.CustomerRepository;
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

    public PolicyPaymentService(
            CustomerRepository customerRepository,
            CustomerPolicyRepository policyRepository,
            PremiumInstallmentRepository installmentRepository,
            PremiumPaymentRepository paymentRepository
    ) {
        this.customerRepository = customerRepository;
        this.policyRepository = policyRepository;
        this.installmentRepository = installmentRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentBatchResponse recordPayments(
            Long customerId,
            String policyNumber,
            String idempotencyKey,
            PaymentRequest request
    ) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String paymentMethod = request.paymentMethod().trim();
        if (paymentMethod.isEmpty() || paymentMethod.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment method must be 1 to 30 characters.");
        }
        Set<Integer> installmentNumbers = request.payments().stream()
                .map(PaymentLineRequest::installmentNumber)
                .collect(Collectors.toSet());
        if (installmentNumbers.size() != request.payments().size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Each installment may appear only once in a payment request."
            );
        }
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        CustomerPolicy policy = policyRepository
                .findForUpdateByPolicyNumberAndCustomerId(policyNumber, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String receiptNumber = receiptNumber(customer.getId(), normalizedKey);
        List<PremiumPayment> priorPayments =
                paymentRepository.findAllByReceiptNumberOrderByInstallment_InstallmentNumberAsc(receiptNumber);
        if (!priorPayments.isEmpty()) {
            if (!matchesRequest(policy, priorPayments, paymentMethod, request.payments())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This idempotency key was already used for a different payment request."
                );
            }
            List<PremiumInstallment> policyInstallments =
                    updateAndLoadInstallments(policy, LocalDate.now());
            return paymentResponse(policy, policyInstallments, priorPayments);
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

        List<PremiumPayment> payments = new ArrayList<>(paymentPlans.size());
        for (PaymentPlan plan : paymentPlans) {
            if (plan.penaltyToAssess().signum() > 0) {
                plan.installment().assessPenalty(plan.penaltyToAssess());
            }
            plan.installment().applyPayment(plan.premiumAmount(), plan.penaltyAmount());
            plan.installment().updatePaymentStatus(today);
            payments.add(new PremiumPayment(
                    plan.installment(),
                    receiptNumber,
                    "HD-TXN-" + UUID.randomUUID(),
                    plan.premiumAmount(),
                    plan.penaltyAmount(),
                    paymentMethod
            ));
        }
        installmentRepository.saveAll(installments);
        List<PremiumPayment> savedPayments = paymentRepository.saveAll(payments);
        return paymentResponse(policy, installments, savedPayments);
    }

    @Transactional
    public PolicyDetailsResponse getPolicyDetails(Long customerId, String policyNumber) {
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
    public List<InstallmentDetailResponse> getInstallments(Long customerId, String policyNumber) {
        CustomerPolicy policy = ownedPolicy(customerId, policyNumber);
        LocalDate today = LocalDate.now();
        return updateAndLoadInstallments(policy, today).stream()
                .map(installment -> InstallmentDetailResponse.from(installment, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPaymentHistory(Long customerId, String policyNumber) {
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
        if (premiumPaid.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Each payment must include a positive premium amount."
            );
        }
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

    private CustomerPolicy ownedPolicy(Long customerId, String policyNumber) {
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

    private String receiptNumber(Long customerId, String idempotencyKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((customerId + ":" + idempotencyKey).getBytes(StandardCharsets.UTF_8));
            return "HD-" + HexFormat.of().formatHex(digest, 0, 20);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for payment receipts.", exception);
        }
    }

    private boolean matchesRequest(
            CustomerPolicy policy,
            List<PremiumPayment> priorPayments,
            String paymentMethod,
            List<PaymentLineRequest> requestedPayments
    ) {
        if (priorPayments.size() != requestedPayments.size()
                || priorPayments.stream().anyMatch(payment ->
                        !payment.getInstallmentPolicyId().equals(policy.getId())
                                || !paymentMethod.equals(payment.getPaymentMethod()))) {
            return false;
        }
        Map<Integer, PremiumPayment> priorByInstallment = priorPayments.stream()
                .collect(Collectors.toMap(PremiumPayment::getInstallmentNumber, Function.identity()));
        return requestedPayments.stream().allMatch(line -> {
            PremiumPayment prior = priorByInstallment.get(line.installmentNumber());
            return prior != null && prior.getAmountPaid().compareTo(line.amountPaid()) == 0;
        });
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

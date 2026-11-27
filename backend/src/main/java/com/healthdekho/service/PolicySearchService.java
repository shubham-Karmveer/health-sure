package com.healthdekho.service;

import com.healthdekho.dto.PageResponse;
import com.healthdekho.dto.PolicySearchResultResponse;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.PolicyPaymentSummaryProjection;
import com.healthdekho.repository.PremiumInstallmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PolicySearchService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final int MAX_PAGE_SIZE = 50;

    private final CustomerPolicyRepository policyRepository;
    private final PremiumInstallmentRepository installmentRepository;

    public PolicySearchService(
            CustomerPolicyRepository policyRepository,
            PremiumInstallmentRepository installmentRepository
    ) {
        this.policyRepository = policyRepository;
        this.installmentRepository = installmentRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PolicySearchResultResponse> search(
            UUID customerId,
            String policyNumber,
            String customerName,
            LocalDate dateOfBirth,
            int page,
            int size
    ) {
        String normalizedPolicyNumber = normalize(policyNumber);
        String normalizedCustomerName = normalize(customerName);
        validateCriteria(normalizedPolicyNumber, normalizedCustomerName, dateOfBirth, page, size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("policyNumber"))
        );
        Page<CustomerPolicy> policyPage;
        if (normalizedPolicyNumber != null && dateOfBirth != null) {
            policyPage = policyRepository.searchOwnedByPolicyNumberAndDateOfBirth(
                    customerId, normalizedPolicyNumber, dateOfBirth, pageRequest);
        } else if (normalizedPolicyNumber != null) {
            policyPage = policyRepository.searchOwnedByPolicyNumber(
                    customerId, normalizedPolicyNumber, pageRequest);
        } else {
            policyPage = policyRepository.searchOwnedByCustomerName(
                    customerId, normalizedCustomerName, pageRequest);
        }

        List<UUID> policyIds = policyPage.getContent().stream().map(CustomerPolicy::getId).toList();
        Map<UUID, PolicyPaymentSummaryProjection> paymentSummaries = policyIds.isEmpty()
                ? Map.of()
                : installmentRepository.summarizePaymentsByPolicy(policyIds, LocalDate.now())
                        .stream()
                        .collect(Collectors.toMap(
                                PolicyPaymentSummaryProjection::getPolicyId,
                                Function.identity()
                        ));

        Page<PolicySearchResultResponse> responsePage = policyPage.map(policy ->
                toResponse(policy, paymentSummaries.get(policy.getId())));
        return PageResponse.from(responsePage);
    }

    private PolicySearchResultResponse toResponse(
            CustomerPolicy policy,
            PolicyPaymentSummaryProjection paymentSummary
    ) {
        BigDecimal premiumPaid = paymentSummary == null || paymentSummary.getPremiumPaid() == null
                ? ZERO
                : paymentSummary.getPremiumPaid();
        BigDecimal remainingPremium = policy.getTotalPremiumPayable().subtract(premiumPaid);
        String paymentStatus = paymentStatus(paymentSummary);
        return new PolicySearchResultResponse(
                policy.getPolicyNumber(),
                policy.getCustomer().getFullName(),
                policy.getCustomer().getDateOfBirth(),
                policy.getProduct().getPolicyName(),
                policy.getProduct().getPolicyType(),
                policy.getStartDate(),
                policy.getMaturityDate(),
                policy.getTotalPremiumPayable(),
                paymentStatus,
                premiumPaid,
                remainingPremium
        );
    }

    private String paymentStatus(PolicyPaymentSummaryProjection paymentSummary) {
        if (paymentSummary == null) {
            return "PENDING";
        }
        if (paymentSummary.getIncompleteInstallments() == 0) {
            return "PAID";
        }
        if (paymentSummary.getOverdueInstallments() > 0) {
            return "OVERDUE";
        }
        if (paymentSummary.getInstallmentsWithPayment() > 0) {
            return "PARTIALLY_PAID";
        }
        return "PENDING";
    }

    private void validateCriteria(
            String policyNumber,
            String customerName,
            LocalDate dateOfBirth,
            int page,
            int size
    ) {
        boolean policyNumberOnly = policyNumber != null && customerName == null && dateOfBirth == null;
        boolean customerNameOnly = policyNumber == null && customerName != null && dateOfBirth == null;
        boolean policyNumberAndDateOfBirth = policyNumber != null && customerName == null && dateOfBirth != null;
        if (!policyNumberOnly && !customerNameOnly && !policyNumberAndDateOfBirth) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Search by policy identifier, customer name, or policy identifier and date of birth."
            );
        }
        if (policyNumber != null && policyNumber.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy identifier exceeds 50 characters.");
        }
        if (customerName != null && customerName.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name exceeds 120 characters.");
        }
        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth cannot be in the future.");
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and page size must be between 1 and 50."
            );
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

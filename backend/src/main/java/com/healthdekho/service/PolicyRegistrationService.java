package com.healthdekho.service;

import com.healthdekho.dto.InstallmentResponse;
import com.healthdekho.dto.PolicyRegistrationRequest;
import com.healthdekho.dto.PolicyRegistrationResponse;
import com.healthdekho.entity.Customer;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.entity.PremiumInstallment;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.CustomerRepository;
import com.healthdekho.repository.InsuranceProductRepository;
import com.healthdekho.repository.PremiumInstallmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PolicyRegistrationService {

    private final CustomerRepository customerRepository;
    private final InsuranceProductRepository productRepository;
    private final CustomerPolicyRepository policyRepository;
    private final PremiumInstallmentRepository installmentRepository;

    public PolicyRegistrationService(
            CustomerRepository customerRepository,
            InsuranceProductRepository productRepository,
            CustomerPolicyRepository policyRepository,
            PremiumInstallmentRepository installmentRepository
    ) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.policyRepository = policyRepository;
        this.installmentRepository = installmentRepository;
    }

    @Transactional
    public PolicyRegistrationResponse register(UUID customerId, PolicyRegistrationRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        InsuranceProduct product = productRepository.findByProductCodeAndActiveTrue(request.productCode().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "The selected product is not available for registration."
                ));

        validateEligibility(customer, product);
        int intervalMonths = intervalMonths(product.getPremiumFrequency());
        int termMonths = product.getTermMonths();
        if (termMonths <= 0 || termMonths % intervalMonths != 0
                || product.getPremiumAmount() == null || product.getPremiumAmount().signum() <= 0
                || product.getSumInsured() == null || product.getSumInsured().signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The selected product has an invalid premium schedule."
            );
        }
        if (policyRepository.existsByCustomer_IdAndProduct_Id(customerId, product.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already have a policy for this product."
            );
        }

        int installmentCount = termMonths / intervalMonths;
        BigDecimal totalPremiumPayable = product.getPremiumAmount()
                .multiply(BigDecimal.valueOf(installmentCount))
                .setScale(2, RoundingMode.UNNECESSARY);
        if (totalPremiumPayable.precision() > 19) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The selected product has an invalid premium schedule."
            );
        }
        LocalDate maturityDate = request.startDate().plusMonths(termMonths).minusDays(1);
        CustomerPolicy policy = policyRepository.save(new CustomerPolicy(
                "HD-POL-" + UUID.randomUUID(),
                customer,
                product,
                termMonths,
                request.startDate(),
                maturityDate,
                product.getPremiumFrequency(),
                product.getPremiumAmount(),
                product.getSumInsured(),
                totalPremiumPayable,
                "ACTIVE"
        ));

        List<PremiumInstallment> installments = createSchedule(
                policy,
                request.startDate(),
                product,
                installmentCount,
                intervalMonths
        );
        installmentRepository.saveAll(installments);
        return new PolicyRegistrationResponse(
                policy.getPolicyNumber(),
                "Policy registered successfully.",
                customer.getFullName(),
                customer.getDateOfBirth(),
                customer.getPhone(),
                product.getProductCode(),
                product.getPolicyName(),
                product.getPolicyType(),
                termMonths,
                policy.getStartDate(),
                maturityDate,
                product.getPremiumFrequency(),
                product.getPremiumAmount(),
                product.getSumInsured(),
                totalPremiumPayable,
                installments.stream().map(InstallmentResponse::from).toList()
        );
    }

    private void validateEligibility(Customer customer, InsuranceProduct product) {
        LocalDate today = LocalDate.now();
        if (customer.getDateOfBirth().isAfter(today)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The customer date of birth is invalid."
            );
        }
        int age = Period.between(customer.getDateOfBirth(), today).getYears();
        if (age < product.getMinEntryAge() || age > product.getMaxEntryAge()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The customer does not meet the product age eligibility requirements."
            );
        }
    }

    private int intervalMonths(String frequency) {
        return switch (frequency) {
            case "MONTHLY" -> 1;
            case "QUARTERLY" -> 3;
            case "HALF_YEARLY" -> 6;
            case "YEARLY" -> 12;
            default -> throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The selected product has an unsupported premium frequency."
            );
        };
    }

    private List<PremiumInstallment> createSchedule(
            CustomerPolicy policy,
            LocalDate startDate,
            InsuranceProduct product,
            int installmentCount,
            int intervalMonths
    ) {
        List<PremiumInstallment> installments = new ArrayList<>(installmentCount);
        for (int installmentNumber = 1; installmentNumber <= installmentCount; installmentNumber++) {
            LocalDate dueDate = startDate.plusMonths((long) (installmentNumber - 1) * intervalMonths);
            installments.add(new PremiumInstallment(
                    policy,
                    installmentNumber,
                    dueDate,
                    dueDate.plusMonths(1),
                    product.getPremiumAmount()
            ));
        }
        return installments;
    }
}

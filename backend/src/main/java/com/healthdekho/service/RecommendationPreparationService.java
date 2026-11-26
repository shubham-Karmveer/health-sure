package com.healthdekho.service;

import com.healthdekho.dto.CreateRecommendationRequest;
import com.healthdekho.entity.Customer;
import com.healthdekho.recommendation.RecommendationProvider;
import com.healthdekho.repository.CustomerRepository;
import com.healthdekho.repository.InsuranceProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationPreparationService {

    private final CustomerRepository customerRepository;
    private final InsuranceProductRepository productRepository;

    public RecommendationPreparationService(
            CustomerRepository customerRepository,
            InsuranceProductRepository productRepository
    ) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PreparedRecommendation prepare(UUID customerId, CreateRecommendationRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        LocalDate dateOfBirth = request.dateOfBirth() == null ? customer.getDateOfBirth() : request.dateOfBirth();
        if (!dateOfBirth.equals(customer.getDateOfBirth())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Date of birth must match the authenticated customer's profile."
            );
        }
        LocalDate today = LocalDate.now();
        if (!dateOfBirth.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth must be in the past.");
        }
        int age = Period.between(dateOfBirth, today).getYears();
        List<RecommendationProvider.Candidate> candidates = productRepository
                .findEligibleAvailableForCustomer(customerId, age)
                .stream()
                .map(product -> new RecommendationProvider.Candidate(
                        product.getProductCode(),
                        product.getPolicyName(),
                        product.getPolicyType(),
                        product.getTermMonths(),
                        product.getPremiumFrequency(),
                        product.getPremiumAmount(),
                        product.getSumInsured(),
                        product.getMinEntryAge(),
                        product.getMaxEntryAge()
                ))
                .toList();
        return new PreparedRecommendation(
                dateOfBirth,
                age,
                normalize(request.insuranceGoals()),
                normalize(request.preferredTerm()),
                normalize(request.preferredPolicyType()),
                candidates
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.healthdekho.service;

import com.healthdekho.dto.CustomerResponse;
import com.healthdekho.dto.PolicySummaryResponse;
import com.healthdekho.entity.Customer;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerPolicyRepository customerPolicyRepository;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerPolicyRepository customerPolicyRepository
    ) {
        this.customerRepository = customerRepository;
        this.customerPolicyRepository = customerPolicyRepository;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getProfile(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public List<PolicySummaryResponse> getPolicies(Long customerId) {
        return customerPolicyRepository.findAllByCustomer_IdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(PolicySummaryResponse::from)
                .toList();
    }
}

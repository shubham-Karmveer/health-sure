package com.healthdekho.service;

import com.healthdekho.dto.LoginRequest;
import com.healthdekho.entity.Customer;
import com.healthdekho.repository.CustomerRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Customer authenticate(LoginRequest request) {
        Customer customer = customerRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));
        if (!"ACTIVE".equals(customer.getStatus())
                || !passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        return customer;
    }
}

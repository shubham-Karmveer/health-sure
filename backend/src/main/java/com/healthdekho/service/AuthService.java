package com.healthdekho.service;

import com.healthdekho.dto.LoginRequest;
import com.healthdekho.entity.Customer;
import com.healthdekho.repository.CustomerRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final String demoLoginEmail;
    private final String demoLoginPassword;

    public AuthService(
            CustomerRepository customerRepository,
            @Value("${app.auth.demo.email:}") String demoLoginEmail,
            @Value("${app.auth.demo.password:}") String demoLoginPassword
    ) {
        this.customerRepository = customerRepository;
        this.demoLoginEmail = demoLoginEmail == null ? "" : demoLoginEmail.trim();
        this.demoLoginPassword = demoLoginPassword == null ? "" : demoLoginPassword;
    }

    public Customer authenticate(LoginRequest request) {
        String email = request.email().trim();
        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));
        boolean configuredEmailMatches = !demoLoginEmail.isBlank()
                && email.equalsIgnoreCase(demoLoginEmail);
        boolean configuredPasswordMatches = !demoLoginPassword.isEmpty()
                && MessageDigest.isEqual(
                        demoLoginPassword.getBytes(StandardCharsets.UTF_8),
                        request.password().getBytes(StandardCharsets.UTF_8)
                );
        if (!configuredEmailMatches || !configuredPasswordMatches) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        return customer;
    }
}

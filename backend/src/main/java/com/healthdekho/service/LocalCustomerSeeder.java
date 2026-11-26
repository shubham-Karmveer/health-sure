package com.healthdekho.service;

import com.healthdekho.entity.Customer;
import com.healthdekho.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "app.seed.customer.enabled", havingValue = "true")
public class LocalCustomerSeeder implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public LocalCustomerSeeder(
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.customer.email:}") String email,
            @Value("${app.seed.customer.password:}") String password
    ) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "SEED_CUSTOMER_EMAIL and SEED_CUSTOMER_PASSWORD are required when local customer seeding is enabled."
            );
        }
        if (customerRepository.findByEmailIgnoreCase(email).isEmpty()) {
            customerRepository.save(new Customer(
                    email,
                    passwordEncoder.encode(password),
                    "Local Demo Customer",
                    LocalDate.of(1990, 1, 1),
                    null,
                    "ACTIVE"
            ));
        }
    }
}

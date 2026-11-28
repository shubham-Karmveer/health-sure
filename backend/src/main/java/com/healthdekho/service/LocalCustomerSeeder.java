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
import java.util.List;

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
        List<SampleCustomer> sampleCustomers = List.of(
                new SampleCustomer(
                        "aarav.sharma@example.com",
                        "Aarav Sharma",
                        LocalDate.of(1990, 4, 15),
                        "9876501001"
                ),
                new SampleCustomer(
                        "meera.iyer@example.com",
                        "Meera Iyer",
                        LocalDate.of(1985, 9, 22),
                        "9876501002"
                ),
                new SampleCustomer(
                        "rohan.verma@example.com",
                        "Rohan Verma",
                        LocalDate.of(2000, 1, 10),
                        "9876501003"
                )
        );
        SampleCustomer configuredCustomer = sampleCustomers.stream()
                .filter(customer -> customer.email().equalsIgnoreCase(email))
                .findFirst()
                .orElse(new SampleCustomer(email, "Local Demo Customer", LocalDate.of(1990, 1, 1), "0000000000"));

        seedCustomer(configuredCustomer);
        sampleCustomers.stream()
                .filter(customer -> !customer.email().equalsIgnoreCase(configuredCustomer.email()))
                .forEach(this::seedCustomer);
    }

    private void seedCustomer(SampleCustomer sampleCustomer) {
        if (customerRepository.findByEmailIgnoreCase(sampleCustomer.email()).isEmpty()) {
            customerRepository.save(new Customer(
                    sampleCustomer.email(),
                    passwordEncoder.encode(password),
                    sampleCustomer.fullName(),
                    sampleCustomer.dateOfBirth(),
                    sampleCustomer.phone(),
                    "ACTIVE"
            ));
        }
    }

    private record SampleCustomer(String email, String fullName, LocalDate dateOfBirth, String phone) {
    }
}

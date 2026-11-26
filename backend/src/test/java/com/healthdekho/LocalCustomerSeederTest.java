package com.healthdekho;

import com.healthdekho.entity.Customer;
import com.healthdekho.repository.CustomerRepository;
import com.healthdekho.service.LocalCustomerSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalCustomerSeederTest {

    @Test
    void seedsSourceCustomersAndLocalAccountWithHashedPassword() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LocalCustomerSeeder seeder = new LocalCustomerSeeder(
                customerRepository,
                passwordEncoder,
                "demo@example.com",
                "LocalDemoPassword123!"
        );

        seeder.run(null);

        org.mockito.ArgumentCaptor<Customer> customerCaptor =
                org.mockito.ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, org.mockito.Mockito.times(4)).save(customerCaptor.capture());
        List<Customer> createdCustomers = customerCaptor.getAllValues();
        assertThat(createdCustomers)
                .extracting(Customer::getEmail)
                .containsExactlyInAnyOrder(
                        "demo@example.com",
                        "aarav.sharma@example.com",
                        "meera.iyer@example.com",
                        "rohan.verma@example.com"
                );
        assertThat(createdCustomers).allSatisfy(customer -> {
            assertThat(customer.getPasswordHash()).isNotEqualTo("LocalDemoPassword123!");
            assertThat(passwordEncoder.matches(
                    "LocalDemoPassword123!",
                    customer.getPasswordHash()
            )).isTrue();
        });
    }

    @Test
    void seedsSourceCustomerProfileWhenConfiguredEmailMatches() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LocalCustomerSeeder seeder = new LocalCustomerSeeder(
                customerRepository,
                new BCryptPasswordEncoder(),
                "aarav.sharma@example.com",
                "LocalDemoPassword123!"
        );

        seeder.run(null);

        org.mockito.ArgumentCaptor<Customer> customerCaptor =
                org.mockito.ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, org.mockito.Mockito.times(3)).save(customerCaptor.capture());
        Customer aarav = customerCaptor.getAllValues().stream()
                .filter(customer -> customer.getEmail().equals("aarav.sharma@example.com"))
                .findFirst()
                .orElseThrow();
        assertThat(aarav.getFullName()).isEqualTo("Aarav Sharma");
        assertThat(aarav.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 4, 15));
        assertThat(aarav.getPhone()).isEqualTo("9876501001");
    }

    @Test
    void doesNotReplaceExistingCustomers() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        when(customerRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(mock(Customer.class)));
        LocalCustomerSeeder seeder = new LocalCustomerSeeder(
                customerRepository,
                new BCryptPasswordEncoder(),
                "demo@example.com",
                "LocalDemoPassword123!"
        );

        seeder.run(null);

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void rejectsEnabledSeedingWithoutCredentials() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        LocalCustomerSeeder seeder = new LocalCustomerSeeder(
                customerRepository,
                new BCryptPasswordEncoder(),
                "",
                ""
        );

        assertThatThrownBy(() -> seeder.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SEED_CUSTOMER_EMAIL and SEED_CUSTOMER_PASSWORD are required");
        verify(customerRepository, never()).save(any(Customer.class));
    }
}

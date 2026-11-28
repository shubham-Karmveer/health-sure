package com.healthdekho;

import com.healthdekho.dto.LoginRequest;
import com.healthdekho.entity.Customer;
import com.healthdekho.repository.CustomerRepository;
import com.healthdekho.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void acceptsOnlyConfiguredEmailAndExactConfiguredPassword() {
        CustomerRepository repository = mock(CustomerRepository.class);
        Customer customer = new Customer(
                "demo@example.com",
                null,
                "Demo Employee",
                LocalDate.of(1990, 4, 15),
                null,
                "ACTIVE"
        );
        when(repository.findByEmailIgnoreCase("demo@example.com")).thenReturn(Optional.of(customer));
        AuthService service = new AuthService(repository, "demo@example.com", "StrongDemoPassword!");

        assertThat(service.authenticate(new LoginRequest("demo@example.com", "StrongDemoPassword!")))
                .isSameAs(customer);
        assertThatThrownBy(() -> service.authenticate(
                new LoginRequest("demo@example.com", "another-password")
        )).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsNonAllowlistedAccountsEvenWhenTheSuppliedPasswordMatches() {
        CustomerRepository repository = mock(CustomerRepository.class);
        when(repository.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(new Customer(
                "other@example.com",
                null,
                "Other Customer",
                LocalDate.of(1990, 4, 15),
                null,
                "ACTIVE"
        )));
        AuthService service = new AuthService(repository, "demo@example.com", "StrongDemoPassword!");

        assertThatThrownBy(() -> service.authenticate(
                new LoginRequest("other@example.com", "StrongDemoPassword!")
        )).isInstanceOf(BadCredentialsException.class);
    }
}

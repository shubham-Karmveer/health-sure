package com.healthdekho;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdekho.entity.Customer;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.CustomerRepository;
import com.healthdekho.repository.InsuranceProductRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationIntegrationTest {

    private static final String EMAIL = "customer@example.com";
    private static final String PASSWORD = "LocalTestPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InsuranceProductRepository productRepository;

    @Autowired
    private CustomerPolicyRepository policyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void customerCanSignInAndReadOnlyTheirProfileAndPolicies() throws Exception {
        Customer customer = customerRepository.save(new Customer(
                EMAIL,
                passwordEncoder.encode(PASSWORD),
                "Test Customer",
                LocalDate.of(1990, 4, 15),
                "5550100",
                "ACTIVE"
        ));
        InsuranceProduct product = productRepository.save(new InsuranceProduct(
                "HD-TERM-001",
                "Family Term Cover",
                "TERM",
                240,
                "MONTHLY",
                new BigDecimal("1250.00"),
                new BigDecimal("1000000.00"),
                18,
                60,
                true
        ));
        policyRepository.save(new CustomerPolicy(
                "HD-POL-0001",
                customer,
                product,
                240,
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2046, 1, 30),
                "MONTHLY",
                new BigDecimal("1250.00"),
                new BigDecimal("1000000.00"),
                new BigDecimal("300000.00"),
                "ACTIVE"
        ));

        CsrfFixture csrf = getCsrfFixture();
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginPayload(EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn();

        Cookie authCookie = loginResult.getResponse().getCookie("HD_ACCESS_TOKEN");
        assertThat(authCookie).isNotNull();
        assertThat(authCookie.isHttpOnly()).isTrue();
        assertThat(loginResult.getResponse().getHeader("Set-Cookie")).contains("SameSite=Lax");

        mockMvc.perform(get("/api/customers/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(get("/api/me/policies").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyNumber").value("HD-POL-0001"))
                .andExpect(jsonPath("$[0].policyName").value("Family Term Cover"));
    }

    @Test
    void customerEndpointsRejectRequestsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidCredentialsReturnUnauthorizedWithoutSettingAuthCookie() throws Exception {
        Customer customer = customerRepository.save(new Customer(
                EMAIL,
                passwordEncoder.encode(PASSWORD),
                "Test Customer",
                LocalDate.of(1990, 4, 15),
                null,
                "ACTIVE"
        ));

        CsrfFixture csrf = getCsrfFixture();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginPayload(EMAIL, "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        assertThat(result.getResponse().getCookie("HD_ACCESS_TOKEN")).isNull();
        assertThat(customerRepository.findById(customer.getId())).isPresent();
    }

    private CsrfFixture getCsrfFixture() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        return new CsrfFixture(
                csrfCookie,
                response.get("headerName").asText(),
                response.get("token").asText()
        );
    }

    private record LoginPayload(String email, String password) {
    }

    private record CsrfFixture(Cookie cookie, String headerName, String token) {
    }
}

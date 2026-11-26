package com.healthdekho;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdekho.entity.Customer;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.entity.PremiumInstallment;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.CustomerRepository;
import com.healthdekho.repository.InsuranceProductRepository;
import com.healthdekho.repository.PremiumInstallmentRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PolicyRegistrationIntegrationTest {

    private static final String PASSWORD = "LocalTestPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerPolicyRepository policyRepository;

    @Autowired
    private InsuranceProductRepository productRepository;

    @Autowired
    private PremiumInstallmentRepository installmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void availableProductsAreActiveAndExcludeProductsAlreadyOwnedByCustomer() throws Exception {
        Customer customer = createCustomer("catalog@example.com", LocalDate.of(1990, 4, 15));
        InsuranceProduct ownedProduct = productRepository.findByProductCode("HD-TERM-001").orElseThrow();
        policyRepository.save(new CustomerPolicy(
                "HD-POL-EXISTING",
                customer,
                ownedProduct,
                ownedProduct.getTermMonths(),
                LocalDate.of(2026, 11, 26),
                LocalDate.of(2046, 11, 25),
                ownedProduct.getPremiumFrequency(),
                ownedProduct.getPremiumAmount(),
                ownedProduct.getSumInsured(),
                new BigDecimal("300000.00"),
                "ACTIVE"
        ));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());

        mockMvc.perform(get("/api/products/available").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productCode").value("HD-HEALTH-001"))
                .andExpect(jsonPath("$[1].productCode").value("HD-SAV-001"))
                .andExpect(jsonPath("$[?(@.productCode == 'HD-LEGACY-001')]").isEmpty());
    }

    @Test
    void registrationPersistsPolicyAndCompleteStartAnchoredSchedule() throws Exception {
        Customer customer = createCustomer("register@example.com", LocalDate.of(1990, 4, 15));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());
        LocalDate startDate = LocalDate.of(2030, 1, 31);
        CsrfFixture csrf = getCsrfFixture();

        MvcResult result = mockMvc.perform(post("/api/policies")
                        .cookie(authCookie, csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new RegistrationPayload("HD-TERM-001", startDate)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productCode").value("HD-TERM-001"))
                .andExpect(jsonPath("$.customerName").value("Test Customer"))
                .andExpect(jsonPath("$.startDate").value("2030-01-31"))
                .andExpect(jsonPath("$.maturityDate").value("2050-01-30"))
                .andExpect(jsonPath("$.totalPremiumPayable").value(300000.00))
                .andExpect(jsonPath("$.installments.length()").value(240))
                .andExpect(jsonPath("$.installments[0].dueDate").value("2030-01-31"))
                .andExpect(jsonPath("$.installments[0].gracePeriodEndDate").value("2030-02-28"))
                .andExpect(jsonPath("$.installments[1].dueDate").value("2030-02-28"))
                .andExpect(jsonPath("$.installments[1].gracePeriodEndDate").value("2030-03-28"))
                .andExpect(jsonPath("$.installments[2].dueDate").value("2030-03-31"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        String policyNumber = response.get("policyNumber").asText();
        CustomerPolicy savedPolicy = policyRepository.findByPolicyNumber(policyNumber).orElseThrow();
        List<PremiumInstallment> savedInstallments =
                installmentRepository.findAllByPolicy_IdOrderByInstallmentNumberAsc(savedPolicy.getId());
        assertThat(savedInstallments).hasSize(240);
        assertThat(savedInstallments.get(239).getDueDate()).isEqualTo(LocalDate.of(2049, 12, 31));
        assertThat(savedPolicy.getTotalPremiumPayable()).isEqualByComparingTo("300000.00");
        mockMvc.perform(get("/api/me/policies").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyNumber").value(policyNumber));
    }

    @Test
    void duplicateRegistrationReturnsConflict() throws Exception {
        Customer customer = createCustomer("duplicate@example.com", LocalDate.of(1990, 4, 15));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());
        CsrfFixture csrf = getCsrfFixture();
        RegistrationPayload request = new RegistrationPayload("HD-TERM-001", LocalDate.of(2030, 1, 31));

        mockMvc.perform(post("/api/policies")
                        .cookie(authCookie, csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/policies")
                        .cookie(authCookie, csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("You already have a policy for this product."));
    }

    @Test
    void quarterlyPremiumsUseThreeMonthStartAnchoredIntervals() throws Exception {
        Customer customer = createCustomer("quarterly@example.com", LocalDate.of(1990, 4, 15));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());
        CsrfFixture csrf = getCsrfFixture();

        mockMvc.perform(post("/api/policies")
                        .cookie(authCookie, csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new RegistrationPayload("HD-SAV-001", LocalDate.of(2030, 1, 31))
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.premiumFrequency").value("QUARTERLY"))
                .andExpect(jsonPath("$.totalPremiumPayable").value(300000.00))
                .andExpect(jsonPath("$.installments.length()").value(40))
                .andExpect(jsonPath("$.installments[0].dueDate").value("2030-01-31"))
                .andExpect(jsonPath("$.installments[1].dueDate").value("2030-04-30"))
                .andExpect(jsonPath("$.installments[2].dueDate").value("2030-07-31"));
    }

    @Test
    void registrationRejectsIneligibleCustomerAndPastStartDate() throws Exception {
        Customer underageCustomer = createCustomer("underage@example.com", LocalDate.of(2010, 4, 15));
        Cookie underageCookie = loginAndGetAuthCookie(underageCustomer.getEmail());
        CsrfFixture underageCsrf = getCsrfFixture();
        mockMvc.perform(post("/api/policies")
                        .cookie(underageCookie, underageCsrf.cookie())
                        .header(underageCsrf.headerName(), underageCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new RegistrationPayload("HD-TERM-001", LocalDate.now().plusDays(1))
                        )))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "The customer does not meet the product age eligibility requirements."
                ));

        Customer eligibleCustomer = createCustomer("pastdate@example.com", LocalDate.of(1990, 4, 15));
        Cookie eligibleCookie = loginAndGetAuthCookie(eligibleCustomer.getEmail());
        CsrfFixture eligibleCsrf = getCsrfFixture();
        mockMvc.perform(post("/api/policies")
                        .cookie(eligibleCookie, eligibleCsrf.cookie())
                        .header(eligibleCsrf.headerName(), eligibleCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new RegistrationPayload("HD-TERM-001", LocalDate.now().minusDays(1))
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void productDiscoveryAndRegistrationRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/products/available"))
                .andExpect(status().isUnauthorized());
        CsrfFixture csrf = getCsrfFixture();
        mockMvc.perform(post("/api/policies")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new RegistrationPayload("HD-TERM-001", LocalDate.now().plusDays(1))
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void databaseRejectsProductTermsThatCannotProduceWholeYearlyInstallments() {
        InsuranceProduct invalidProduct = new InsuranceProduct(
                "HD-INVALID-TERM",
                "Invalid Yearly Product",
                "TERM",
                10,
                "YEARLY",
                new BigDecimal("1000.00"),
                new BigDecimal("100000.00"),
                18,
                55,
                true
        );

        assertThatThrownBy(() -> productRepository.saveAndFlush(invalidProduct))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Customer createCustomer(String email, LocalDate dateOfBirth) {
        return customerRepository.save(new Customer(
                email,
                passwordEncoder.encode(PASSWORD),
                "Test Customer",
                dateOfBirth,
                null,
                "ACTIVE"
        ));
    }

    private Cookie loginAndGetAuthCookie(String email) throws Exception {
        CsrfFixture csrf = getCsrfFixture();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginPayload(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("HD_ACCESS_TOKEN");
    }

    private CsrfFixture getCsrfFixture() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return new CsrfFixture(
                result.getResponse().getCookie("XSRF-TOKEN"),
                response.get("headerName").asText(),
                response.get("token").asText()
        );
    }

    private record LoginPayload(String email, String password) {
    }

    private record RegistrationPayload(String productCode, LocalDate startDate) {
    }

    private record CsrfFixture(Cookie cookie, String headerName, String token) {
    }
}

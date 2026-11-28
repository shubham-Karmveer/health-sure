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
import com.healthdekho.repository.PremiumPaymentRepository;
import com.healthdekho.entity.PremiumPayment;
import com.healthdekho.service.JwtTokenService;
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
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PolicySearchIntegrationTest {

    private static final String PASSWORD = "LocalTestPassword123!";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1990, 4, 15);

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
    private PremiumPaymentRepository paymentRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void searchesByPolicyNumberAndDateOfBirthAndReturnsPaymentSummary() throws Exception {
        Customer customer = createCustomer("Test Search Customer", DATE_OF_BIRTH);
        CustomerPolicy policy = createPolicy(customer, new BigDecimal("250.00"));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());

        mockMvc.perform(get("/api/policies/search")
                        .param("policyNumber", policy.getPolicyNumber())
                        .param("dateOfBirth", DATE_OF_BIRTH.toString())
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].policyNumber").value(policy.getPolicyNumber()))
                .andExpect(jsonPath("$.content[0].customerName").value("Test Search Customer"))
                .andExpect(jsonPath("$.content[0].dateOfBirth").value(DATE_OF_BIRTH.toString()))
                .andExpect(jsonPath("$.content[0].policyName").value(policy.getProduct().getPolicyName()))
                .andExpect(jsonPath("$.content[0].policyType").value("HEALTH"))
                .andExpect(jsonPath("$.content[0].startDate").value(policy.getStartDate().toString()))
                .andExpect(jsonPath("$.content[0].maturityDate").value(policy.getMaturityDate().toString()))
                .andExpect(jsonPath("$.content[0].totalPremiumPayable").value(1200.00))
                .andExpect(jsonPath("$.content[0].premiumPaymentStatus").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.content[0].totalPremiumPaid").value(250.00))
                .andExpect(jsonPath("$.content[0].remainingPremium").value(950.00))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/policies/search")
                        .param("policyNumber", policy.getPolicyNumber())
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].policyNumber").value(policy.getPolicyNumber()));
    }

    @Test
    void searchesByCaseInsensitivePartialCustomerNameAndRestrictsResultsToOwner() throws Exception {
        Customer owner = createCustomer("Sample Search Customer", DATE_OF_BIRTH);
        Customer anotherCustomer = createCustomer("Sample Search Customer", DATE_OF_BIRTH);
        CustomerPolicy ownedPolicy = createPolicy(owner, BigDecimal.ZERO);
        CustomerPolicy anotherPolicy = createPolicy(anotherCustomer, BigDecimal.ZERO);
        Cookie authCookie = loginAndGetAuthCookie(owner.getEmail());

        mockMvc.perform(get("/api/policies/search")
                        .param("customerName", "sEaRcH cUsT")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].policyNumber").value(ownedPolicy.getPolicyNumber()))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/policies/search")
                        .param("policyNumber", anotherPolicy.getPolicyNumber())
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void paginatesPolicySearchResults() throws Exception {
        Customer customer = createCustomer("Pagination Search Customer", DATE_OF_BIRTH);
        createPolicy(customer, BigDecimal.ZERO);
        createPolicy(customer, BigDecimal.ZERO);
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());

        mockMvc.perform(get("/api/policies/search")
                        .param("customerName", "pagination")
                        .param("page", "0")
                        .param("size", "1")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/policies/search")
                        .param("customerName", "pagination")
                        .param("page", "1")
                        .param("size", "1")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void validatesCriteriaAndRequiresAuthentication() throws Exception {
        Customer customer = createCustomer("Criteria Search Customer", DATE_OF_BIRTH);
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());

        mockMvc.perform(get("/api/policies/search").cookie(authCookie))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/policies/search")
                        .param("dateOfBirth", DATE_OF_BIRTH.toString())
                        .cookie(authCookie))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/policies/search")
                        .param("customerName", "Criteria")
                        .param("policyNumber", "HD-POL-ANY")
                        .cookie(authCookie))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/policies/search")
                        .param("customerName", "Criteria")
                        .param("dateOfBirth", DATE_OF_BIRTH.toString())
                        .cookie(authCookie))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/policies/search")
                        .param("policyNumber", "HD-POL-ANY")
                        .param("dateOfBirth", LocalDate.now().plusDays(1).toString())
                        .cookie(authCookie))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/policies/search")
                        .param("customerName", "Criteria")
                        .param("size", "51")
                        .cookie(authCookie))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/policies/search").param("policyNumber", "HD-POL-ANY"))
                .andExpect(status().isUnauthorized());
    }

    private Customer createCustomer(String name, LocalDate dateOfBirth) {
        return customerRepository.save(new Customer(
                "policy-search-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode(PASSWORD),
                name,
                dateOfBirth,
                "5550101",
                "ACTIVE"
        ));
    }

    private CustomerPolicy createPolicy(Customer customer, BigDecimal firstInstallmentPayment) {
        String suffix = UUID.randomUUID().toString();
        InsuranceProduct product = productRepository.save(new InsuranceProduct(
                "PS-" + suffix.substring(0, 27),
                "Policy Search Product " + suffix.substring(0, 8),
                "HEALTH",
                12,
                "MONTHLY",
                new BigDecimal("100.00"),
                new BigDecimal("100000.00"),
                18,
                65,
                true
        ));
        LocalDate startDate = LocalDate.now();
        CustomerPolicy policy = policyRepository.save(new CustomerPolicy(
                "HD-PS-" + suffix.replace("-", ""),
                customer,
                product,
                12,
                startDate,
                startDate.plusMonths(12).minusDays(1),
                "MONTHLY",
                new BigDecimal("100.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("1200.00"),
                "ACTIVE"
        ));
        PremiumInstallment firstInstallment = new PremiumInstallment(
                policy, 1, startDate.plusDays(1), startDate.plusMonths(1), new BigDecimal("600.00")
        );
        if (firstInstallmentPayment.signum() > 0) {
            firstInstallment.applyPayment(firstInstallmentPayment, BigDecimal.ZERO);
        }
        PremiumInstallment secondInstallment = new PremiumInstallment(
                policy, 2, startDate.plusMonths(1), startDate.plusMonths(2), new BigDecimal("600.00")
        );
        installmentRepository.saveAll(List.of(firstInstallment, secondInstallment));
        if (firstInstallmentPayment.signum() > 0) {
            paymentRepository.save(new PremiumPayment(
                    firstInstallment,
                    "TEST-RECEIPT-" + suffix.substring(0, 20),
                    "TEST-TXN-" + suffix.substring(0, 20),
                    firstInstallmentPayment,
                    BigDecimal.ZERO,
                    "TEST"
            ));
        }
        return policy;
    }

    private Cookie loginAndGetAuthCookie(String email) throws Exception {
        Customer customer = customerRepository.findByEmailIgnoreCase(email).orElseThrow();
        return new Cookie("HD_ACCESS_TOKEN", jwtTokenService.issueFor(customer));
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

    private record CsrfFixture(Cookie cookie, String headerName, String token) {
    }
}

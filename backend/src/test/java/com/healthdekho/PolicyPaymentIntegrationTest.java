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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PolicyPaymentIntegrationTest {

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
    private PremiumPaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void paymentIsPersistedAndExactRetriesReturnTheOriginalReceipt() throws Exception {
        Customer customer = createCustomer("payments@example.com");
        CustomerPolicy policy = createPolicy(customer, LocalDate.now());
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());
        PaymentPayload request = payment(1, "250.00");

        mockMvc.perform(get("/api/policies/{policyNumber}", policy.getPolicyNumber()).cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingBalance").value(2000.00));
        mockMvc.perform(get("/api/policies/{policyNumber}/installments", policy.getPolicyNumber())
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentStatus").value("PENDING"));

        MvcResult firstResponse = postPayment(authCookie, policy.getPolicyNumber(), "retry-key-1", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPremiumPaid").value(250.00))
                .andExpect(jsonPath("$.remainingPremium").value(1750.00))
                .andExpect(jsonPath("$.payments.length()").value(1))
                .andExpect(jsonPath("$.payments[0].premiumAmount").value(250.00))
                .andExpect(jsonPath("$.payments[0].penaltyAmount").value(0.00))
                .andReturn();
        String transactionReference = objectMapper.readTree(firstResponse.getResponse().getContentAsByteArray())
                .at("/payments/0/transactionReference").asText();

        postPayment(authCookie, policy.getPolicyNumber(), "retry-key-1", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments[0].transactionReference").value(transactionReference))
                .andExpect(jsonPath("$.totalPremiumPaid").value(250.00));

        postPayment(authCookie, policy.getPolicyNumber(), "retry-key-1", payment(1, "300.00"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/policies/{policyNumber}/payments", policy.getPolicyNumber())
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionReference").value(transactionReference))
                .andExpect(jsonPath("$[0].amountPaid").value(250.00));

        List<PremiumInstallment> installments =
                installmentRepository.findAllByPolicy_IdOrderByInstallmentNumberAsc(policy.getId());
        assertThat(installments.get(0).getPaidAmount()).isEqualByComparingTo("250.00");
        assertThat(installments.get(0).getPaymentStatus()).isEqualTo("PARTIALLY_PAID");
        assertThat(paymentRepository.findAllByInstallment_Policy_Id(policy.getId())).hasSize(1);
    }

    @Test
    void latePaymentAssessesPenaltyOnceAndAllocatesCollectedAmountToPenaltyFirst() throws Exception {
        Customer customer = createCustomer("late-payment@example.com");
        CustomerPolicy policy = createPolicy(customer, LocalDate.now().minusMonths(2));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());

        mockMvc.perform(get("/api/policies/{policyNumber}/installments", policy.getPolicyNumber())
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estimatedPenalty").value(100.00))
                .andExpect(jsonPath("$[0].estimatedBalance").value(1100.00));
        postPayment(authCookie, policy.getPolicyNumber(), "late-key-1", payment(1, "200.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPremiumPaid").value(100.00))
                .andExpect(jsonPath("$.totalPenaltyAssessed").value(100.00))
                .andExpect(jsonPath("$.totalPenaltyPaid").value(100.00))
                .andExpect(jsonPath("$.remainingBalance").value(1900.00))
                .andExpect(jsonPath("$.payments[0].amountPaid").value(200.00))
                .andExpect(jsonPath("$.payments[0].premiumAmount").value(100.00))
                .andExpect(jsonPath("$.payments[0].penaltyAmount").value(100.00));

        postPayment(authCookie, policy.getPolicyNumber(), "late-key-2", payment(1, "50.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPremiumPaid").value(150.00))
                .andExpect(jsonPath("$.totalPenaltyAssessed").value(100.00))
                .andExpect(jsonPath("$.totalPenaltyPaid").value(100.00))
                .andExpect(jsonPath("$.payments[0].premiumAmount").value(50.00))
                .andExpect(jsonPath("$.payments[0].penaltyAmount").value(0.00));

        PremiumInstallment installment =
                installmentRepository.findAllByPolicy_IdOrderByInstallmentNumberAsc(policy.getId()).get(0);
        assertThat(installment.getPaidAmount()).isEqualByComparingTo("150.00");
        assertThat(installment.getPenaltyAmount()).isEqualByComparingTo("100.00");
        assertThat(installment.getPaidPenaltyAmount()).isEqualByComparingTo("100.00");
        assertThat(installment.getPaymentStatus()).isEqualTo("OVERDUE");
    }

    @Test
    void onTimeFullPaymentMarksTheInstallmentPaid() throws Exception {
        Customer customer = createCustomer("full-payment@example.com");
        CustomerPolicy policy = createPolicy(customer, LocalDate.now());
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());

        postPayment(authCookie, policy.getPolicyNumber(), "full-key", payment(1, "1000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPremium").value(1000.00))
                .andExpect(jsonPath("$.remainingBalance").value(1000.00));
        mockMvc.perform(get("/api/policies/{policyNumber}/installments", policy.getPolicyNumber())
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentStatus").value("PAID"))
                .andExpect(jsonPath("$[1].paymentStatus").value("PENDING"));
    }

    @Test
    void batchOverpaymentRollsBackAndPolicyAccessIsCustomerScoped() throws Exception {
        Customer owner = createCustomer("owner@example.com");
        Customer otherCustomer = createCustomer("other-owner@example.com");
        CustomerPolicy policy = createPolicy(owner, LocalDate.now());
        CustomerPolicy otherPolicy = createPolicy(otherCustomer, LocalDate.now());
        Cookie authCookie = loginAndGetAuthCookie(owner.getEmail());

        postPayment(
                        authCookie,
                        policy.getPolicyNumber(),
                        "batch-key",
                        new PaymentPayload("UPI", List.of(
                                new PaymentLine(1, new BigDecimal("100.00")),
                                new PaymentLine(2, new BigDecimal("1001.00"))
                        ))
                )
                .andExpect(status().isUnprocessableEntity());

        assertThat(paymentRepository.findAllByInstallment_Policy_Id(policy.getId())).isEmpty();
        assertThat(installmentRepository.findAllByPolicy_IdOrderByInstallmentNumberAsc(policy.getId()))
                .allSatisfy(installment -> assertThat(installment.getPaidAmount()).isEqualByComparingTo("0.00"));
        mockMvc.perform(get("/api/policies/{policyNumber}", otherPolicy.getPolicyNumber()).cookie(authCookie))
                .andExpect(status().isNotFound());
        postPayment(authCookie, otherPolicy.getPolicyNumber(), "unauthorized-key", payment(1, "100.00"))
                .andExpect(status().isNotFound());
        postPayment(
                authCookie,
                policy.getPolicyNumber(),
                "unknown-installment-key",
                payment(3, "100.00")
        ).andExpect(status().isNotFound());
    }

    private Customer createCustomer(String email) {
        return customerRepository.save(new Customer(
                email,
                passwordEncoder.encode(PASSWORD),
                "Test Customer",
                LocalDate.of(1990, 4, 15),
                "5550100",
                "ACTIVE"
        ));
    }

    private CustomerPolicy createPolicy(Customer customer, LocalDate startDate) {
        InsuranceProduct product = productRepository.findByProductCode("HD-TERM-001").orElseThrow();
        CustomerPolicy policy = policyRepository.save(new CustomerPolicy(
                "HD-POL-PAY-" + customer.getEmail().hashCode(),
                customer,
                product,
                12,
                startDate,
                startDate.plusMonths(12).minusDays(1),
                "MONTHLY",
                new BigDecimal("1000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("2000.00"),
                "ACTIVE"
        ));
        installmentRepository.saveAll(List.of(
                new PremiumInstallment(policy, 1, startDate, startDate.plusMonths(1), new BigDecimal("1000.00")),
                new PremiumInstallment(
                        policy,
                        2,
                        startDate.plusMonths(1),
                        startDate.plusMonths(2),
                        new BigDecimal("1000.00")
                )
        ));
        return policy;
    }

    private PaymentPayload payment(int installmentNumber, String amount) {
        return new PaymentPayload(
                "UPI",
                List.of(new PaymentLine(installmentNumber, new BigDecimal(amount)))
        );
    }

    private org.springframework.test.web.servlet.ResultActions postPayment(
            Cookie authCookie,
            String policyNumber,
            String idempotencyKey,
            PaymentPayload request
    ) throws Exception {
        CsrfFixture csrf = getCsrfFixture();
        return mockMvc.perform(post("/api/policies/{policyNumber}/payments", policyNumber)
                .cookie(authCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));
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

    private record PaymentPayload(String paymentMethod, List<PaymentLine> payments) {
    }

    private record PaymentLine(int installmentNumber, BigDecimal amountPaid) {
    }

    private record CsrfFixture(Cookie cookie, String headerName, String token) {
    }
}

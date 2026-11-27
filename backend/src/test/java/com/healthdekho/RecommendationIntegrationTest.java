package com.healthdekho;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdekho.entity.AiRecommendationItem;
import com.healthdekho.entity.AiRecommendationRequest;
import com.healthdekho.entity.Customer;
import com.healthdekho.entity.CustomerPolicy;
import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.repository.AiRecommendationItemRepository;
import com.healthdekho.repository.AiRecommendationRequestRepository;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.CustomerRepository;
import com.healthdekho.repository.InsuranceProductRepository;
import com.healthdekho.recommendation.RecommendationProvider;
import com.healthdekho.recommendation.RecommendationProviderUnavailableException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(RecommendationIntegrationTest.ProviderTestConfiguration.class)
class RecommendationIntegrationTest {

    private static final String PASSWORD = "LocalTestPassword123!";
    private static final RecommendationProvider.Metadata METADATA =
            new RecommendationProvider.Metadata("test-model", "test-version", "test-prompt");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerPolicyRepository policyRepository;

    @Autowired
    private InsuranceProductRepository productRepository;

    @Autowired
    private AiRecommendationRequestRepository recommendationRequestRepository;

    @Autowired
    private AiRecommendationItemRepository recommendationItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecommendationProvider recommendationProvider;

    @TestConfiguration
    static class ProviderTestConfiguration {

        @Bean
        @Primary
        RecommendationProvider recommendationProvider() {
            return Mockito.mock(RecommendationProvider.class);
        }
    }

    @BeforeEach
    void configureProvider() {
        doReturn(METADATA).when(recommendationProvider).metadata();
        doAnswer(invocation -> {
            RecommendationProvider.Context context = invocation.getArgument(0);
            return context.candidates().stream()
                    .limit(1)
                    .map(candidate -> new RecommendationProvider.Suggestion(
                            candidate.productCode(),
                            "Eligible for the customer's profile.",
                            "HIGH"
                    ))
                    .toList();
        }).when(recommendationProvider).recommend(any());
        clearInvocations(recommendationProvider);
    }

    @Test
    void recommendationsAreGroundedPersistedAndVisibleOnlyToTheirOwner() throws Exception {
        Customer customer = createCustomer("recommend@example.com", LocalDate.of(1990, 4, 15));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());
        CsrfFixture csrf = getCsrfFixture();

        MvcResult result = mockMvc.perform(post("/api/recommendations")
                        .cookie(authCookie, csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RecommendationPayload(
                                customer.getDateOfBirth(),
                                "Family protection",
                                "20 years",
                                "TERM"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calculatedAge").value(
                        java.time.Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears()
                ))
                .andExpect(jsonPath("$.modelName").value("test-model"))
                .andExpect(jsonPath("$.recommendations.length()").value(1))
                .andExpect(jsonPath("$.recommendations[0].productCode").value("HD-HEALTH-001"))
                .andExpect(jsonPath("$.recommendations[0].rankOrder").value(1))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        UUID requestId = UUID.fromString(response.get("requestId").asText());
        AiRecommendationRequest savedRequest = recommendationRequestRepository.findById(requestId).orElseThrow();
        List<AiRecommendationItem> savedItems =
                recommendationItemRepository.findAllByRequest_IdOrderByRankOrderAsc(requestId);
        assertThat(savedRequest.getDateOfBirth()).isEqualTo(customer.getDateOfBirth());
        assertThat(savedRequest.getInsuranceGoals()).isEqualTo("Family protection");
        assertThat(savedRequest.getCalculatedAge())
                .isEqualTo(java.time.Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears());
        assertThat(savedItems).extracting(item -> item.getProduct().getProductCode())
                .containsExactly("HD-HEALTH-001");

        mockMvc.perform(get("/api/recommendations/{requestId}", requestId).cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.recommendations.length()").value(1));

        Customer anotherCustomer = createCustomer("other-recommend@example.com", LocalDate.of(1991, 4, 15));
        Cookie otherCookie = loginAndGetAuthCookie(anotherCustomer.getEmail());
        mockMvc.perform(get("/api/recommendations/{requestId}", requestId).cookie(otherCookie))
                .andExpect(status().isNotFound());

        verify(recommendationProvider).recommend(any());
    }

    @Test
    void rejectsAnotherDateOfBirthAndDoesNotCallTheProvider() throws Exception {
        Customer customer = createCustomer("dob-recommend@example.com", LocalDate.of(1990, 4, 15));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());
        CsrfFixture csrf = getCsrfFixture();

        mockMvc.perform(post("/api/recommendations")
                        .cookie(authCookie, csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RecommendationPayload(
                                LocalDate.of(1991, 4, 15),
                                null,
                                null,
                                null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Date of birth must match the authenticated customer's profile."
                ));

        verify(recommendationProvider, never()).recommend(any());
        assertThat(recommendationRequestRepository.countByCustomer_Id(customer.getId())).isZero();
    }

    @Test
    void recordsRequestWithoutCallingProviderWhenNoProductsMeetAgeEligibility() throws Exception {
        Customer customer = createCustomer("age-ineligible@example.com", LocalDate.of(2010, 4, 15));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());
        CsrfFixture csrf = getCsrfFixture();

        mockMvc.perform(post("/api/recommendations")
                        .cookie(authCookie, csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RecommendationPayload(
                                customer.getDateOfBirth(),
                                null,
                                null,
                                null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recommendations.length()").value(0));

        verify(recommendationProvider, never()).recommend(any());
        assertThat(recommendationRequestRepository.countByCustomer_Id(customer.getId())).isEqualTo(1);
    }

    @Test
    void rejectsUnknownModelProductsAndRollsBackRecommendationRequest() throws Exception {
        doReturn(List.of(
                new RecommendationProvider.Suggestion("MODEL-INVENTED-999", "Invented", "HIGH")
        )).when(recommendationProvider).recommend(any());
        Customer customer = createCustomer("untrusted-recommend@example.com", LocalDate.of(1990, 4, 15));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());
        CsrfFixture csrf = getCsrfFixture();

        mockMvc.perform(post("/api/recommendations")
                        .cookie(authCookie, csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RecommendationPayload(
                                customer.getDateOfBirth(),
                                "Protection",
                                null,
                                null
                        ))))
                .andExpect(status().isBadGateway());

        assertThat(recommendationRequestRepository.countByCustomer_Id(customer.getId())).isZero();
        assertThat(recommendationItemRepository.countByRequest_Customer_Id(customer.getId())).isZero();
    }

    @Test
    void providerFailureReturnsServiceUnavailableWithoutSavingSuccessShapedData() throws Exception {
        doThrow(new RecommendationProviderUnavailableException(new IllegalStateException("offline")))
                .when(recommendationProvider).recommend(any());
        Customer customer = createCustomer("failed-recommend@example.com", LocalDate.of(1990, 4, 15));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());
        CsrfFixture csrf = getCsrfFixture();

        mockMvc.perform(post("/api/recommendations")
                        .cookie(authCookie, csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RecommendationPayload(
                                customer.getDateOfBirth(),
                                "Protection",
                                null,
                                null
                        ))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Recommendations are temporarily unavailable."));

        assertThat(recommendationRequestRepository.countByCustomer_Id(customer.getId())).isZero();
    }

    @Test
    void productSearchFiltersAvailableActiveOffersAndPaginates() throws Exception {
        Customer customer = createCustomer("search-products@example.com", LocalDate.of(1990, 4, 15));
        InsuranceProduct ownedProduct = productRepository.findByProductCode("HD-TERM-001").orElseThrow();
        policyRepository.save(new CustomerPolicy(
                "HD-POL-SEARCH",
                customer,
                ownedProduct,
                ownedProduct.getTermMonths(),
                LocalDate.now(),
                LocalDate.now().plusMonths(ownedProduct.getTermMonths()).minusDays(1),
                ownedProduct.getPremiumFrequency(),
                ownedProduct.getPremiumAmount(),
                ownedProduct.getSumInsured(),
                new BigDecimal("300000.00"),
                "ACTIVE"
        ));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());

        mockMvc.perform(get("/api/products/search")
                        .param("policyType", "hea")
                        .param("page", "0")
                        .param("size", "1")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productCode").value("HD-HEALTH-001"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(get("/api/products/search").param("productCode", "HD").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[?(@.productCode == 'HD-TERM-001')]").isEmpty())
                .andExpect(jsonPath("$.content[?(@.productCode == 'HD-LEGACY-001')]").isEmpty());
    }

    @Test
    void productSearchRequiresCriteriaAndAuthentication() throws Exception {
        Customer customer = createCustomer("search-validation@example.com", LocalDate.of(1990, 4, 15));
        Cookie authCookie = loginAndGetAuthCookie(customer.getEmail());

        mockMvc.perform(get("/api/products/search").cookie(authCookie))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products/search").param("productType", "TERM"))
                .andExpect(status().isUnauthorized());
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

    private record RecommendationPayload(
            LocalDate dateOfBirth,
            String insuranceGoals,
            String preferredTerm,
            String preferredPolicyType
    ) {
    }

    private record CsrfFixture(Cookie cookie, String headerName, String token) {
    }
}

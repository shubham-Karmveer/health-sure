package com.healthdekho.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.OutputFormatType;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BedrockRecommendationProviderTest {

    private final BedrockRuntimeClient bedrockRuntimeClient = mock(BedrockRuntimeClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private BedrockRecommendationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new BedrockRecommendationProvider(
                bedrockRuntimeClient,
                objectMapper,
                "us.anthropic.claude-haiku-4-5-20251001-v1:0",
                "configured",
                "recommendations-v1",
                1024,
                0.1f
        );
    }

    @Test
    void requestsSchemaConstrainedOutputAndParsesRecommendations() throws Exception {
        when(bedrockRuntimeClient.converse(any(ConverseRequest.class)))
                .thenReturn(response("""
                        {"recommendations":[{"productCode":"HEALTH-001","recommendationReason":"Fits the requested coverage.","suitability":"HIGH"}]}
                        """, StopReason.END_TURN));

        List<RecommendationProvider.Suggestion> suggestions = provider.recommend(
                new RecommendationProvider.Context(35, "family coverage", "12 months", "health",
                        List.of(new RecommendationProvider.Candidate(
                                "HEALTH-001", "Family Health", "health", 12, "monthly",
                                null, null, 18, 60)))
        );

        assertThat(suggestions).containsExactly(new RecommendationProvider.Suggestion(
                "HEALTH-001", "Fits the requested coverage.", "HIGH"));

        ArgumentCaptor<ConverseRequest> requestCaptor = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrockRuntimeClient).converse(requestCaptor.capture());
        ConverseRequest request = requestCaptor.getValue();
        assertThat(request.modelId()).isEqualTo("us.anthropic.claude-haiku-4-5-20251001-v1:0");
        assertThat(request.inferenceConfig().maxTokens()).isEqualTo(1024);
        assertThat(request.inferenceConfig().temperature()).isEqualTo(0.1f);
        assertThat(request.outputConfig().textFormat().type()).isEqualTo(OutputFormatType.JSON_SCHEMA);

        JsonNode schema = objectMapper.readTree(request.outputConfig().textFormat().structure().jsonSchema().schema());
        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("properties").path("recommendations").path("items")
                .path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("properties").path("recommendations").path("items")
                .path("properties").path("suitability").path("enum"))
                .containsExactly(objectMapper.getNodeFactory().textNode("HIGH"),
                        objectMapper.getNodeFactory().textNode("MODERATE"),
                        objectMapper.getNodeFactory().textNode("LOW"));
    }

    @Test
    void rejectsResponsesTruncatedAtTheTokenLimit() {
        when(bedrockRuntimeClient.converse(any(ConverseRequest.class)))
                .thenReturn(response("{}", StopReason.MAX_TOKENS));

        assertThatThrownBy(() -> provider.recommend(context()))
                .isInstanceOf(InvalidRecommendationProviderResponseException.class)
                .hasMessageContaining("token limit");
    }

    @Test
    void rejectsMalformedJsonEvenWhenTheModelCallCompletesNormally() {
        when(bedrockRuntimeClient.converse(any(ConverseRequest.class)))
                .thenReturn(response("not-json", StopReason.END_TURN));

        assertThatThrownBy(() -> provider.recommend(context()))
                .isInstanceOf(InvalidRecommendationProviderResponseException.class);
    }

    private RecommendationProvider.Context context() {
        return new RecommendationProvider.Context(35, "family coverage", "12 months", "health", List.of());
    }

    private ConverseResponse response(String text, StopReason stopReason) {
        return ConverseResponse.builder()
                .output(ConverseOutput.builder()
                        .message(Message.builder()
                                .role(ConversationRole.ASSISTANT)
                                .content(ContentBlock.fromText(text))
                                .build())
                        .build())
                .stopReason(stopReason)
                .build();
    }
}

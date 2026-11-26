package com.healthdekho.recommendation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "bedrock")
public class BedrockRecommendationConfiguration {

    @Bean(destroyMethod = "close")
    BedrockRuntimeClient recommendationBedrockRuntimeClient(
            @Value("${spring.ai.bedrock.aws.region:us-east-1}") String region
    ) {
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .httpClientBuilder(Apache5HttpClient.builder()
                        .socketTimeout(Duration.ofSeconds(120)))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(Duration.ofSeconds(130))
                        .apiCallTimeout(Duration.ofSeconds(150))
                        .build())
                .build();
    }
}

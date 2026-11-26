package com.healthdekho.controller;

import com.healthdekho.dto.CreateRecommendationRequest;
import com.healthdekho.dto.RecommendationResponse;
import com.healthdekho.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecommendationResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRecommendationRequest request
    ) {
        return recommendationService.create(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping("/{requestId}")
    public RecommendationResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId
    ) {
        return recommendationService.get(UUID.fromString(jwt.getSubject()), requestId);
    }
}

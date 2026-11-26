package com.healthdekho.controller;

import com.healthdekho.dto.CustomerResponse;
import com.healthdekho.dto.PolicySummaryResponse;
import com.healthdekho.service.CustomerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/customers/me")
    public CustomerResponse profile(@AuthenticationPrincipal Jwt jwt) {
        return customerService.getProfile(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/me/policies")
    public List<PolicySummaryResponse> policies(@AuthenticationPrincipal Jwt jwt) {
        return customerService.getPolicies(UUID.fromString(jwt.getSubject()));
    }
}

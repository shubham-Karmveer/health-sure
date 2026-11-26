package com.healthdekho.controller;

import com.healthdekho.dto.CsrfResponse;
import com.healthdekho.dto.LoginRequest;
import com.healthdekho.dto.LoginResponse;
import com.healthdekho.entity.Customer;
import com.healthdekho.security.CookieBearerTokenResolver;
import com.healthdekho.service.AuthService;
import com.healthdekho.service.JwtTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenService jwtTokenService;
    private final boolean secureCookie;

    public AuthController(
            AuthService authService,
            JwtTokenService jwtTokenService,
            @Value("${app.jwt.cookie-secure:false}") boolean secureCookie
    ) {
        this.authService = authService;
        this.jwtTokenService = jwtTokenService;
        this.secureCookie = secureCookie;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        Customer customer = authService.authenticate(request);
        ResponseCookie cookie = ResponseCookie.from(
                        CookieBearerTokenResolver.AUTH_COOKIE_NAME,
                        jwtTokenService.issueFor(customer))
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtTokenService.tokenLifetime())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return new LoginResponse(true);
    }

    @PostMapping("/logout")
    public LoginResponse logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(CookieBearerTokenResolver.AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return new LoginResponse(false);
    }
}

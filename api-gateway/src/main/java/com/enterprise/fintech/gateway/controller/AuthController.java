package com.enterprise.fintech.gateway.controller;

import com.enterprise.fintech.gateway.security.JwtTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenService jwtTokenService;

    public AuthController(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/token")
    public Mono<ResponseEntity<Map<String, Object>>> generateToken(
            @RequestBody(required = false) Map<String, String> request) {
        String username = (request != null && request.containsKey("username"))
                ? request.get("username")
                : "senior-dev-user";
        List<String> roles = List.of("ROLE_USER", "ROLE_SENIOR_DEV", "SCOPE_PAYMENT_WRITE");

        String token = jwtTokenService.generateToken(username, roles);

        return Mono.just(ResponseEntity.ok(Map.of(
                "access_token", token,
                "token_type", "Bearer",
                "expires_in", 86400,
                "user", username,
                "authorities", roles
        )));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> health() {
        return Mono.just(ResponseEntity.ok(Collections.singletonMap("status", "UP")));
    }
}

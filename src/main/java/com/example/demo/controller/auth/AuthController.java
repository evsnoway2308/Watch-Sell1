package com.example.demo.controller.auth;

import com.example.demo.dto.request.auth.LoginRequest;
import com.example.demo.dto.request.auth.SignUpRequest;
import com.example.demo.dto.request.auth.SocialLoginRequest;
import com.example.demo.dto.response.auth.TokenResponse;
import com.example.demo.model.User;
import com.example.demo.service.auth.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@RequestBody @Valid SignUpRequest req) {
        User signupUser = authService.signUp(req);
        return ResponseEntity.ok(signupUser.getId());
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/google")
    public ResponseEntity<TokenResponse> googleLogin(@RequestBody SocialLoginRequest req) {
        TokenResponse tokenResponse = authService.googleLogin(req);
        return ResponseEntity.ok(tokenResponse);
    }
}

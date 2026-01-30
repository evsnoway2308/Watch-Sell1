package com.example.demo.service.auth;

import com.example.demo.model.User;
import com.example.demo.dto.request.auth.SignUpRequest;
import com.example.demo.dto.request.auth.SocialLoginRequest;
import com.example.demo.dto.request.auth.LoginRequest;
import com.example.demo.dto.response.auth.TokenResponse;

public interface AuthService {

    User signUp(SignUpRequest req);
    TokenResponse login(LoginRequest request);
   
   TokenResponse getRefreshToken(String refreshToken);

   TokenResponse googleLogin(SocialLoginRequest req);
}
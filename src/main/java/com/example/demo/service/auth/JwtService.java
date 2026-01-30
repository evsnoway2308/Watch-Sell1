package com.example.demo.service.auth;

import java.util.List;
import com.example.demo.common.TokenType;

public interface JwtService {
    
    String generateAccessToken(String username, List<String> authorities);

    String generateRefreshToken(String username, List<String> authorities);

    String extractUsername(String token, TokenType tokenType);
}
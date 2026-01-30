package com.example.demo.service.auth.impl;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.demo.common.TokenType;
import com.example.demo.exception.AppException;
import com.example.demo.service.auth.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j(topic = "JWT-SERVICE")
public class JwtServiceImpl implements JwtService {
    @Value("${jwt.expiration-time}")
    private long expirationTime;
    @Value("${jwt.expir-days}")
    private long expirDays;
    @Value("${jwt.access-key}")
    private String acessKey;
    @Value("${jwt.refresh-key}")
    private String refreshKey;

    @Override
    public String generateAccessToken( String username,List<String> authorities) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", authorities);

        log.info("Generating access token for userId: {}, username: {}", username);

        return generateToken(claims, username);
    }

    @Override
    public String generateRefreshToken(String username,
           List<String> authorities) {

        log.info("Generating refresh token for userId: {}, username: {}",username);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", authorities);
        return refreshToken(claims, username);
    }

    @Override
    public String extractUsername(String token, TokenType tokenType) {
        log.info("Extracting username from {} token: {}", tokenType, token);
        return extractClaim(token, tokenType, Claims::getSubject);
    }

    private <T> T extractClaim(String token, TokenType tokenType, Function<Claims, T> claimExtractor) {
        final Claims claims = extractAllClaims(token, tokenType);
        return claimExtractor.apply(claims);

    }

    private Claims extractAllClaims(String token, TokenType tokenType) {
        try {
            return Jwts.parserBuilder() 
                    .setSigningKey(getKey(tokenType)) 
                    .build() 
                    .parseClaimsJws(token) 
                    .getBody();
        } catch (SignatureException | ExpiredJwtException e) {
            throw new AppException("Token không hợp lệ");
        }

    }

    private String generateToken(Map<String, Object> claims, String username) {
        log.info("Generating token for username: {}", username);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * expirationTime))
                .signWith(getKey(TokenType.ACCESS_TOKEN), SignatureAlgorithm.HS256)
                .compact();
    }

    private String refreshToken(Map<String, Object> claims, String username) {
        log.info("Generating token for username: {}", username);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24 * expirDays))
                .signWith(getKey(TokenType.REFRESH_TOKEN), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getKey(TokenType tokenType) {
        switch (tokenType) {
            case ACCESS_TOKEN:
                return Keys.hmacShaKeyFor(Decoders.BASE64.decode(acessKey));
            case REFRESH_TOKEN:

                return Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshKey));
            default:
                throw new IllegalArgumentException("Invalid token type");
        }

    }

}

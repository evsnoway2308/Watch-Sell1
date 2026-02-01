package com.example.demo.service.auth.impl;

import com.example.demo.model.User;
import com.example.demo.service.auth.AuthService;
import com.example.demo.service.auth.JwtService;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.client.RestTemplate;

import com.example.demo.repository.UserRepository;
import com.example.demo.model.Role;
import com.example.demo.common.TokenType;
import com.example.demo.dto.request.auth.SignUpRequest;
import com.example.demo.dto.request.auth.SocialLoginRequest;
import com.example.demo.dto.request.auth.LoginRequest;
import com.example.demo.dto.response.auth.TokenResponse;
import com.example.demo.exception.AppException;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.util.UUID;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import com.example.demo.dto.response.auth.GoogleUserInfo;
import com.example.demo.dto.response.auth.GoogleTokenResponse;
import com.example.demo.repository.RoleRepository;




@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${google.client-id}")
    private String googleClientId;
    @Value("${google.client-secret}")
    private String googleClientSecret;
    @Value("${google.redirect-uri}")
    private String googleRedirectUri;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RoleRepository roleRepository;



    @Override
    public User signUp(SignUpRequest req) {

        if (userRepository.existsByUsername(req.getUsername()) ||
            userRepository.existsByEmail(req.getEmail())) {
            throw new AppException("Tên đăng nhập hoặc email đã tồn tại");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setName(req.getName());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhonenumber(req.getPhonenumber());
        Role role = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("USER");
                    return roleRepository.save(newRole);
                });
        user.setRole(role);

        return userRepository.save(user);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        List<String> authorities = new ArrayList<>();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            
            authorities.addAll(authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            throw new AppException("Tên đăng nhập hoặc mật khẩu không đúng: " + e.getMessage());
        }

        String accessToken = jwtService.generateAccessToken(request.getUsername(), authorities);
        String refreshToken = jwtService.generateRefreshToken(request.getUsername(), authorities);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public TokenResponse getRefreshToken(String refreshToken) {
        try {
            String username = jwtService.extractUsername(refreshToken, TokenType.REFRESH_TOKEN);
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new AppException("Người dùng không tồn tại");
            }
            List<String> authorities = new ArrayList<>();
            authorities.add(user.getRole().getName());
            String accessToken = jwtService.generateAccessToken(user.getUsername(), authorities);
            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

        } catch (Exception e) {

            throw new AppException("Invalid refresh token");
        }
    }

    @Override
    @Transactional
    public TokenResponse googleLogin(SocialLoginRequest req) {
        try {
            GoogleUserInfo googleUser = getGoogleUserInfo(req.getCode());
            System.out.println("Google user info retrieved: " + googleUser.getEmail());

            User user = userRepository.findByEmail(googleUser.getEmail()).orElseGet(() -> {
                System.out.println("User not found, creating new user for email: " + googleUser.getEmail());
                
                User newUser = new User();
                newUser.setEmail(googleUser.getEmail());
                newUser.setName(googleUser.getName());
                newUser.setUsername(googleUser.getEmail());
                newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                newUser.setAvatarUrl(googleUser.getPicture());
                
                Role role = roleRepository.findByName("USER").orElseGet(() -> {
                     System.out.println("Role USER not found, creating new role");
                     Role newRole = new Role();
                     newRole.setName("USER");
                     Role savedRole = roleRepository.save(newRole);
                     System.out.println("Role saved with ID: " + savedRole.getId());
                     return savedRole;
                });
                newUser.setRole(role);
                
                System.out.println("Saving new user to database...");
                User savedUser = userRepository.save(newUser);
                System.out.println("User saved successfully with ID: " + savedUser.getId());
                return savedUser;
            });

            System.out.println("User retrieved/created with ID: " + user.getId());
            List<String> authorities = Collections.singletonList(user.getRole().getName());

            String accessToken = jwtService.generateAccessToken(user.getUsername(), authorities);
            String refreshToken = jwtService.generateRefreshToken(user.getUsername(), authorities);

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } catch (AppException e) {
            System.err.println("AppException in googleLogin: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Exception in googleLogin: " + e.getMessage());
            e.printStackTrace();
            throw new AppException("Lỗi không xác định khi đăng nhập Google: " + e.getMessage());
        }
    }

    private GoogleUserInfo getGoogleUserInfo(String code) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<GoogleTokenResponse> tokenResponse = restTemplate.postForEntity(
                    tokenUrl, request, GoogleTokenResponse.class);

            if (tokenResponse.getBody() == null || tokenResponse.getBody().getAccessToken() == null) {
                throw new AppException("Không thể lấy token từ Google");
            }

            String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
            HttpHeaders authHeaders = new HttpHeaders();
            authHeaders.setBearerAuth(tokenResponse.getBody().getAccessToken());

            HttpEntity<String> entity = new HttpEntity<>(authHeaders);

            ResponseEntity<GoogleUserInfo> userInfoResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity,
                    GoogleUserInfo.class);

            if (userInfoResponse.getBody() == null) {
                throw new AppException("Không thể lấy thông tin người dùng từ Google");
            }

            return userInfoResponse.getBody();

        } catch (Exception e) {
            throw new AppException("Lỗi đăng nhập Google: " + e.getMessage());
        }
    }
    }


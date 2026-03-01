package com.example.demo.service.common.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.request.ChangePasswordRequest;
import com.example.demo.dto.request.ProfileUpdateRequest;

import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.common.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ProfileResponse getProfile(String name) {
        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ProfileResponse.builder()
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phonenumber(user.getPhonenumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .build();
    }

    @Override
    @Transactional
    public void updateProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhonenumber(request.getPhonenumber());
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Old password does not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public Page<UserResponse> getAllUsers(int page, int size, String keyword, String status, Boolean isShopOwner) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isEmpty()) {
                String search = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), search),
                        cb.like(cb.lower(root.get("name")), search),
                        cb.like(cb.lower(root.get("email")), search)));
            }

            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (isShopOwner != null) {
                if (isShopOwner) {
                    predicates.add(cb.equal(root.get("role").get("name"), "SHOP_OWNER"));
                } else {
                    predicates.add(cb.notEqual(root.get("role").get("name"), "SHOP_OWNER"));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(this::mapToUserResponse);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(status);
        userRepository.save(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .phonenumber(user.getPhonenumber())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .status(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}

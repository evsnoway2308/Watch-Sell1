package com.example.demo.service.common;

import com.example.demo.dto.response.ProfileResponse;

public interface UserService {
    ProfileResponse getProfile(String name);

    void updateProfile(String username, com.example.demo.dto.request.ProfileUpdateRequest request);

    void changePassword(String username, com.example.demo.dto.request.ChangePasswordRequest request);

    org.springframework.data.domain.Page<com.example.demo.dto.response.UserResponse> getAllUsers(int page, int size,
            String keyword, String status, Boolean isShopOwner);

    void updateUserStatus(Long userId, String status);
}

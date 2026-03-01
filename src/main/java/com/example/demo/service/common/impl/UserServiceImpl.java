package com.example.demo.service.common.impl;

import org.springframework.stereotype.Service;

import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.common.UserService;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    @Override
    public ProfileResponse getProfile(String name) {
        User user = userRepository.findByUsername(name);
        return ProfileResponse.builder()
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .build();

    }
    
}

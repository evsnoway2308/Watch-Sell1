package com.example.demo.service.auth.impl;

import com.example.demo.dto.request.SignUpRequest;
import com.example.demo.model.User;
import com.example.demo.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.demo.repository.UserRepository;
import com.example.demo.model.Role;
import com.example.demo.exception.AppException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    @Override
    public User signUp(SignUpRequest req) {

        if (userRepository.existsByUsername(req.getUsername()) ||
            userRepository.existsByEmail(req.getEmail())) {
            throw new AppException("Tên đăng nhập hoặc email đã tồn tại", 400);
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setName(req.getName());
        user.setPassword(req.getPassword());
        user.setPhonenumber(req.getPhonenumber());
        user.setRole(Role.USER);

        return userRepository.save(user);
    }
}

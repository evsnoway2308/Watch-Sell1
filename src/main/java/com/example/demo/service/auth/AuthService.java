package com.example.demo.service.auth;

import com.example.demo.dto.request.SignUpRequest;
import com.example.demo.model.User;

public interface AuthService {

    User signUp(SignUpRequest req);

}
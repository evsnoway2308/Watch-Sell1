package com.example.demo.service.common;
import com.example.demo.dto.response.ProfileResponse;

public interface UserService {
    ProfileResponse getProfile(String name);

}

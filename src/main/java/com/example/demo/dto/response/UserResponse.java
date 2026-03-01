package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String phonenumber;
    private String role;
    private String status;
    private String avatarUrl;
}

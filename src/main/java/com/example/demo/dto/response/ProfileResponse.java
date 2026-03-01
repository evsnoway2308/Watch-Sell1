package com.example.demo.dto.response;
import lombok.Builder;
import lombok.Getter;
@Getter
@Builder

public class ProfileResponse {
    private String name;
    private String avatarUrl;
}

package com.example.demo.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    private String name;
    private String email;
    private String phonenumber;
    private String avatarUrl;
}

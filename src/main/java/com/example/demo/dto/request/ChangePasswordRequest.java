package com.example.demo.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;
}

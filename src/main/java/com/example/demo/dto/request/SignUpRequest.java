package com.example.demo.dto.request;
import com.example.demo.validator.PasswordMatches;
import lombok.Getter;
import lombok.Setter;

@PasswordMatches
@Setter
@Getter

public class SignUpRequest {
    private String username;
    private String name;
    private String email;
    private String password;
    private String confirmPassword;
    private String phonenumber;
}

package com.example.demo.dto.request.auth;
import com.example.demo.validator.PasswordMatches;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;


@PasswordMatches
@Setter
@Getter

public class SignUpRequest {
@NotBlank(message = "Tên đăng nhập là bắt buộc")
    @Size(min = 3, max = 30)
    private String username;

    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    private String phonenumber;

    @NotBlank(message = "Họ và tên là bắt buộc")
    @Size(min = 3, max = 50)
    private String name;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 8, max = 100)
    private String password;
    
    @NotBlank(message = "Xác nhận mật khẩu là bắt buộc")
    @Size(min = 8, max = 100)
    private String confirmPassword;
}

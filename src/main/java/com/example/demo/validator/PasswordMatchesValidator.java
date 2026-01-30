package com.example.demo.validator;

import com.example.demo.dto.request.SignUpRequest;

public class PasswordMatchesValidator implements jakarta.validation.ConstraintValidator<PasswordMatches, SignUpRequest> {
 @Override
 public boolean isValid(SignUpRequest signUpRequest, jakarta.validation.ConstraintValidatorContext context) {
     if (signUpRequest == null || signUpRequest.getPassword() == null || signUpRequest.getConfirmPassword() == null) {
        return false;
     }
     boolean matches = signUpRequest.getPassword().equals(signUpRequest.getConfirmPassword());
        if (!matches) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Mật khẩu không trùng khớp")
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }
        return matches;
 }
    
}

package com.example.demo.controller.user;

import com.example.demo.dto.request.ChangePasswordRequest;
import com.example.demo.dto.request.ProfileUpdateRequest;
import com.example.demo.service.common.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final UserService userService;

    @PutMapping("/update")
    public ResponseEntity<String> updateProfile(@RequestBody ProfileUpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.updateProfile(username, request);
        return ResponseEntity.ok("Profile updated successfully");
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.changePassword(username, request);
        return ResponseEntity.ok("Password changed successfully");
    }
}

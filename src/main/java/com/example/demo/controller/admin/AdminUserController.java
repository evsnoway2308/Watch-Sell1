package com.example.demo.controller.admin;

import com.example.demo.dto.response.UserResponse;
import com.example.demo.service.common.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isShopOwner) {
        return ResponseEntity.ok(userService.getAllUsers(page, size, keyword, status, isShopOwner));
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam String status) {
        userService.updateUserStatus(userId, status);
        return ResponseEntity.ok("User status updated successfully");
    }
}

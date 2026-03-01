package com.example.demo.controller;

import com.example.demo.dto.request.OrderRequest;
import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> createOrder(Authentication authentication, @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(authentication.getName(), request);
        return ResponseEntity.ok(order);
    }
}

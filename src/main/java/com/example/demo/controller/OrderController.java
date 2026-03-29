package com.example.demo.controller;

import com.example.demo.dto.request.OrderRequest;
import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    /**
     * Tạo đơn hàng (COD hoặc BANK_TRANSFER).
     * Với BANK_TRANSFER: trả về order có qrCodeUrl và paymentRef.
     * SePay polling service sẽ tự động cập nhật status=PAID khi nhận thanh toán.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> createOrder(Authentication authentication,
                                             @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(authentication.getName(), request);
        return ResponseEntity.ok(order);
    }

    /**
     * Lấy đơn hàng theo ID - dùng để SepayPaymentComponent polling trạng thái thanh toán.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getMyOrders(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id,
                                                    @RequestBody String status) {
        String cleanStatus = status.replace("\"", "");
        return ResponseEntity.ok(orderService.updateOrderStatus(id, cleanStatus));
    }
}

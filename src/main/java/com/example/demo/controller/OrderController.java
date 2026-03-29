package com.example.demo.controller;

import com.example.demo.dto.request.OrderRequest;
import com.example.demo.dto.response.PaymentSessionResponse;
import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import com.example.demo.service.QrPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final QrPaymentService qrPaymentService;
    private final ObjectMapper objectMapper;

    /**
     * Create a COD order directly.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> createOrder(Authentication authentication, @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(authentication.getName(), request);
        return ResponseEntity.ok(order);
    }

    /**
     * Initiate a QR payment session (does NOT create order yet).
     * Frontend calls this to get the QR code, then polls /check-payment/{ref}.
     */
    @PostMapping("/initiate-qr-payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentSessionResponse> initiateQrPayment(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {

        OrderRequest orderRequest = objectMapper.convertValue(body.get("orderRequest"), OrderRequest.class);
        double totalAmount = Double.parseDouble(body.get("totalAmount").toString());

        PaymentSessionResponse response = qrPaymentService.initiatePayment(
                authentication.getName(), orderRequest, totalAmount);
        return ResponseEntity.ok(response);
    }


    /**
     * Poll payment status by paymentRef.
     * Returns status: PENDING or PAID.
     */
    @GetMapping("/check-payment/{paymentRef}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentSessionResponse> checkPayment(@PathVariable String paymentRef) {
        return ResponseEntity.ok(qrPaymentService.checkPaymentStatus(paymentRef));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<Order>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getMyOrders(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody String status) {
        String cleanStatus = status.replace("\"", "");
        return ResponseEntity.ok(orderService.updateOrderStatus(id, cleanStatus));
    }
}

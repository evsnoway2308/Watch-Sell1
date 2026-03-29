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

import java.util.List;
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
     * Tạo đơn COD trực tiếp.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> createOrder(Authentication authentication,
                                             @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(authentication.getName(), request);
        return ResponseEntity.ok(order);
    }

    /**
     * Khởi tạo phiên thanh toán QR.
     * KHÔNG tạo đơn hàng - chỉ tạo PaymentSession với QR code.
     * Frontend sẽ hiển thị QR và poll /check-payment/{ref}.
     * Khi SePay xác nhận → đơn hàng mới được tạo + kho bị trừ.
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
     * Poll trạng thái thanh toán QR.
     * Frontend gọi mỗi 5 giây.
     * Khi status = PAID → frontend hiển thị thành công.
     */
    @GetMapping("/check-payment/{paymentRef}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentSessionResponse> checkPayment(@PathVariable String paymentRef) {
        return ResponseEntity.ok(qrPaymentService.checkPaymentStatus(paymentRef));
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

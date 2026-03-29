package com.example.demo.service;

import com.example.demo.dto.request.OrderRequest;
import com.example.demo.model.Order;

import java.util.List;

public interface OrderService {

    /**
     * Tạo đơn hàng COD ngay (không cần thanh toán trước).
     */
    Order createOrder(String username, OrderRequest request);

    /**
     * Tạo đơn hàng sau khi thanh toán QR đã được xác nhận.
     * Gọi bởi SePayPollingService khi phát hiện giao dịch hợp lệ.
     * Lúc này mới trừ kho và lưu đơn hàng.
     */
    Order createOrderAfterPayment(String username, OrderRequest request, String paymentRef);

    Order getOrderById(Long orderId);

    List<Order> getMyOrders(String username);

    List<Order> getAllOrders();

    Order updateOrderStatus(Long orderId, String status);
}

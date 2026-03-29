package com.example.demo.service;

import com.example.demo.dto.request.OrderRequest;
import com.example.demo.model.Order;

import java.util.List;

public interface OrderService {
    Order createOrder(String username, OrderRequest request);

    Order getOrderById(Long orderId);

    List<Order> getMyOrders(String username);

    List<Order> getAllOrders();

    Order updateOrderStatus(Long orderId, String status);
}

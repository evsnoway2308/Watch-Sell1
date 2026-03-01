package com.example.demo.service;

import com.example.demo.dto.request.OrderRequest;
import com.example.demo.model.Order;

public interface OrderService {
    Order createOrder(String username, OrderRequest request);
}

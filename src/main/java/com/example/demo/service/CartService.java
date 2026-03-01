package com.example.demo.service;

import com.example.demo.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart();

    void addToCart(Long productId, Integer quantity);

    void updateQuantity(Long productId, Integer quantity);

    void removeFromCart(Long productId);

    void clearCart();
}

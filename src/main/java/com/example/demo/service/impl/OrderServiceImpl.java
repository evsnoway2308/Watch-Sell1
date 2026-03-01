package com.example.demo.service.impl;

import com.example.demo.dto.request.OrderRequest;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Order createOrder(String username, OrderRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus("PENDING");

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Not enough stock for product: " + product.getName());
            }

            // Deduct stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());

            orderItems.add(orderItem);
            totalAmount += product.getPrice() * cartItem.getQuantity();
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        // Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        return savedOrder;
    }
}

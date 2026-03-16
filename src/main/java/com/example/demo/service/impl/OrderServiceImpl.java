package com.example.demo.service.impl;

import com.example.demo.dto.request.OrderRequest;
import com.example.demo.dto.request.OrderItemRequest;
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

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setShippingAddress(request.getShippingAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setNotes(request.getNotes());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("PENDING");

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0;

        // Check if items are provided in the request (Direct Checkout / Buy Now)
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (OrderItemRequest itemReq : request.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

                if (product.getStock() < itemReq.getQuantity()) {
                    throw new RuntimeException("Not enough stock for product: " + product.getName());
                }

                // Deduct stock
                product.setStock(product.getStock() - itemReq.getQuantity());
                productRepository.save(product);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(itemReq.getQuantity());
                orderItem.setPrice(product.getPrice());

                orderItems.add(orderItem);
                totalAmount += product.getPrice() * itemReq.getQuantity();
            }
        } else {
            // Fallback to cart-based order
            Cart cart = cartRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Cart not found"));

            if (cart.getItems().isEmpty()) {
                throw new RuntimeException("Cart is empty");
            }

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

            // Clear cart ONLY for cart-based orders
            cart.getItems().clear();
            cartRepository.save(cart);
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getMyOrders(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        order.setStatus(status);
        return orderRepository.save(order);
    }
}

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

    /**
     * Tạo đơn hàng COD ngay.
     * Chỉ dùng cho COD - không dùng cho BANK_TRANSFER.
     */
    @Override
    @Transactional
    public Order createOrder(String username, OrderRequest request) {
        Order order = buildAndSaveOrder(username, request, "PENDING", null);
        return order;
    }

    /**
     * Tạo đơn hàng SAU KHI thanh toán QR đã được xác nhận.
     * Gọi bởi SePayPollingService khi SePay xác nhận giao dịch.
     * Lúc này mới trừ kho và lưu đơn hàng vào DB.
     */
    @Override
    @Transactional
    public Order createOrderAfterPayment(String username, OrderRequest request, String paymentRef) {
        return buildAndSaveOrder(username, request, "PAID", paymentRef);
    }

    /**
     * Logic dùng chung: tạo order, trừ kho, xóa giỏ hàng (nếu cart-based).
     */
    private Order buildAndSaveOrder(String username, OrderRequest request, String status, String paymentRef) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setShippingAddress(request.getShippingAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setNotes(request.getNotes());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(status);
        order.setPaymentRef(paymentRef);

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0;

        // Luồng "Buy Now" - có items cụ thể
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (OrderItemRequest itemReq : request.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

                if (product.getStock() < itemReq.getQuantity()) {
                    throw new RuntimeException("Không đủ hàng trong kho: " + product.getName());
                }

                // Trừ kho
                product.setStock(product.getStock() - itemReq.getQuantity());
                productRepository.save(product);

                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setProduct(product);
                item.setQuantity(itemReq.getQuantity());
                item.setPrice(product.getPrice());
                orderItems.add(item);
                totalAmount += product.getPrice() * itemReq.getQuantity();
            }
        } else {
            // Luồng từ giỏ hàng
            Cart cart = cartRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Cart not found"));

            if (cart.getItems().isEmpty()) {
                throw new RuntimeException("Cart is empty");
            }

            for (CartItem cartItem : cart.getItems()) {
                Product product = cartItem.getProduct();

                if (product.getStock() < cartItem.getQuantity()) {
                    throw new RuntimeException("Không đủ hàng trong kho: " + product.getName());
                }

                // Trừ kho
                product.setStock(product.getStock() - cartItem.getQuantity());
                productRepository.save(product);

                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setProduct(product);
                item.setQuantity(cartItem.getQuantity());
                item.setPrice(product.getPrice());
                orderItems.add(item);
                totalAmount += product.getPrice() * cartItem.getQuantity();
            }

            // Xóa giỏ hàng sau khi đặt
            cart.getItems().clear();
            cartRepository.save(cart);
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
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
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Hoàn kho khi hủy đơn
        if ("CANCELLED".equalsIgnoreCase(status) && !"CANCELLED".equalsIgnoreCase(order.getStatus())) {
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    Product product = item.getProduct();
                    if (product != null && item.getQuantity() != null) {
                        product.setStock(product.getStock() + item.getQuantity());
                        productRepository.save(product);
                    }
                }
            }
        }

        order.setStatus(status);
        return orderRepository.save(order);
    }
}

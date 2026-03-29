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
        // This method handles COD orders only.
        // BANK_TRANSFER/QR orders go through QrPaymentService.initiatePayment() + createOrderAfterPayment()
        if ("BANK_TRANSFER".equalsIgnoreCase(request.getPaymentMethod())
                || "SEPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            throw new RuntimeException("Use QR payment flow for BANK_TRANSFER orders.");
        }
        Order order = buildOrder(username, request);
        order.setStatus("PENDING");
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order createOrderAfterPayment(String username, OrderRequest request, String paymentRef) {
        // Reuse the same order creation logic
        Order order = buildOrder(username, request);
        order.setPaymentRef(paymentRef);
        order.setStatus("PAID"); // Payment is already confirmed

        // Generate QR URL (for record keeping)
        String qrUrl = String.format(
                "https://img.vietqr.io/image/BIDV-96247111204-compact2.png?amount=%d&addInfo=%s&accountName=NGUYEN%%20DUC%%20KHANH",
                order.getTotalAmount().intValue(), paymentRef);
        order.setQrCodeUrl(qrUrl);

        return orderRepository.save(order);
    }

    /**
     * Builds an Order object from request without saving - shared logic.
     */
    private Order buildOrder(String username, OrderRequest request) {
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

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (OrderItemRequest itemReq : request.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

                if (product.getStock() < itemReq.getQuantity()) {
                    throw new RuntimeException("Not enough stock for product: " + product.getName());
                }

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

            // Clear cart
            cart.getItems().clear();
            cartRepository.save(cart);
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        return order;
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

        // Restore inventory stock when transitioning from a non-cancelled state to CANCELLED
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

package com.example.demo.service.admin.impl;

import com.example.demo.dto.response.DashboardStatsResponse;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.model.Order;
import com.example.demo.model.Product;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.admin.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardStatsResponse getStats() {
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.countTotalOrders();
        long totalUsers = userRepository.count();
        Double totalRevenue = orderRepository.getTotalRevenue();
        if (totalRevenue == null)
            totalRevenue = 0.0;

        List<Product> recentProducts = productRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"))).getContent();

        List<Order> recentOrders = orderRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "orderDate"))).getContent();

        return DashboardStatsResponse.builder()
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalUsers(totalUsers)
                .totalRevenue(totalRevenue)
                .recentProducts(recentProducts.stream().map(this::mapToProductResponse).collect(Collectors.toList()))
                .recentOrders(recentOrders.stream().map(this::mapToOrderResponse).collect(Collectors.toList()))
                .build();
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .stock(product.getStock())
                .build();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .customerName(order.getUser() != null ? order.getUser().getUsername() : "Guest")
                .build();
    }
}

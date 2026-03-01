package com.example.demo.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalProducts;
    private long totalOrders;
    private long totalUsers;
    private Double totalRevenue;
    private List<ProductResponse> recentProducts;
    private List<OrderResponse> recentOrders;
}

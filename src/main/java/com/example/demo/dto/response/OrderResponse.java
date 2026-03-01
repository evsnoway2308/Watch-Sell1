package com.example.demo.dto.response;

import lombok.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Date orderDate;
    private Double totalAmount;
    private String status;
    private String customerName;
}

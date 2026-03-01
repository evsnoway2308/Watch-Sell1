package com.example.demo.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    private Long id;
    private List<CartItemResponse> items;
    private Double totalAmount;
}

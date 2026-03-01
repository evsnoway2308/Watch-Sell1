package com.example.demo.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Double productPrice;
    private String productImageUrl;
    private Integer quantity;
    private Double subTotal;
}

package com.example.demo.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private Double price;
    private String imageUrl;
    private String categoryName;
    private Integer stock;
    private Boolean available;
    private Double averageRating;
    private Integer reviewCount;
}

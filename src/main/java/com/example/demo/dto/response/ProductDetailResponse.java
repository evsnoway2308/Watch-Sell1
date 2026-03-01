package com.example.demo.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailResponse {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private String imageUrl;
    private String categoryName;
    private Integer stock;
    private Boolean available;
    private Double averageRating;
    private Integer reviewCount;
    private List<String> images;
}

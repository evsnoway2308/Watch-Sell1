package com.example.demo.dto.request;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {
    private String name;
    private String description;
    private Double price;
    private String imageUrl;
    private Long categoryId;
    private Integer stock;
    private List<String> images;
}

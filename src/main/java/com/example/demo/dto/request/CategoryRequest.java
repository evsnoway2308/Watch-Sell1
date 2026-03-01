package com.example.demo.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    private String name;
    private String description;
}

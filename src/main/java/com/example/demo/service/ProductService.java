package com.example.demo.service;

import com.example.demo.dto.response.ProductDetailResponse;
import com.example.demo.dto.response.ProductResponse;
import org.springframework.data.domain.Page;

public interface ProductService {
    Page<ProductResponse> getAllProducts(int page, int size);

    ProductDetailResponse getProductById(Long id);
}

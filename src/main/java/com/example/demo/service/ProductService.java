package com.example.demo.service;

import com.example.demo.dto.request.ProductRequest;
import com.example.demo.dto.response.ProductDetailResponse;
import com.example.demo.dto.response.ProductResponse;
import org.springframework.data.domain.Page;

public interface ProductService {
    Page<ProductResponse> getAllProducts(int page, int size, Long categoryId);

    ProductDetailResponse getProductById(Long id);

    void addProduct(ProductRequest request);

    void updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);
}

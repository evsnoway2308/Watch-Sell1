package com.example.demo.service.impl;

import java.util.List;
import com.example.demo.dto.request.ProductRequest;
import com.example.demo.dto.response.ProductDetailResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.model.Category;
import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;
import com.example.demo.model.Review;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void addProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setStock(request.getStock());
        product.setCategory(category);
        product.setAvailable(true);
        product.setIsDeleted(false);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<ProductImage> productImages = request.getImages().stream()
                    .map(url -> {
                        ProductImage pi = new ProductImage();
                        pi.setImageUrl(url);
                        pi.setProduct(product);
                        return pi;
                    }).collect(Collectors.toList());
            product.setImages(productImages);
        }

        productRepository.save(product);
    }

    @Override
    public Page<ProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findAllByIsDeletedIsFalse(pageable);
        return productPage.map(this::mapToProductResponse);
    }

    @Override
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToProductDetailResponse(product);
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .stock(product.getStock())
                .available(product.getAvailable())
                .averageRating(calculateAverageRating(product))
                .reviewCount(product.getReviews() != null ? product.getReviews().size() : 0)
                .build();
    }

    private ProductDetailResponse mapToProductDetailResponse(Product product) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .stock(product.getStock())
                .available(product.getAvailable())
                .averageRating(calculateAverageRating(product))
                .reviewCount(product.getReviews() != null ? product.getReviews().size() : 0)
                .images(product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .collect(Collectors.toList()))
                .build();
    }

    private double calculateAverageRating(Product product) {
        if (product.getReviews() == null || product.getReviews().isEmpty()) {
            return 0.0;
        }
        double avg = product.getReviews().stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        return Math.round(avg * 10.0) / 10.0;
    }
}

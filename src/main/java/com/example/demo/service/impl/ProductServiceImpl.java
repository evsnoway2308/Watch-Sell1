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
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.OrderItemRepository;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;

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
    public Page<ProductResponse> getAllProducts(int page, int size, Long categoryId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage;
        if (categoryId != null) {
            productPage = productRepository.findByCategoryIdAndIsDeletedIsFalse(categoryId, pageable);
        } else {
            productPage = productRepository.findAllByIsDeletedIsFalse(pageable);
        }
        return productPage.map(this::mapToProductResponse);
    }

    @Override
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToProductDetailResponse(product);
    }

    @Override
    @Transactional
    public void updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setStock(request.getStock());
        product.setCategory(category);

        if (request.getImages() != null) {
            // Simple approach: replace all images
            product.getImages().clear();
            List<ProductImage> productImages = request.getImages().stream()
                    .map(url -> {
                        ProductImage pi = new ProductImage();
                        pi.setImageUrl(url);
                        pi.setProduct(product);
                        return pi;
                    }).collect(Collectors.toList());
            product.getImages().addAll(productImages);
        }

        productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Physical delete requested for product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Check if product has orders
        if (orderItemRepository.existsByProductId(id)) {
            log.warn(
                    "Cannot physically delete product with id: {} as it has associated orders. Falling back to soft delete.",
                    id);
            product.setIsDeleted(true);
            product.setAvailable(false);
            productRepository.save(product);
            return;
        }

        // Delete from cart items first (constraint)
        log.info("Deleting cart items for product id: {}", id);
        cartItemRepository.deleteByProductId(id);

        // Physical delete (images and reviews are cascaded)
        log.info("Physically deleting product with id: {}", id);
        productRepository.delete(product);
        log.info("Product with id: {} successfully deleted from database", id);
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

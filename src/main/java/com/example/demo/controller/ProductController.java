package com.example.demo.controller;

import com.example.demo.dto.request.ProductRequest;
import com.example.demo.dto.response.ProductDetailResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j(topic = "PRODUCT-CONTROLLER")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> addProduct(@RequestBody ProductRequest request) {
        productService.addProduct(request);
        return ResponseEntity.ok("Product added successfully");
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(productService.getAllProducts(page, size, categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        productService.updateProduct(id, request);
        return ResponseEntity.ok("Product updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("DELETE request for product id: {} from user: {}", id, auth != null ? auth.getName() : "Anonymous");
        try {
            log.info("Calling productService.deleteProduct for id: {}", id);
            productService.deleteProduct(id);
            log.info("Successfully finished productService.deleteProduct for id: {}", id);
            return ResponseEntity.ok("Product deleted successfully");
        } catch (Exception e) {
            log.error("Error occurred while deleting product id: {}. Error: {}", id, e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}

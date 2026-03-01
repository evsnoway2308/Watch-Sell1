package com.example.demo.service;

import com.example.demo.dto.request.CategoryRequest;
import com.example.demo.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);

    void addCategory(CategoryRequest request);

    void updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}

package com.example.demo.repository;

import com.example.demo.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false OR p.isDeleted IS NULL")
    Page<Product> findAllByIsDeletedIsFalse(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    Page<Product> findByCategoryIdAndIsDeletedIsFalse(@Param("categoryId") Long categoryId, Pageable pageable);
}

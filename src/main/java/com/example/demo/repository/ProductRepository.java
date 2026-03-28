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

    @Query("SELECT p FROM Product p WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    Page<Product> findByKeywordAndIsDeletedIsFalse(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    Page<Product> findByCategoryIdAndKeywordAndIsDeletedIsFalse(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);
}

package com.example.demo.repository;

import com.example.demo.model.Cart;
import com.example.demo.model.CartItem;
import com.example.demo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.product.id = :productId")
    void deleteByProductId(Long productId);
}

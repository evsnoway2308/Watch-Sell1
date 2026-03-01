package com.example.demo.repository;

import com.example.demo.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'DELIVERED' OR o.status = 'PAID'")
    Double getTotalRevenue();

    @Query("SELECT COUNT(o) FROM Order o")
    long countTotalOrders();
}

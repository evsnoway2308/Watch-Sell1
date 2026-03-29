package com.example.demo.repository;

import com.example.demo.model.PaymentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PaymentSessionRepository extends JpaRepository<PaymentSession, Long> {

    Optional<PaymentSession> findByPaymentRef(String paymentRef);

    List<PaymentSession> findByStatus(String status);
}

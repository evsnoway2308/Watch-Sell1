package com.example.demo.service;

import com.example.demo.dto.request.OrderRequest;
import com.example.demo.dto.response.PaymentSessionResponse;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrPaymentService {

    private final PaymentSessionRepository paymentSessionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Initiate a QR payment session. Order is NOT created yet.
     * Returns paymentRef, QR URL, and expiry info.
     */
    @Transactional
    public PaymentSessionResponse initiatePayment(String username, OrderRequest orderRequest, double totalAmount) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate unique paymentRef
        String randomStr = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String paymentRef = "DH" + randomStr;

        // Generate QR URL (VietQR)
        String qrCodeUrl = String.format(
                "https://img.vietqr.io/image/BIDV-96247111204-compact2.png?amount=%d&addInfo=%s&accountName=NGUYEN%%20DUC%%20KHANH",
                (long) totalAmount, paymentRef
        );

        // Serialize order request data to JSON for later order creation
        String orderDataJson;
        try {
            orderDataJson = objectMapper.writeValueAsString(orderRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order data", e);
        }

        // Expiry: 15 minutes
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.MINUTE, 15);
        Date expiresAt = cal.getTime();

        PaymentSession session = new PaymentSession();
        session.setPaymentRef(paymentRef);
        session.setQrCodeUrl(qrCodeUrl);
        session.setAmount(totalAmount);
        session.setStatus("PENDING");
        session.setCreatedAt(now);
        session.setExpiresAt(expiresAt);
        session.setUser(user);
        session.setOrderData(orderDataJson);

        paymentSessionRepository.save(session);

        log.info("QR Payment session initiated: ref={}, amount={}, user={}", paymentRef, totalAmount, username);

        return PaymentSessionResponse.builder()
                .paymentRef(paymentRef)
                .qrCodeUrl(qrCodeUrl)
                .amount(totalAmount)
                .status("PENDING")
                .expiresInSeconds(900L) // 15 minutes
                .build();
    }

    /**
     * Check payment status by paymentRef.
     */
    @Transactional(readOnly = true)
    public PaymentSessionResponse checkPaymentStatus(String paymentRef) {
        PaymentSession session = paymentSessionRepository.findByPaymentRef(paymentRef)
                .orElseThrow(() -> new RuntimeException("Payment session not found: " + paymentRef));

        long remainingSeconds = 0;
        if (session.getExpiresAt() != null) {
            remainingSeconds = Math.max(0, (session.getExpiresAt().getTime() - System.currentTimeMillis()) / 1000);
        }

        return PaymentSessionResponse.builder()
                .paymentRef(session.getPaymentRef())
                .qrCodeUrl(session.getQrCodeUrl())
                .amount(session.getAmount())
                .status(session.getStatus())
                .expiresInSeconds(remainingSeconds)
                .build();
    }

    /**
     * Get payment session entity by paymentRef (used internally by SePayPollingService).
     */
    public Optional<PaymentSession> findByPaymentRef(String paymentRef) {
        return paymentSessionRepository.findByPaymentRef(paymentRef);
    }

    /**
     * Get all PENDING sessions for polling.
     */
    public java.util.List<PaymentSession> findAllPendingSessions() {
        return paymentSessionRepository.findByStatus("PENDING");
    }
}

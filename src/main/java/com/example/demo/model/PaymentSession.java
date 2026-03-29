package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Table(name = "payment_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_ref", unique = true, nullable = false)
    private String paymentRef;

    @Column(name = "qr_code_url", length = 1000)
    private String qrCodeUrl;

    private Double amount;

    // Status: PENDING, PAID, EXPIRED
    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Snapshot of order data (stored as JSON string)
    @Column(name = "order_data", columnDefinition = "TEXT")
    private String orderData;
}

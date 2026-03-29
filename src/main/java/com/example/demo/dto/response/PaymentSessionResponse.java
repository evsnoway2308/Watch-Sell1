package com.example.demo.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSessionResponse {
    private String paymentRef;
    private String qrCodeUrl;
    private Double amount;
    private String status;
    private Long expiresInSeconds;
}

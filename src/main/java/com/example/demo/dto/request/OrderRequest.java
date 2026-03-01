package com.example.demo.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    private String shippingAddress;
    private String phoneNumber;
    private String notes;
    private String paymentMethod; // e.g., "COD", "BANK_TRANSFER"
}

package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SePayPollingService {

    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @Value("${SEPAY_API_TOKEN}")
    private String apiToken;

    private static final String SEPAY_API_URL = "https://my.sepay.vn/userapi/transactions/list?account_number=4506630423&limit=20";

    @Scheduled(fixedDelay = 60000) // Run every 60 seconds
    @Transactional
    public void fetchAndProcessTransactions() {
        if (apiToken == null || apiToken.isEmpty() || "PLACEHOLDER".equals(apiToken)) {
            log.warn("SEPAY_API_TOKEN is not configured. Polling skipped.");
            return;
        }

        log.info("Polling SePay API for recent transactions...");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    SEPAY_API_URL, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode transactions = root.path("transactions");

                if (transactions.isArray()) {
                    for (JsonNode txn : transactions) {
                        double amountIn = txn.path("amount_in").asDouble(0.0);
                        String content = txn.path("transaction_content").asText("");

                        if (amountIn > 0 && content != null && !content.trim().isEmpty()) {
                            processTransaction(amountIn, content.trim().toUpperCase());
                        }
                    }
                }
            } else {
                log.error("Failed to fetch SePay transactions. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error polling SePay API: {}", e.getMessage(), e);
        }
    }

    private void processTransaction(double amountIn, String content) {
        // Try to find if any pending order's paymentRef is included in the transfer content
        // E.g., user might send "Thanh toan don hang DH123456" -> we look for DH123456
        
        // This is a simplified approach: we can fetch all PENDING orders and check if their ref is in the content
        // Or we can extract DHXXXXXX from content and query db.
        
        // Let's rely on finding by substring
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("DH[A-Z0-9]{5,}");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        String extractedRef = null;
        if (matcher.find()) {
            extractedRef = matcher.group();
        }

        if (extractedRef != null) {
            List<Order> matchingOrders = orderRepository.findByPaymentRefAndStatus(extractedRef, "PENDING");
            
            for (Order order : matchingOrders) {
                if (amountIn >= order.getTotalAmount()) {
                    order.setStatus("PAID");
                    orderRepository.save(order);
                    log.info("Order {} updated to PAID via SePay Polling (Ref: {})", order.getId(), extractedRef);
                } else {
                    log.warn("Amount mismatch for Order {}. Expected: {}, Received: {}", 
                            order.getId(), order.getTotalAmount(), amountIn);
                }
            }
        }
    }
}

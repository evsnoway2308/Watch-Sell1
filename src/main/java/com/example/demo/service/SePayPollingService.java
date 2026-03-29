package com.example.demo.service;

import com.example.demo.dto.request.OrderRequest;
import com.example.demo.model.Order;
import com.example.demo.model.PaymentSession;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SePayPollingService {

    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final QrPaymentService qrPaymentService;
    private final OrderService orderService;
    private final PaymentSessionRepository paymentSessionRepository;



    @Value("${SEPAY_API_TOKEN:PLACEHOLDER}")
    private String apiToken;

    private static final String SEPAY_API_URL =
            "https://my.sepay.vn/userapi/transactions/list?account_number=4506630423&limit=20";

    @Scheduled(fixedDelay = 30000) // Run every 30 seconds
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
        // Extract paymentRef pattern DH + 6 alphanumeric chars
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("DH[A-Z0-9]{6}");
        java.util.regex.Matcher matcher = pattern.matcher(content);

        String extractedRef = null;
        if (matcher.find()) {
            extractedRef = matcher.group();
        }

        if (extractedRef == null) return;

        final String ref = extractedRef;

        // 1. Check NEW flow: QR Payment Sessions (order NOT yet created)
        Optional<PaymentSession> sessionOpt = qrPaymentService.findByPaymentRef(ref);
        if (sessionOpt.isPresent()) {
            PaymentSession session = sessionOpt.get();
            if ("PENDING".equals(session.getStatus())) {
                if (amountIn >= session.getAmount()) {
                    try {
                        // Mark session as PAID and save
                        session.setStatus("PAID");
                        paymentSessionRepository.save(session);

                        // Create the actual order now that payment is confirmed
                        OrderRequest orderRequest = objectMapper.readValue(session.getOrderData(), OrderRequest.class);
                        String username = session.getUser().getUsername();

                        Order createdOrder = orderService.createOrderAfterPayment(username, orderRequest, ref);

                        log.info("QR Payment confirmed for ref={}. Order {} created for user={}.",
                                ref, createdOrder.getId(), username);
                    } catch (Exception e) {
                        log.error("Failed to create order after QR payment for ref={}: {}", ref, e.getMessage(), e);
                        // Revert session status so it can be retried? Keep PAID to avoid duplicate
                        session.setStatus("PAID"); // still mark PAID to avoid retry
                    }
                } else {
                    log.warn("Amount mismatch for QR session ref={}. Expected: {}, Received: {}",
                            ref, session.getAmount(), amountIn);
                }
            }
            return; // Don't process old-flow orders for same ref
        }

        // 2. Fallback: OLD flow - check existing PENDING orders (legacy support)
        List<Order> matchingOrders = orderRepository.findByPaymentRefAndStatus(ref, "PENDING");
        for (Order order : matchingOrders) {
            if (amountIn >= order.getTotalAmount()) {
                order.setStatus("PAID");
                orderRepository.save(order);
                log.info("Order {} updated to PAID via SePay Polling (Ref: {})", order.getId(), ref);
                break;
            } else {
                log.warn("Amount mismatch for Order {}. Expected: {}, Received: {}",
                        order.getId(), order.getTotalAmount(), amountIn);
            }
        }
    }
}

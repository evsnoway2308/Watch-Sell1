package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SePayPollingService {

    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @Value("${SEPAY_API_TOKEN:PLACEHOLDER}")
    private String apiToken;

    private static final String SEPAY_API_URL =
            "https://my.sepay.vn/userapi/transactions/list?account_number=4506630423&limit=20";

    /**
     * Mỗi 30 giây: gọi SePay API lấy giao dịch mới nhất,
     * tìm đơn hàng PENDING khớp với nội dung chuyển khoản,
     * cập nhật status = PAID nếu số tiền đủ.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void fetchAndProcessTransactions() {
        if (apiToken == null || apiToken.isEmpty() || "PLACEHOLDER".equals(apiToken)) {
            log.warn("SEPAY_API_TOKEN chưa được cấu hình. Bỏ qua polling.");
            return;
        }

        log.info("Đang polling SePay API...");

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
                log.error("SePay API trả về lỗi. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Lỗi khi polling SePay API: {}", e.getMessage(), e);
        }
    }

    private void processTransaction(double amountIn, String content) {
        // Tìm mã đơn hàng dạng DH + 6 ký tự alphanumeric
        Pattern pattern = Pattern.compile("DH[A-Z0-9]{6}");
        Matcher matcher = pattern.matcher(content);

        if (!matcher.find()) return;

        String paymentRef = matcher.group();

        // Tìm đơn hàng PENDING có paymentRef khớp
        List<Order> matchingOrders = orderRepository.findByPaymentRefAndStatus(paymentRef, "PENDING");

        for (Order order : matchingOrders) {
            if (amountIn >= order.getTotalAmount()) {
                order.setStatus("PAID");
                orderRepository.save(order);
                log.info("✅ Đơn hàng #{} đã được cập nhật PAID (Ref: {}, Số tiền: {})",
                        order.getId(), paymentRef, amountIn);
                break; // Chỉ xử lý 1 đơn để tránh trùng lặp
            } else {
                log.warn("⚠️ Số tiền không khớp cho đơn #{} (Ref: {}). Yêu cầu: {}, Nhận: {}",
                        order.getId(), paymentRef, order.getTotalAmount(), amountIn);
            }
        }
    }
}

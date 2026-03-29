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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SePayPollingService {

    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final ObjectMapper objectMapper;
    private final QrPaymentService qrPaymentService;
    private final OrderService orderService;

    @Value("${SEPAY_API_TOKEN:PLACEHOLDER}")
    private String apiToken;

    private static final String SEPAY_API_URL =
            "https://my.sepay.vn/userapi/transactions/list?account_number=4506630423&limit=20";

    /**
     * Mỗi 30 giây poll SePay API kiểm tra giao dịch mới.
     * Khi tìm thấy giao dịch khớp PaymentSession PENDING:
     *   → Đánh dấu session PAID
     *   → Tạo đơn hàng + trừ kho (createOrderAfterPayment)
     * Frontend đang poll /check-payment/{ref} sẽ phát hiện status=PAID → thông báo thành công.
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
                log.error("SePay API lỗi. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Lỗi khi polling SePay API: {}", e.getMessage(), e);
        }
    }

    private void processTransaction(double amountIn, String content) {
        // Tìm mã dạng DH + 6 ký tự
        Pattern pattern = Pattern.compile("DH[A-Z0-9]{6}");
        Matcher matcher = pattern.matcher(content);

        if (!matcher.find()) return;

        String paymentRef = matcher.group();

        // Kiểm tra PaymentSession (QR flow) - chưa tạo đơn hàng
        Optional<PaymentSession> sessionOpt = paymentSessionRepository.findByPaymentRef(paymentRef);
        if (sessionOpt.isPresent()) {
            PaymentSession session = sessionOpt.get();

            if (!"PENDING".equals(session.getStatus())) return; // Đã xử lý rồi

            if (amountIn >= session.getAmount()) {
                try {
                    // 1. Đánh dấu session PAID để frontend biết
                    session.setStatus("PAID");
                    paymentSessionRepository.save(session);

                    // 2. Tạo đơn hàng + trừ kho (chỉ bây giờ mới làm)
                    OrderRequest orderRequest = objectMapper.readValue(session.getOrderData(), OrderRequest.class);
                    String username = session.getUser().getUsername();

                    Order order = orderService.createOrderAfterPayment(username, orderRequest, paymentRef);

                    log.info("✅ Thanh toán QR xác nhận! Đơn hàng #{} tạo thành công cho user={} (Ref: {})",
                            order.getId(), username, paymentRef);

                } catch (Exception e) {
                    log.error("❌ Lỗi tạo đơn hàng sau thanh toán ref={}: {}", paymentRef, e.getMessage(), e);
                    // Session vẫn PAID để tránh xử lý lại, cần can thiệp thủ công nếu cần
                }
            } else {
                log.warn("⚠️ Số tiền không khớp ref={}. Cần: {}, Nhận: {}",
                        paymentRef, session.getAmount(), amountIn);
            }
            return;
        }

        // Fallback: kiểm tra đơn hàng COD PENDING cũ (nếu có paymentRef)
        List<Order> matchingOrders = orderRepository.findByPaymentRefAndStatus(paymentRef, "PENDING");
        for (Order order : matchingOrders) {
            if (amountIn >= order.getTotalAmount()) {
                order.setStatus("PAID");
                orderRepository.save(order);
                log.info("✅ Đơn hàng #{} cập nhật PAID (Ref: {})", order.getId(), paymentRef);
                break;
            }
        }
    }
}

package order_service.dto;

import order_service.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice,
        OrderStatus status,
        Instant createdAt) {
}
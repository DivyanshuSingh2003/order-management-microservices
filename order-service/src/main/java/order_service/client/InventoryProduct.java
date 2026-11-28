package order_service.client;

import java.math.BigDecimal;

public record InventoryProduct(
        Long id,
        String sku,
        String name,
        BigDecimal price,
        int quantity) {
}
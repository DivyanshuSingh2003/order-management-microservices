package inventory_service.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustRequest(@NotNull Integer delta) {
}
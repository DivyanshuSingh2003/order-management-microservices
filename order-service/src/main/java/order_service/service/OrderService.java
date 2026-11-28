package order_service.service;

import order_service.dto.OrderRequest;
import order_service.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse create(OrderRequest request);
    List<OrderResponse> getAll();
    OrderResponse getById(Long id);
    OrderResponse cancel(Long id);
}
package order_service.service.impl;

import order_service.client.InventoryClient;
import order_service.client.InventoryProduct;
import order_service.dto.OrderRequest;
import order_service.dto.OrderResponse;
import order_service.entity.Order;
import order_service.entity.OrderStatus;
import order_service.exception.ConflictException;
import order_service.exception.ResourceNotFoundException;
import order_service.repository.OrderRepository;
import order_service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public OrderServiceImpl(OrderRepository orderRepository, InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
    }

    @Override
    public OrderResponse create(OrderRequest request) {
        // 1. Reserve stock first; this also tells us the product's name and price.
        InventoryProduct product = inventoryClient.adjustStock(request.productId(), -request.quantity());

        Order order = new Order();
        order.setProductId(product.id());
        order.setSku(product.sku());
        order.setProductName(product.name());
        order.setUnitPrice(product.price());
        order.setQuantity(request.quantity());
        order.setTotalPrice(product.price().multiply(BigDecimal.valueOf(request.quantity())));
        order.setStatus(OrderStatus.CONFIRMED);

        // 2. Save the order. If that fails, give the stock back (compensation).
        try {
            return toResponse(orderRepository.save(order));
        } catch (RuntimeException e) {
            try {
                inventoryClient.adjustStock(request.productId(), request.quantity());
            } catch (RuntimeException releaseFailure) {
                log.error("Could not release {} units of product {} after a failed order save",
                        request.quantity(), request.productId(), releaseFailure);
            }
            throw e;
        }
    }

    @Override
    public List<OrderResponse> getAll() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public OrderResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public OrderResponse cancel(Long id) {
        Order order = findOrThrow(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Order " + id + " is already cancelled");
        }
        inventoryClient.adjustStock(order.getProductId(), order.getQuantity());
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    private Order findOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private OrderResponse toResponse(Order o) {
        return new OrderResponse(o.getId(), o.getProductId(), o.getSku(), o.getProductName(),
                o.getUnitPrice(), o.getQuantity(), o.getTotalPrice(), o.getStatus(), o.getCreatedAt());
    }
}
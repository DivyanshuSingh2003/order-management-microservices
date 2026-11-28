package order_service.service.impl;

import order_service.client.InventoryClient;
import order_service.client.InventoryProduct;
import order_service.dto.OrderRequest;
import order_service.dto.OrderResponse;
import order_service.entity.Order;
import order_service.entity.OrderStatus;
import order_service.exception.ConflictException;
import order_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private OrderServiceImpl service;

    private InventoryProduct inventoryProduct() {
        return new InventoryProduct(1L, "SKU-1", "Keyboard", new BigDecimal("49.99"), 8);
    }

    private Order order(OrderStatus status) {
        Order o = new Order();
        o.setProductId(1L);
        o.setSku("SKU-1");
        o.setProductName("Keyboard");
        o.setUnitPrice(new BigDecimal("49.99"));
        o.setQuantity(2);
        o.setTotalPrice(new BigDecimal("99.98"));
        o.setStatus(status);
        return o;
    }

    @Test
    void create_reservesStockAndSavesConfirmedOrder() {
        when(inventoryClient.adjustStock(1L, -2)).thenReturn(inventoryProduct());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse result = service.create(new OrderRequest(1L, 2));

        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.totalPrice()).isEqualByComparingTo("99.98");
    }

    @Test
    void create_doesNotSaveWhenStockIsRejected() {
        when(inventoryClient.adjustStock(1L, -50)).thenThrow(new ConflictException("no stock"));

        assertThatThrownBy(() -> service.create(new OrderRequest(1L, 50)))
                .isInstanceOf(ConflictException.class);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void create_releasesStockWhenSaveFails() {
        when(inventoryClient.adjustStock(1L, -2)).thenReturn(inventoryProduct());
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.create(new OrderRequest(1L, 2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");
        verify(inventoryClient).adjustStock(1L, 2);
    }

    @Test
    void cancel_releasesStockAndMarksCancelled() {
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse result = service.cancel(5L);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryClient).adjustStock(1L, 2);
    }

    @Test
    void cancel_failsWhenAlreadyCancelled() {
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order(OrderStatus.CANCELLED)));

        assertThatThrownBy(() -> service.cancel(5L)).isInstanceOf(ConflictException.class);
        verify(inventoryClient, never()).adjustStock(anyLong(), anyInt());
    }
}
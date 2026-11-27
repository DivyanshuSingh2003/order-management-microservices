package inventory_service.service.impl;

import inventory_service.dto.ProductRequest;
import inventory_service.dto.ProductResponse;
import inventory_service.dto.StockAdjustRequest;
import inventory_service.entity.Product;
import inventory_service.exception.DuplicateResourceException;
import inventory_service.exception.InsufficientStockException;
import inventory_service.exception.ResourceNotFoundException;
import inventory_service.repository.ProductRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductServiceImpl service;

    private Product product(String sku, int quantity) {
        Product p = new Product();
        p.setSku(sku);
        p.setName("Test");
        p.setPrice(new BigDecimal("10.00"));
        p.setQuantity(quantity);
        return p;
    }

    @Test
    void adjustStock_addsQuantity() {
        when(repository.findById(1L)).thenReturn(Optional.of(product("SKU-1", 10)));
        when(repository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = service.adjustStock(1L, new StockAdjustRequest(5));

        assertThat(result.quantity()).isEqualTo(15);
    }

    @Test
    void adjustStock_removesQuantity() {
        when(repository.findById(1L)).thenReturn(Optional.of(product("SKU-1", 10)));
        when(repository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = service.adjustStock(1L, new StockAdjustRequest(-4));

        assertThat(result.quantity()).isEqualTo(6);
    }

    @Test
    void adjustStock_throwsWhenStockWouldGoNegative() {
        when(repository.findById(1L)).thenReturn(Optional.of(product("SKU-1", 3)));

        assertThatThrownBy(() -> service.adjustStock(1L, new StockAdjustRequest(-10)))
                .isInstanceOf(InsufficientStockException.class);
        verify(repository, never()).save(any(Product.class));
    }

    @Test
    void getById_throwsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_throwsWhenSkuExists() {
        when(repository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new ProductRequest("SKU-1", "Name", new BigDecimal("5.00"), 1)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(repository, never()).save(any(Product.class));
    }
}
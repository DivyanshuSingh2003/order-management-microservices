package inventory_service.service;

import inventory_service.dto.ProductRequest;
import inventory_service.dto.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse create(ProductRequest request);
    List<ProductResponse> getAll();
    ProductResponse getById(Long id);
    ProductResponse update(Long id, ProductRequest request);
    void delete(Long id);
}
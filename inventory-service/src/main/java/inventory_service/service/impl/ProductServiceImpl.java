package inventory_service.service.impl;

import inventory_service.dto.ProductRequest;
import inventory_service.dto.ProductResponse;
import inventory_service.entity.Product;
import inventory_service.exception.DuplicateResourceException;
import inventory_service.exception.ResourceNotFoundException;
import inventory_service.repository.ProductRepository;
import inventory_service.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;

    public ProductServiceImpl(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProductResponse create(ProductRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("SKU already exists: " + request.sku());
        }
        Product product = new Product();
        apply(product, request);
        return toResponse(repository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        if (!product.getSku().equals(request.sku()) && repository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("SKU already exists: " + request.sku());
        }
        apply(product, request);
        return toResponse(repository.save(product));
    }

    @Override
    public void delete(Long id) {
        repository.delete(findOrThrow(id));
    }

    private Product findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private void apply(Product product, ProductRequest r) {
        product.setSku(r.sku());
        product.setName(r.name());
        product.setPrice(r.price());
        product.setQuantity(r.quantity());
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getPrice(), p.getQuantity());
    }
}
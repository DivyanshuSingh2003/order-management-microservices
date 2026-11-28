package order_service.client;

import order_service.exception.ConflictException;
import order_service.exception.ResourceNotFoundException;
import order_service.exception.ServiceUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient inventoryRestClient) {
        this.restClient = inventoryRestClient;
    }

    /** Negative delta reserves stock, positive delta releases it. */
    public InventoryProduct adjustStock(Long productId, int delta) {
        try {
            return restClient.patch()
                    .uri("/api/products/{id}/stock", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StockAdjustRequest(delta))
                    .retrieve()
                    .body(InventoryProduct.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        } catch (HttpClientErrorException.Conflict e) {
            throw new ConflictException(
                    "Stock could not be reserved for product " + productId
                            + " (insufficient stock or a concurrent update)");
        } catch (RestClientResponseException e) {
            throw new ServiceUnavailableException(
                    "Inventory service returned an unexpected response: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("Inventory service is unreachable", e);
        }
    }
}

package com.supplyhub.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Component
public class CatalogClient {
    private final RestClient client;

    public CatalogClient(
            @Value("${catalog.service.url:http://localhost:8081}")
            String baseUrl
    ) {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public CatalogProduct requireActiveProduct(String sku) {
        try {
            CatalogProduct product = client.get()
                    .uri("/api/catalog/products/sku/{sku}", sku)
                    .retrieve()
                    .body(CatalogProduct.class);

            if (product == null) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Catalog returned an empty response"
                );
            }

            if (!product.active()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Catalog product is inactive: " + sku
                );
            }

            return product;

        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Product does not exist in Catalog: " + sku
            );

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Catalog Service is unavailable; inventory was not changed",
                    e
            );
        }
    }

    public record CatalogProduct(
            Long id,
            String sku,
            String name,
            boolean active
    ) {}
}

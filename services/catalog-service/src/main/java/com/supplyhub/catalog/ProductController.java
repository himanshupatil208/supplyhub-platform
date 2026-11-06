
package com.supplyhub.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/catalog/products")
public class ProductController {
    private final ProductRepository repo;

    public ProductController(ProductRepository repo) {
        this.repo = repo;
    }

    public record ProductRequest(
            @NotBlank String sku,
            @NotBlank String name,
            String description,
            String category,
            @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
            boolean active
    ) {}

    public record ProductResponse(
            Long id,
            String sku,
            String name,
            String description,
            String category,
            BigDecimal unitPrice,
            boolean active
    ) {
        static ProductResponse from(Product p) {
            return new ProductResponse(
                    p.getId(),
                    p.getSku(),
                    p.getName(),
                    p.getDescription(),
                    p.getCategory(),
                    p.getUnitPrice(),
                    p.isActive()
            );
        }
    }

    @GetMapping
    public List<ProductResponse> all(
            @RequestParam(required = false) String q
    ) {
        var products = (q == null || q.isBlank())
                ? repo.findAll()
                : repo.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(q, q);

        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/sku/{sku}")
    public ProductResponse bySku(@PathVariable String sku) {
        return ProductResponse.from(
                repo.findBySku(sku)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found: " + sku
                        ))
        );
    }

    @GetMapping("/{id}")
    public ProductResponse one(@PathVariable Long id) {
        return ProductResponse.from(
                repo.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        ))
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @Valid @RequestBody ProductRequest r
    ) {
        if (repo.findBySku(r.sku()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "SKU already exists"
            );
        }

        return ProductResponse.from(repo.save(
                new Product(
                        r.sku(),
                        r.name(),
                        r.description(),
                        r.category(),
                        r.unitPrice(),
                        r.active()
                )
        ));
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest r
    ) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        if (!p.getSku().equals(r.sku())
                && repo.findBySku(r.sku()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "SKU already exists"
            );
        }

        p.update(
                r.name(),
                r.description(),
                r.category(),
                r.unitPrice(),
                r.active()
        );

        return ProductResponse.from(repo.save(p));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repo.delete(
                repo.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        ))
        );
    }
}

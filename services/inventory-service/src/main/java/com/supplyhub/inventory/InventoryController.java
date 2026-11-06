
package com.supplyhub.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final StockRepository repo;
    private final CatalogClient catalog;

    public InventoryController(
            StockRepository repo,
            CatalogClient catalog
    ) {
        this.repo = repo;
        this.catalog = catalog;
    }

    public record StockRequest(
            @NotBlank String sku,
            @Min(0) int availableQuantity,
            @Min(0) int reorderLevel,
            @NotBlank String warehouse
    ) {}

    public record StockUpdateRequest(
            @Min(0) int availableQuantity,
            @Min(0) int reorderLevel,
            @NotBlank String warehouse
    ) {}

    public record AdjustRequest(
            @NotNull Integer quantity
    ) {}

    public record ReservationRequest(
            @NotBlank String sku,
            @Min(1) int quantity
    ) {}

    public record StockResponse(
            Long id,
            String sku,
            String productName,
            int availableQuantity,
            int reservedQuantity,
            int reorderLevel,
            String warehouse,
            boolean lowStock
    ) {
        static StockResponse from(StockItem s) {
            return new StockResponse(
                    s.getId(),
                    s.getSku(),
                    s.getProductName(),
                    s.getAvailableQuantity(),
                    s.getReservedQuantity(),
                    s.getReorderLevel(),
                    s.getWarehouse(),
                    s.getAvailableQuantity() <= s.getReorderLevel()
            );
        }
    }

    @GetMapping("/stock")
    public List<StockResponse> all() {
        return repo.findAll()
                .stream()
                .map(StockResponse::from)
                .toList();
    }

    @PostMapping("/stock")
    @ResponseStatus(HttpStatus.CREATED)
    public StockResponse create(
            @Valid @RequestBody StockRequest r
    ) {
        String sku = r.sku().trim();

        if (repo.findBySku(sku).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inventory already exists for SKU: " + sku
            );
        }

        var product = catalog.requireActiveProduct(sku);

        return StockResponse.from(
                repo.save(new StockItem(
                        sku,
                        product.name(),
                        r.availableQuantity(),
                        r.reorderLevel(),
                        r.warehouse().trim()
                ))
        );
    }

    @PutMapping("/stock/{sku}")
    @Transactional
    public StockResponse update(
            @PathVariable String sku,
            @Valid @RequestBody StockUpdateRequest r
    ) {
        StockItem item = find(sku);
        var product = catalog.requireActiveProduct(sku);

        try {
            item.updateDetails(
                    product.name(),
                    r.availableQuantity(),
                    r.reorderLevel(),
                    r.warehouse().trim()
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }

        return StockResponse.from(item);
    }

    @PatchMapping("/stock/{sku}/adjust")
    @Transactional
    public StockResponse adjust(
            @PathVariable String sku,
            @Valid @RequestBody AdjustRequest r
    ) {
        StockItem s = find(sku);
        catalog.requireActiveProduct(sku);

        try {
            s.adjust(r.quantity());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }

        return StockResponse.from(s);
    }

    @PostMapping("/reservations")
    @Transactional
    public StockResponse reserve(
            @Valid @RequestBody ReservationRequest r
    ) {
        StockItem s = find(r.sku());
        catalog.requireActiveProduct(r.sku());

        try {
            s.reserve(r.quantity());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    e.getMessage()
            );
        }

        return StockResponse.from(s);
    }

    @PostMapping("/reservations/release")
    @Transactional
    public StockResponse release(
            @Valid @RequestBody ReservationRequest r
    ) {
        StockItem s = find(r.sku());

        try {
            s.release(r.quantity());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }

        return StockResponse.from(s);
    }

    private StockItem find(String sku) {
        return repo.findBySku(sku)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory not found for SKU: " + sku
                ));
    }
}

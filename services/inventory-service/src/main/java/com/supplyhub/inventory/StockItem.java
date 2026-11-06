
package com.supplyhub.inventory;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "stock_items",
        uniqueConstraints = @UniqueConstraint(columnNames = "sku")
)
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String productName;

    private int availableQuantity;
    private int reservedQuantity;
    private int reorderLevel;
    private String warehouse;
    private Instant updatedAt = Instant.now();

    protected StockItem() {}

    public StockItem(
            String sku,
            String productName,
            int qty,
            int reorder,
            String warehouse
    ) {
        this.sku = sku;
        this.productName = productName;
        this.availableQuantity = qty;
        this.reorderLevel = reorder;
        this.warehouse = warehouse;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(
            String productName,
            int availableQuantity,
            int reorderLevel,
            String warehouse
    ) {
        if (availableQuantity < 0 || reorderLevel < 0) {
            throw new IllegalArgumentException(
                    "Quantities cannot be negative"
            );
        }

        this.productName = productName;
        this.availableQuantity = availableQuantity;
        this.reorderLevel = reorderLevel;
        this.warehouse = warehouse;
        this.updatedAt = Instant.now();
    }

    public void adjust(int delta) {
        if (availableQuantity + delta < 0) {
            throw new IllegalArgumentException(
                    "Stock cannot become negative"
            );
        }

        availableQuantity += delta;
        updatedAt = Instant.now();
    }

    public void reserve(int qty) {
        if (qty <= 0 || availableQuantity < qty) {
            throw new IllegalArgumentException(
                    "Insufficient available stock for " + sku
            );
        }

        availableQuantity -= qty;
        reservedQuantity += qty;
        updatedAt = Instant.now();
    }

    public void release(int qty) {
        if (qty <= 0 || reservedQuantity < qty) {
            throw new IllegalArgumentException(
                    "Invalid release quantity"
            );
        }

        reservedQuantity -= qty;
        availableQuantity += qty;
        updatedAt = Instant.now();
    }
}

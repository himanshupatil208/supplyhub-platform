package com.supplyhub.catalog;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = "sku"))
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String sku;
    @Column(nullable = false)
    private String name;
    @Column(length = 1200)
    private String description;
    private String category;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    private boolean active = true;
    private Instant createdAt = Instant.now();

    protected Product() {
    }

    public Product(String sku, String name, String description, String category, BigDecimal unitPrice, boolean active) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.unitPrice = unitPrice;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void update(String name, String description, String category, BigDecimal unitPrice, boolean active) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.unitPrice = unitPrice;
        this.active = active;
    }
}

package com.supplyhub.orders;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_lines")
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private PurchaseOrder order;
    private String sku;
    private String productName;
    private int quantity;
    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected OrderLine() {
    }

    public OrderLine(String sku, String productName, int quantity, BigDecimal unitPrice) {
        this.sku = sku;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    void setOrder(PurchaseOrder order) {
        this.order = order;
    }

    public String getSku() {
        return sku;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}

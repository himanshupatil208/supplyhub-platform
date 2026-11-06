package com.supplyhub.orders;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long customerId;
    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false)
    private String status = "PLACED";
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderLine> items = new ArrayList<>();

    protected PurchaseOrder() {
    }

    public PurchaseOrder(Long customerId, String customerName, BigDecimal total) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.total = total;
    }

    public void addItem(OrderLine item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderLine> getItems() {
        return items;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

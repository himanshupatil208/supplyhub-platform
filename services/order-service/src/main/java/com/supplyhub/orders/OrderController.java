package com.supplyhub.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository repo;
    private final RestClient inventory;
    private final RestClient notifications;

    public OrderController(OrderRepository repo, @Value("${INVENTORY_SERVICE_URL:http://localhost:8082}") String inventoryUrl, @Value("${NOTIFICATION_SERVICE_URL:http://localhost:8085}") String notificationUrl) {
        this.repo = repo;
        this.inventory = RestClient.builder().baseUrl(inventoryUrl).build();
        this.notifications = RestClient.builder().baseUrl(notificationUrl).build();
    }

    public record LineRequest(@NotBlank String sku, @NotBlank String productName, @Min(1) int quantity,
                              @NotNull @DecimalMin("0.01") BigDecimal unitPrice) {
    }

    public record OrderRequest(@NotNull Long customerId, @NotBlank String customerName,
                               @NotEmpty List<@Valid LineRequest> items) {
    }

    public record LineResponse(String sku, String productName, int quantity, BigDecimal unitPrice,
                               BigDecimal lineTotal) {
    }

    public record OrderResponse(Long id, Long customerId, String customerName, String status, BigDecimal total,
                                String createdAt, List<LineResponse> items) {
        static OrderResponse from(PurchaseOrder o) {
            return new OrderResponse(o.getId(), o.getCustomerId(), o.getCustomerName(), o.getStatus(), o.getTotal(), o.getCreatedAt().toString(), o.getItems().stream().map(i -> new LineResponse(i.getSku(), i.getProductName(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal())).toList());
        }
    }

    public record ReservationRequest(String sku, int quantity) {
    }

    @GetMapping
    public List<OrderResponse> all() {
        return repo.findAll().stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse one(@PathVariable Long id) {
        return OrderResponse.from(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public OrderResponse create(@Valid @RequestBody OrderRequest r) {
        // Reserve all requested SKUs before persisting the order. Release prior reservations if a later SKU fails.
        List<LineRequest> reserved = new ArrayList<>();
        try {
            for (LineRequest item : r.items()) {
                inventory.post().uri("/api/inventory/reservations").contentType(MediaType.APPLICATION_JSON).body(new ReservationRequest(item.sku(), item.quantity())).retrieve().toBodilessEntity();
                reserved.add(item);
            }
        } catch (Exception ex) {
            for (LineRequest item : reserved) {
                try {
                    inventory.post().uri("/api/inventory/reservations/release").contentType(MediaType.APPLICATION_JSON).body(new ReservationRequest(item.sku(), item.quantity())).retrieve().toBodilessEntity();
                } catch (Exception ignored) {
                }
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to reserve inventory: " + ex.getMessage());
        }
        BigDecimal total = r.items().stream().map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        PurchaseOrder order = new PurchaseOrder(r.customerId(), r.customerName(), total);
        r.items().forEach(i -> order.addItem(new OrderLine(i.sku(), i.productName(), i.quantity(), i.unitPrice())));
        PurchaseOrder saved = repo.save(order);
        try {
            notifications.post().uri("/api/notifications").contentType(MediaType.APPLICATION_JSON).body(Map.of("type", "ORDER_PLACED", "message", "Order #" + saved.getId() + " placed for " + saved.getCustomerName(), "referenceId", saved.getId().toString())).retrieve().toBodilessEntity();
        } catch (Exception ignored) {
        }
        return OrderResponse.from(saved);
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public OrderResponse status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        PurchaseOrder o = find(id);
        String status = body.getOrDefault("status", "").toUpperCase(Locale.ROOT);
        if (!Set.of("PLACED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status");
        if ("CANCELLED".equals(status) && !"CANCELLED".equals(o.getStatus())) for (OrderLine line : o.getItems())
            try {
                inventory.post().uri("/api/inventory/reservations/release").contentType(MediaType.APPLICATION_JSON).body(new ReservationRequest(line.getSku(), line.getQuantity())).retrieve().toBodilessEntity();
            } catch (Exception ignored) {
            }
        o.setStatus(status);
        return OrderResponse.from(o);
    }

    private PurchaseOrder find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}

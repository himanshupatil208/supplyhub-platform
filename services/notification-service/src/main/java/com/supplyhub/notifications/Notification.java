package com.supplyhub.notifications;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String type;
    @Column(nullable = false, length = 2000)
    private String message;
    private String referenceId;
    private Instant createdAt = Instant.now();

    protected Notification() {
    }

    public Notification(String type, String message, String referenceId) {
        this.type = type;
        this.message = message;
        this.referenceId = referenceId;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

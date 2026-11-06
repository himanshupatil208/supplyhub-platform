package com.supplyhub.customers;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String email;
    private String phone;
    private String company;
    private String status = "ACTIVE";
    private Instant createdAt = Instant.now();

    protected Customer() {
    }

    public Customer(String name, String email, String phone, String company) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.company = company;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCompany() {
        return company;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void update(String name, String email, String phone, String company, String status) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.company = company;
        this.status = status;
    }
}

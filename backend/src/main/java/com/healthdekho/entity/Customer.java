package com.healthdekho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Transient
    private String passwordHash;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phone;

    @Transient
    private String status;

    @Column(name = "address", length = 250)
    private String address;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Customer() {
    }

    public Customer(String email, String passwordHash, String fullName, LocalDate dateOfBirth,
                    String phone, String status) {
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.customerName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return customerName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public String getStatus() {
        return status == null ? "ACTIVE" : status;
    }
}

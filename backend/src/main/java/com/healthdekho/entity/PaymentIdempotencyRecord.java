package com.healthdekho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_idempotency",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_payment_idempotency_customer_key",
                columnNames = {"customer_id", "idempotency_key"}
        )
)
public class PaymentIdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private CustomerPolicy policy;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_json", nullable = false, columnDefinition = "TEXT")
    private String responseJson = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentIdempotencyRecord() {
    }

    public PaymentIdempotencyRecord(
            Customer customer,
            CustomerPolicy policy,
            String idempotencyKey,
            String requestHash
    ) {
        this.customer = customer;
        this.policy = policy;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public CustomerPolicy getPolicy() {
        return policy;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }
}

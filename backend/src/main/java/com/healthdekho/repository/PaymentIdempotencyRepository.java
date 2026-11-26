package com.healthdekho.repository;

import com.healthdekho.entity.PaymentIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIdempotencyRepository extends JpaRepository<PaymentIdempotencyRecord, UUID> {

    Optional<PaymentIdempotencyRecord> findByCustomer_IdAndIdempotencyKey(UUID customerId, String idempotencyKey);
}

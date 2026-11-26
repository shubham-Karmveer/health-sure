package com.healthdekho.repository;

import com.healthdekho.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select customer from Customer customer where customer.id = :customerId")
    Optional<Customer> findByIdForUpdate(@Param("customerId") UUID customerId);
}

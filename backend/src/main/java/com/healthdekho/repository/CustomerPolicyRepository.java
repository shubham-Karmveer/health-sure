package com.healthdekho.repository;

import com.healthdekho.entity.CustomerPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerPolicyRepository extends JpaRepository<CustomerPolicy, UUID> {

    List<CustomerPolicy> findAllByCustomer_IdOrderByCreatedAtDesc(UUID customerId);
}

package com.healthdekho.repository;

import com.healthdekho.entity.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, UUID> {
}

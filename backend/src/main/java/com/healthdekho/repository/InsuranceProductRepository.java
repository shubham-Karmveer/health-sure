package com.healthdekho.repository;

import com.healthdekho.entity.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, UUID> {

    Optional<InsuranceProduct> findByProductCode(String productCode);

    Optional<InsuranceProduct> findByProductCodeAndActiveTrue(String productCode);

    boolean existsByProductCode(String productCode);

    @Query("""
            select product from InsuranceProduct product
            where product.active = true
              and not exists (
                  select policy.id from CustomerPolicy policy
                  where policy.customer.id = :customerId
                    and policy.product.id = product.id
              )
            order by product.productCode
            """)
    List<InsuranceProduct> findAvailableForCustomer(@Param("customerId") UUID customerId);
}

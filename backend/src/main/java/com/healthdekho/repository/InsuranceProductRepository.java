package com.healthdekho.repository;

import com.healthdekho.entity.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {

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
    List<InsuranceProduct> findAvailableForCustomer(@Param("customerId") Long customerId);

    @Query("""
            select product from InsuranceProduct product
            where product.active = true
              and product.minEntryAge <= :age
              and product.maxEntryAge >= :age
              and not exists (
                  select policy.id from CustomerPolicy policy
                  where policy.customer.id = :customerId
                    and policy.product.id = product.id
              )
            order by product.productCode
            """)
    List<InsuranceProduct> findEligibleAvailableForCustomer(
            @Param("customerId") Long customerId,
            @Param("age") int age
    );

    @Query(
            value = """
                    select product from InsuranceProduct product
                    where product.active = true
                      and not exists (
                          select policy.id from CustomerPolicy policy
                          where policy.customer.id = :customerId
                            and policy.product.id = product.id
                      )
                      and (:productCode = '' or lower(product.productCode) like lower(concat('%', :productCode, '%')))
                      and (:policyName = '' or lower(product.policyName) like lower(concat('%', :policyName, '%')))
                      and (:policyType = '' or lower(product.policyType) like lower(concat('%', :policyType, '%')))
                    order by product.productCode
                    """,
            countQuery = """
                    select count(product) from InsuranceProduct product
                    where product.active = true
                      and not exists (
                          select policy.id from CustomerPolicy policy
                          where policy.customer.id = :customerId
                            and policy.product.id = product.id
                      )
                      and (:productCode = '' or lower(product.productCode) like lower(concat('%', :productCode, '%')))
                      and (:policyName = '' or lower(product.policyName) like lower(concat('%', :policyName, '%')))
                      and (:policyType = '' or lower(product.policyType) like lower(concat('%', :policyType, '%')))
                    """
    )
    Page<InsuranceProduct> searchAvailableForCustomer(
            @Param("customerId") Long customerId,
            @Param("productCode") String productCode,
            @Param("policyName") String policyName,
            @Param("policyType") String policyType,
            Pageable pageable
    );
}

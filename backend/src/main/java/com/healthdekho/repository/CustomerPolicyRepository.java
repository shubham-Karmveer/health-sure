package com.healthdekho.repository;

import com.healthdekho.entity.CustomerPolicy;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerPolicyRepository extends JpaRepository<CustomerPolicy, UUID> {

    List<CustomerPolicy> findAllByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    boolean existsByCustomer_IdAndProduct_Id(UUID customerId, UUID productId);

    Optional<CustomerPolicy> findByPolicyNumber(String policyNumber);

    Optional<CustomerPolicy> findByPolicyNumberAndCustomer_Id(String policyNumber, UUID customerId);

    @EntityGraph(attributePaths = {"customer", "product"})
    @Query("""
            select policy from CustomerPolicy policy
            where policy.customer.id = :customerId
              and policy.policyNumber = :policyNumber
            """)
    Page<CustomerPolicy> searchOwnedByPolicyNumber(
            @Param("customerId") UUID customerId,
            @Param("policyNumber") String policyNumber,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "product"})
    @Query("""
            select policy from CustomerPolicy policy
            where policy.customer.id = :customerId
              and lower(policy.customer.fullName) like lower(concat('%', :customerName, '%'))
            """)
    Page<CustomerPolicy> searchOwnedByCustomerName(
            @Param("customerId") UUID customerId,
            @Param("customerName") String customerName,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "product"})
    @Query("""
            select policy from CustomerPolicy policy
            where policy.customer.id = :customerId
              and policy.policyNumber = :policyNumber
              and policy.customer.dateOfBirth = :dateOfBirth
            """)
    Page<CustomerPolicy> searchOwnedByPolicyNumberAndDateOfBirth(
            @Param("customerId") UUID customerId,
            @Param("policyNumber") String policyNumber,
            @Param("dateOfBirth") LocalDate dateOfBirth,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select policy from CustomerPolicy policy
            where policy.policyNumber = :policyNumber and policy.customer.id = :customerId
            """)
    Optional<CustomerPolicy> findForUpdateByPolicyNumberAndCustomerId(
            @Param("policyNumber") String policyNumber,
            @Param("customerId") UUID customerId
    );
}

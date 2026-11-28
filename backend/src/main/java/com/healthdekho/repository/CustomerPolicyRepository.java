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

public interface CustomerPolicyRepository extends JpaRepository<CustomerPolicy, Long> {

    List<CustomerPolicy> findAllByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    boolean existsByCustomer_IdAndProduct_Id(Long customerId, Long productId);

    Optional<CustomerPolicy> findByPolicyNumber(String policyNumber);

    Optional<CustomerPolicy> findByPolicyNumberAndCustomer_Id(String policyNumber, Long customerId);

    @EntityGraph(attributePaths = {"customer", "product"})
    @Query("""
            select policy from CustomerPolicy policy
            where policy.customer.id = :customerId
              and policy.policyNumber = :policyNumber
            """)
    Page<CustomerPolicy> searchOwnedByPolicyNumber(
            @Param("customerId") Long customerId,
            @Param("policyNumber") String policyNumber,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "product"})
    @Query("""
            select policy from CustomerPolicy policy
            where policy.customer.id = :customerId
              and lower(policy.customer.customerName) like lower(concat('%', :customerName, '%'))
            """)
    Page<CustomerPolicy> searchOwnedByCustomerName(
            @Param("customerId") Long customerId,
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
            @Param("customerId") Long customerId,
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
            @Param("customerId") Long customerId
    );
}

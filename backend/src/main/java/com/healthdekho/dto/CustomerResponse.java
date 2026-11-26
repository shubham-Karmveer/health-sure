package com.healthdekho.dto;

import com.healthdekho.entity.Customer;

import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID customerId,
        String email,
        String fullName,
        LocalDate dateOfBirth,
        String phone
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getEmail(),
                customer.getFullName(),
                customer.getDateOfBirth(),
                customer.getPhone()
        );
    }
}

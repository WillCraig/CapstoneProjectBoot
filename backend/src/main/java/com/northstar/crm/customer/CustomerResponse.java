package com.northstar.crm.customer;

import java.time.OffsetDateTime;

public record CustomerResponse(
        String customerId,
        String fullName,
        String email,
        String status,
        OffsetDateTime createdAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getStatus(),
                customer.getCreatedAt()
        );
    }
}
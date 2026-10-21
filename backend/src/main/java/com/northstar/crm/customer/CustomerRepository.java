
package com.northstar.crm.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerId(String customerId);

    boolean existsByEmail(String email);
    boolean existsByCustomerId(String customerId);
}

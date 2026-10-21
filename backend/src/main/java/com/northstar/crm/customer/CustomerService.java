
package com.northstar.crm.customer;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<Customer> findByCustomerId(String customerId) {
        return customerRepository.findByCustomerId(customerId);
    }
    public List<Customer> listAll() {
        return customerRepository.findAll();
    }
}

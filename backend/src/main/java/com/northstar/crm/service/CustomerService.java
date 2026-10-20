package com.northstar.crm.service;

import com.northstar.crm.domain.CustomerEntity;
import com.northstar.crm.repo.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerEntity> listCustomers() {
        return customerRepository.findAllByOrderByIdAsc();
    }

    @Transactional
    public CustomerEntity createCustomer(CustomerRequest request) {
        CustomerEntity customer = new CustomerEntity();
        customer.setPublicId("CUS-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        customer.setFullName(request.fullName());
        customer.setStatus(request.status());
        return customerRepository.save(customer);
    }

    @Transactional
    public CustomerEntity updateCustomer(String publicId, CustomerRequest request) {
        CustomerEntity customer =
                customerRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () -> new UnknownCustomerException("Unknown customer: " + publicId));
        customer.setFullName(request.fullName());
        customer.setStatus(request.status());
        return customerRepository.save(customer);
    }

    public record CustomerRequest(String fullName, String status) {
    }
}


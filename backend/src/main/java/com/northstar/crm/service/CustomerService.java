package com.northstar.crm.service;

import com.northstar.crm.api.dto.CustomerResponse;
import com.northstar.crm.api.dto.SaveCustomerRequest;
import com.northstar.crm.domain.CustomerEntity;
import com.northstar.crm.repo.CustomerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

  private final CustomerRepository customerRepository;

  public CustomerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Transactional(readOnly = true)
  public List<CustomerResponse> list() {
    return customerRepository.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public CustomerResponse get(String publicId) {
    return customerRepository
        .findByPublicId(publicId)
        .map(this::toResponse)
        .orElseThrow(() -> new UnknownCustomerException("Unknown customer: " + publicId));
  }

  @Transactional
  public CustomerResponse create(SaveCustomerRequest request) {
    CustomerEntity customer = new CustomerEntity();
    customer.setPublicId(
        "CUS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 28).toUpperCase());
    customer.setFullName(request.fullName().trim());
    customer.setStatus(request.status());
    return toResponse(customerRepository.save(customer));
  }

  @Transactional
  public CustomerResponse update(String publicId, SaveCustomerRequest request) {
    CustomerEntity customer =
        customerRepository
            .findByPublicId(publicId)
            .orElseThrow(() -> new UnknownCustomerException("Unknown customer: " + publicId));
    customer.setFullName(request.fullName().trim());
    customer.setStatus(request.status());
    return toResponse(customerRepository.save(customer));
  }

  private CustomerResponse toResponse(CustomerEntity customer) {
    return new CustomerResponse(customer.getPublicId(), customer.getFullName(), customer.getStatus());
  }
}

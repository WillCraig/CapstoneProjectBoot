package com.northstar.crm.service;

import com.northstar.crm.api.dto.CreateCustomerRequest;
import com.northstar.crm.api.dto.CustomerResponse;
import com.northstar.crm.api.dto.UpdateCustomerRequest;
import com.northstar.crm.domain.CustomerEntity;
import com.northstar.crm.repo.CustomerRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer management slice of the capstone brief: create, search, profile read, update
 * (including lifecycle status changes), and delete. Delete closes the customer ({@code
 * status = CLOSED}) instead of removing the row, so the interaction history that references
 * the customer is never orphaned — the same preserve-the-audit-row rule as interaction
 * soft-delete.
 */
@Service
public class CustomerService {

  private final CustomerRepository customerRepository;

  public CustomerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Transactional(readOnly = true)
  public List<CustomerResponse> search(String query) {
    List<CustomerEntity> customers;
    if (query == null || query.isBlank()) {
      customers = customerRepository.findAllByOrderByFullNameAsc();
    } else {
      String term = query.trim();
      customers =
          customerRepository
              .findByPublicIdContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrderByFullNameAsc(
                  term, term);
    }
    return customers.stream().map(CustomerService::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public CustomerResponse get(String customerId) {
    return toResponse(findCustomer(customerId));
  }

  @Transactional
  public CustomerResponse create(CreateCustomerRequest request) {
    // 1. Build the entity from the validated request; name and status come from the agent.
    CustomerEntity entity = new CustomerEntity();
    entity.setFullName(request.fullName().trim());
    entity.setStatus(request.status());
    entity.setCreatedAt(Instant.now());
    // 2. public_id is NOT NULL, but the real value (CUS-####) derives from the database ID,
    //    which only exists after the first insert. A throwaway unique placeholder covers
    //    the gap; nothing outside this transaction can ever see it. It must fit the
    //    column: "PENDING-" (8) + 22 hex chars = 30 of the VARCHAR(32) limit.
    String placeholder = "PENDING-" + UUID.randomUUID().toString().replace("-", "").substring(0, 22);
    entity.setPublicId(placeholder);
    CustomerEntity saved = customerRepository.save(entity);
    // 3. Derive the public ID from the generated key, continuing the seeded pattern
    //    (row 1 = CUS-1001, row 2 = CUS-1002), then save the final value.
    saved.setPublicId("CUS-" + (1000 + saved.getId()));
    return toResponse(customerRepository.save(saved));
  }

  @Transactional
  public CustomerResponse update(String customerId, UpdateCustomerRequest request) {
    // 1. Load the customer or fail with 404 — an unknown ID must never create a row.
    CustomerEntity entity = findCustomer(customerId);
    // 2. Apply the editable fields. Status is the lifecycle lever: this is where Ravi
    //    moves PROSPECT -> ACTIVE when onboarding completes.
    entity.setFullName(request.fullName().trim());
    entity.setStatus(request.status());
    // 3. Persist and return the stored shape.
    return toResponse(customerRepository.save(entity));
  }

  /** Closes the customer (see class note) while keeping the row and its interaction history. */
  @Transactional
  public void delete(String customerId) {
    CustomerEntity entity = findCustomer(customerId);
    entity.setStatus("CLOSED");
    customerRepository.save(entity);
  }

  private CustomerEntity findCustomer(String customerId) {
    return customerRepository
        .findByPublicId(customerId)
        .orElseThrow(() -> new UnknownCustomerException(customerId));
  }

  private static CustomerResponse toResponse(CustomerEntity entity) {
    return new CustomerResponse(
        entity.getPublicId(), entity.getFullName(), entity.getStatus(), entity.getCreatedAt());
  }
}

package com.northstar.crm.api;

import com.northstar.crm.service.CustomerService;
import com.northstar.crm.domain.CustomerEntity;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/customers")
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @GetMapping
  public List<CustomerResponse> list() {
  return customerService.listCustomers().stream()
          .map(c -> new CustomerResponse(c.getPublicId(), c.getFullName(), c.getStatus()))
                  .toList();
  }

  @PostMapping
  public ResponseEntity<CustomerResponse> create(@RequestBody CustomerService.CustomerRequest request) {
    CustomerEntity customer = customerService.createCustomer(request);
    CustomerResponse response = new CustomerResponse(customer.getPublicId(), customer.getFullName(), customer.getStatus());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{publicId}")
  public CustomerResponse update(
          @PathVariable String publicId,
          @RequestBody CustomerService.CustomerRequest request) {
        CustomerEntity updatedCustomer = customerService.updateCustomer(publicId, request);
        return new CustomerResponse(updatedCustomer.getPublicId(), updatedCustomer.getFullName(), updatedCustomer.getStatus());
    }

public record CustomerResponse(String customerId, String fullName, String status) {}
}
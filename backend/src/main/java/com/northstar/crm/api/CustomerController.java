package com.northstar.crm.api;

import com.northstar.crm.api.dto.CreateCustomerRequest;
import com.northstar.crm.api.dto.CustomerResponse;
import com.northstar.crm.api.dto.UpdateCustomerRequest;
import com.northstar.crm.service.CustomerService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @GetMapping
  public List<CustomerResponse> search(@RequestParam(required = false) String q) {
    return customerService.search(q);
  }

  @PostMapping
  public ResponseEntity<CustomerResponse> create(
      @Valid @RequestBody CreateCustomerRequest request) {
    CustomerResponse body = customerService.create(request);
    URI location = URI.create("/api/v1/customers/" + body.customerId());
    return ResponseEntity.created(location).body(body);
  }

  @GetMapping("/{customerId}")
  public CustomerResponse get(@PathVariable String customerId) {
    return customerService.get(customerId);
  }

  @PutMapping("/{customerId}")
  public CustomerResponse update(
      @PathVariable String customerId, @Valid @RequestBody UpdateCustomerRequest request) {
    return customerService.update(customerId, request);
  }

  @DeleteMapping("/{customerId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String customerId) {
    customerService.delete(customerId);
  }
}

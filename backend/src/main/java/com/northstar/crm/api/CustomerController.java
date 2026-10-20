package com.northstar.crm.api;

import com.northstar.crm.api.dto.CustomerResponse;
import com.northstar.crm.api.dto.SaveCustomerRequest;
import com.northstar.crm.service.CustomerService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@CrossOrigin(
    origins = "http://localhost:4200",
    allowedHeaders = {"Authorization", "Content-Type"})
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @GetMapping
  public List<CustomerResponse> list() {
    return customerService.list();
  }

  @GetMapping("/{publicId}")
  public CustomerResponse get(@PathVariable String publicId) {
    return customerService.get(publicId);
  }

  @PostMapping
  public ResponseEntity<CustomerResponse> create(@Valid @RequestBody SaveCustomerRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
  }

  @PutMapping("/{publicId}")
  public CustomerResponse update(
      @PathVariable String publicId, @Valid @RequestBody SaveCustomerRequest request) {
    return customerService.update(publicId, request);
  }
}

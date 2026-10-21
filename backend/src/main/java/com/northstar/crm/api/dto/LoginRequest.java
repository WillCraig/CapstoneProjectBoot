package com.northstar.crm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Size(max = 64) String username, @NotBlank @Size(max = 128) String password) {

  /** Never print the password, even in a debug log of the request object. */
  @Override
  public String toString() {
    return "LoginRequest[username=" + username + ", password=***]";
  }
}

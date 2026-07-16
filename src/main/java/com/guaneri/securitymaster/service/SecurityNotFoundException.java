package com.guaneri.securitymaster.service;

import java.util.UUID;

public class SecurityNotFoundException extends RuntimeException {

  public SecurityNotFoundException(UUID id) {
    super("No security found with internal id " + id);
  }

  public SecurityNotFoundException(String message) {
    super(message);
  }
}

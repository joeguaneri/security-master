package com.guaneri.securitymaster.service;

public class DuplicateIdentifierException extends RuntimeException {

  public DuplicateIdentifierException(String message) {
    super(message);
  }
}

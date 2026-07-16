package com.guaneri.securitymaster.security;

public class JwtValidationException extends RuntimeException {

  public JwtValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}

package com.guaneri.securitymaster.web;

import com.guaneri.securitymaster.service.DuplicateIdentifierException;
import com.guaneri.securitymaster.service.InvalidSecurityFieldsException;
import com.guaneri.securitymaster.service.OptimisticLockException;
import com.guaneri.securitymaster.service.SecurityNotFoundException;
import com.guaneri.securitymaster.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(SecurityNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleNotFound(SecurityNotFoundException e) {
    return new ErrorResponse(e.getMessage());
  }

  @ExceptionHandler({DuplicateIdentifierException.class, OptimisticLockException.class})
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse handleConflict(RuntimeException e) {
    return new ErrorResponse(e.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, InvalidSecurityFieldsException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleBadRequest(Exception e) {
    return new ErrorResponse(e.getMessage());
  }

  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ErrorResponse handleAuthentication(AuthenticationException e) {
    return new ErrorResponse("Invalid username or password");
  }
}

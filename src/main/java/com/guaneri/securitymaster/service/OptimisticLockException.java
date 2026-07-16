package com.guaneri.securitymaster.service;

import java.util.UUID;

public class OptimisticLockException extends RuntimeException {

  public OptimisticLockException(UUID id, long expectedVersion, long actualVersion) {
    super("Security " + id + " expected version " + expectedVersion + " but was " + actualVersion);
  }
}

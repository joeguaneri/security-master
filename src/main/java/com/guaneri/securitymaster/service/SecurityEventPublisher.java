package com.guaneri.securitymaster.service;

import com.guaneri.securitymaster.domain.Security;

public interface SecurityEventPublisher {

  void publish(Security security, SecurityChangeType changeType);
}

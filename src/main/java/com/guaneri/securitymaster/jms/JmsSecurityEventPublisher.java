package com.guaneri.securitymaster.jms;

import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.service.SecurityChangeType;
import com.guaneri.securitymaster.service.SecurityEventPublisher;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class JmsSecurityEventPublisher implements SecurityEventPublisher {

  private final JmsTemplate topicJmsTemplate;

  public JmsSecurityEventPublisher(@Qualifier("topicJmsTemplate") JmsTemplate topicJmsTemplate) {
    this.topicJmsTemplate = topicJmsTemplate;
  }

  @Override
  public void publish(Security security, SecurityChangeType changeType) {
    SecurityChangedEvent event =
        new SecurityChangedEvent(
            security.id(), security.paceSecId(), security.sedol(), security.cusip(), security.ticker(),
            changeType.name(), Instant.now());
    topicJmsTemplate.convertAndSend("securities.events", event);
  }
}

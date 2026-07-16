package com.guaneri.securitymaster.jms;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class JmsFeedPublisher implements FeedPublisher {

  private final JmsTemplate jmsTemplate;

  public JmsFeedPublisher(JmsTemplate jmsTemplate) {
    this.jmsTemplate = jmsTemplate;
  }

  @Override
  public void publish(SecurityFeedMessage message) {
    jmsTemplate.convertAndSend("securities.feed.inbound", message);
  }
}

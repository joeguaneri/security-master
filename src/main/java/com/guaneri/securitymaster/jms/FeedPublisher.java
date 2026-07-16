package com.guaneri.securitymaster.jms;

public interface FeedPublisher {

  void publish(SecurityFeedMessage message);
}

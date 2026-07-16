package com.guaneri.securitymaster.jms;

import com.guaneri.securitymaster.service.SecurityMasterService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class SecurityFeedListener {

  private final SecurityMasterService securityMasterService;

  public SecurityFeedListener(SecurityMasterService securityMasterService) {
    this.securityMasterService = securityMasterService;
  }

  @JmsListener(destination = "securities.feed.inbound")
  public void onMessage(SecurityFeedMessage message) {
    securityMasterService.upsertFromFeed(message.toDomain());
  }
}

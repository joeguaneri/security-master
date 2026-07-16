package com.guaneri.securitymaster.web;

import com.guaneri.securitymaster.jms.FeedPublisher;
import com.guaneri.securitymaster.jms.SecurityFeedMessage;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feed")
public class AdminFeedController {

  private final FeedPublisher feedPublisher;

  public AdminFeedController(FeedPublisher feedPublisher) {
    this.feedPublisher = feedPublisher;
  }

  @PostMapping("/simulate")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void simulate(@Valid @RequestBody SecurityFeedMessage message) {
    feedPublisher.publish(message);
  }
}

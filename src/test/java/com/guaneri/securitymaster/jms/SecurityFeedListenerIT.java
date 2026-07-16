package com.guaneri.securitymaster.jms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import com.guaneri.securitymaster.service.SecurityChangeType;
import com.guaneri.securitymaster.service.SecurityMasterService;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = SecurityFeedListenerIT.JmsTestContext.class)
@TestPropertySource(
    properties = {
      "spring.artemis.mode=embedded",
      "spring.artemis.embedded.enabled=true",
      "spring.artemis.embedded.persistent=false",
      "spring.artemis.embedded.queues=securities.feed.inbound",
      "spring.artemis.embedded.topics=securities.events"
    })
class SecurityFeedListenerIT {

  @Autowired private JmsTemplate jmsTemplate;

  @Autowired
  @Qualifier("topicJmsTemplate")
  private JmsTemplate topicJmsTemplate;

  @Autowired private JmsSecurityEventPublisher jmsSecurityEventPublisher;
  @Autowired private TestEventCollector testEventCollector;

  @MockBean private SecurityMasterService securityMasterService;

  @Test
  void inboundFeedMessageInvokesUpsertOnMasterService() {
    SecurityFeedMessage message = new SecurityFeedMessage(
        null, null, null, "EXMP", "Example Corp", null, AssetType.EQUITY, "USD", null, null,
        null, null, null, null, null, null, null, null, null, null, null);

    jmsTemplate.convertAndSend("securities.feed.inbound", message);

    verify(securityMasterService, timeout(5000))
        .upsertFromFeed(argThat(security -> "EXMP".equals(security.ticker()) && security.assetType() == AssetType.EQUITY));
  }

  @Test
  void publishedSecurityChangedEventLandsOnOutboundTopic() throws InterruptedException {
    UUID id = UUID.randomUUID();
    Security security = Security.builder()
        .id(id)
        .name("Example Corp")
        .currency("USD")
        .status(SecurityStatus.ACTIVE)
        .assetType(AssetType.EQUITY)
        .ticker("EXMP")
        .build();

    jmsSecurityEventPublisher.publish(security, SecurityChangeType.CREATED);

    SecurityChangedEvent received = testEventCollector.queue.poll(5, TimeUnit.SECONDS);

    assertThat(received).isNotNull();
    assertThat(received.internalId()).isEqualTo(id);
    assertThat(received.ticker()).isEqualTo("EXMP");
    assertThat(received.changeType()).isEqualTo("CREATED");
  }

  @Component
  static class TestEventCollector {
    final BlockingQueue<SecurityChangedEvent> queue = new LinkedBlockingQueue<>();

    @JmsListener(destination = "securities.events", containerFactory = "topicJmsListenerContainerFactory")
    void onEvent(SecurityChangedEvent event) {
      queue.add(event);
    }
  }

  @Configuration
  @ImportAutoConfiguration({ArtemisAutoConfiguration.class, JmsAutoConfiguration.class, JacksonAutoConfiguration.class})
  @Import({JmsConfig.class, SecurityFeedListener.class, JmsSecurityEventPublisher.class, TestEventCollector.class})
  static class JmsTestContext {

    @Bean
    DefaultJmsListenerContainerFactory topicJmsListenerContainerFactory(
        jakarta.jms.ConnectionFactory connectionFactory, MessageConverter messageConverter) {
      DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
      factory.setConnectionFactory(connectionFactory);
      factory.setMessageConverter(messageConverter);
      factory.setPubSubDomain(true);
      return factory;
    }
  }
}

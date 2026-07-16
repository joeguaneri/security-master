package com.guaneri.securitymaster.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
@EnableJms
public class JmsConfig {

  @Bean
  public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    // Reuse Spring Boot's auto-configured ObjectMapper (has the JavaTimeModule registered) rather
    // than a bare `new ObjectMapper()`, which can't (de)serialize java.time.Instant on its own.
    converter.setObjectMapper(objectMapper);
    converter.setTargetType(MessageType.TEXT);
    converter.setTypeIdPropertyName("_type");
    converter.setTypeIdMappings(
        Map.of(
            "securityFeedMessage", SecurityFeedMessage.class,
            "securityChangedEvent", SecurityChangedEvent.class));
    return converter;
  }

  @Bean
  public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonJmsMessageConverter) {
    JmsTemplate template = new JmsTemplate(connectionFactory);
    template.setMessageConverter(jacksonJmsMessageConverter);
    return template;
  }

  @Bean("topicJmsTemplate")
  public JmsTemplate topicJmsTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonJmsMessageConverter) {
    JmsTemplate template = new JmsTemplate(connectionFactory);
    template.setMessageConverter(jacksonJmsMessageConverter);
    template.setPubSubDomain(true);
    return template;
  }

  @Bean
  public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
      ConnectionFactory connectionFactory, MessageConverter jacksonJmsMessageConverter) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jacksonJmsMessageConverter);
    factory.setConcurrency("3-10");
    return factory;
  }
}

package com.guaneri.securitymaster.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guaneri.securitymaster.jms.FeedPublisher;
import com.guaneri.securitymaster.jms.SecurityFeedMessage;
import com.guaneri.securitymaster.security.JwtService;
import com.guaneri.securitymaster.security.SecurityConfig;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminFeedController.class)
@Import({SecurityConfig.class, AdminFeedControllerTest.TestConfig.class})
class AdminFeedControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private FeedPublisher feedPublisher;

  private static final String VALID_FEED_MESSAGE_JSON =
      """
      {"assetType":"EQUITY","name":"Example Corp","currency":"USD","ticker":"EXMP"}""";

  @Test
  void simulateWithoutAuthenticationReturns401() throws Exception {
    mockMvc
        .perform(post("/api/admin/feed/simulate").contentType(MediaType.APPLICATION_JSON).content(VALID_FEED_MESSAGE_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(authorities = "ROLE_WRITE")
  void simulateAsNonAdminReturns403() throws Exception {
    mockMvc
        .perform(post("/api/admin/feed/simulate").contentType(MediaType.APPLICATION_JSON).content(VALID_FEED_MESSAGE_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void simulateAsAdminPublishesMessageAndReturns202() throws Exception {
    mockMvc
        .perform(post("/api/admin/feed/simulate").contentType(MediaType.APPLICATION_JSON).content(VALID_FEED_MESSAGE_JSON))
        .andExpect(status().isAccepted());

    verify(feedPublisher).publish(any(SecurityFeedMessage.class));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void simulateWithMissingRequiredFieldReturns400() throws Exception {
    mockMvc
        .perform(post("/api/admin/feed/simulate").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    JwtService jwtService() {
      return new JwtService("web-mvc-test-secret-of-sufficient-length-000000", Duration.ofMinutes(60), Clock.systemUTC());
    }
  }
}

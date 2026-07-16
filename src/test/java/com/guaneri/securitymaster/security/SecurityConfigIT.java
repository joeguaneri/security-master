package com.guaneri.securitymaster.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SecurityConfigProbeController.class)
@Import({SecurityConfig.class, SecurityConfigIT.TestConfig.class})
class SecurityConfigIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;

  @Test
  void permitAllPathIsAccessibleWithoutToken() throws Exception {
    mockMvc.perform(get("/api/auth/public-probe")).andExpect(status().isOk());
  }

  @Test
  void authenticatedPathRejectsRequestsWithoutTokenAs401() throws Exception {
    mockMvc.perform(get("/api/probe/authenticated")).andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedPathAcceptsValidToken() throws Exception {
    String token = jwtService.issueToken("alice", Set.of("ROLE_READ"));

    mockMvc
        .perform(get("/api/probe/authenticated").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void adminOnlyPathRejectsAuthenticatedNonAdminUserAs403() throws Exception {
    String token = jwtService.issueToken("bob", Set.of("ROLE_READ"));

    mockMvc.perform(get("/api/probe/admin-only").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
  }

  @Test
  void adminOnlyPathAcceptsAdminRole() throws Exception {
    String token = jwtService.issueToken("carol", Set.of("ROLE_ADMIN"));

    mockMvc.perform(get("/api/probe/admin-only").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    JwtService jwtService() {
      return new JwtService("web-mvc-test-secret-of-sufficient-length-000000", Duration.ofMinutes(60), Clock.systemUTC());
    }
  }
}

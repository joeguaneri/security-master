package com.guaneri.securitymaster.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guaneri.securitymaster.domain.AppUser;
import com.guaneri.securitymaster.repository.AppUserRepository;
import com.guaneri.securitymaster.security.JwtService;
import com.guaneri.securitymaster.security.SecurityConfig;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, AuthControllerTest.TestConfig.class})
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockBean private AppUserRepository appUserRepository;

  private AppUser userWithPassword(String username, String rawPassword, Set<String> roles, boolean enabled) {
    return new AppUser(UUID.randomUUID(), username, passwordEncoder.encode(rawPassword), roles, enabled);
  }

  @Test
  void loginWithValidCredentialsReturns200WithToken() throws Exception {
    when(appUserRepository.findByUsername("admin"))
        .thenReturn(Optional.of(userWithPassword("admin", "admin", Set.of("ROLE_READ", "ROLE_WRITE", "ROLE_ADMIN"), true)));

    mockMvc
        .perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"admin","password":"admin"}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
  }

  @Test
  void loginWithWrongPasswordReturns401() throws Exception {
    when(appUserRepository.findByUsername("admin"))
        .thenReturn(Optional.of(userWithPassword("admin", "admin", Set.of("ROLE_READ"), true)));

    mockMvc
        .perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"admin","password":"wrong"}"""))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithUnknownUsernameReturns401() throws Exception {
    when(appUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"ghost","password":"whatever"}"""))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithDisabledUserReturns401() throws Exception {
    when(appUserRepository.findByUsername("disabled"))
        .thenReturn(Optional.of(userWithPassword("disabled", "secret", Set.of("ROLE_READ"), false)));

    mockMvc
        .perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"disabled","password":"secret"}"""))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithMissingFieldsReturns400() throws Exception {
    mockMvc
        .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
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

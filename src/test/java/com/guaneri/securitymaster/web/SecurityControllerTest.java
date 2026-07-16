package com.guaneri.securitymaster.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.IdentifierType;
import com.guaneri.securitymaster.domain.PagedResult;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import com.guaneri.securitymaster.security.JwtService;
import com.guaneri.securitymaster.security.SecurityConfig;
import com.guaneri.securitymaster.service.OptimisticLockException;
import com.guaneri.securitymaster.service.SecurityMasterService;
import com.guaneri.securitymaster.service.SecurityNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
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

@WebMvcTest(controllers = SecurityController.class)
@Import({SecurityConfig.class, SecurityControllerTest.TestConfig.class})
class SecurityControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private SecurityMasterService securityMasterService;

  private static Security sampleEquity(UUID id) {
    return Security.builder()
        .id(id)
        .name("Example Corp")
        .currency("USD")
        .status(SecurityStatus.ACTIVE)
        .assetType(AssetType.EQUITY)
        .ticker("EXMP")
        .version(0)
        .build();
  }

  @Test
  void createWithoutAuthenticationReturns401() throws Exception {
    mockMvc
        .perform(post("/api/securities").contentType(MediaType.APPLICATION_JSON).content("""
            {"assetType":"EQUITY","name":"Example Corp","currency":"USD","ticker":"EXMP"}"""))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(authorities = "ROLE_READ")
  void createAsReadOnlyUserReturns403() throws Exception {
    mockMvc
        .perform(post("/api/securities").contentType(MediaType.APPLICATION_JSON).content("""
            {"assetType":"EQUITY","name":"Example Corp","currency":"USD","ticker":"EXMP"}"""))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "ROLE_WRITE")
  void createAsWriteUserReturns201WithPersistedBody() throws Exception {
    UUID id = UUID.randomUUID();
    when(securityMasterService.create(any())).thenReturn(sampleEquity(id));

    mockMvc
        .perform(post("/api/securities").contentType(MediaType.APPLICATION_JSON).content("""
            {"assetType":"EQUITY","name":"Example Corp","currency":"USD","ticker":"EXMP"}"""))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.ticker").value("EXMP"))
        .andExpect(jsonPath("$.assetType").value("EQUITY"));
  }

  @Test
  @WithMockUser(authorities = "ROLE_WRITE")
  void createWithMissingRequiredFieldReturns400() throws Exception {
    mockMvc
        .perform(post("/api/securities").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void updateReturns200WithUpdatedBody() throws Exception {
    UUID id = UUID.randomUUID();
    Security updated = sampleEquity(id).toBuilder().name("Renamed Corp").version(1).build();
    when(securityMasterService.update(eq(id), any())).thenReturn(updated);

    mockMvc
        .perform(
            put("/api/securities/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"assetType":"EQUITY","name":"Renamed Corp","currency":"USD","ticker":"EXMP","status":"ACTIVE","version":0}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Renamed Corp"))
        .andExpect(jsonPath("$.version").value(1));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void updateReturns409WhenVersionConflict() throws Exception {
    UUID id = UUID.randomUUID();
    when(securityMasterService.update(eq(id), any())).thenThrow(new OptimisticLockException(id, 0, 3));

    mockMvc
        .perform(
            put("/api/securities/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"assetType":"EQUITY","name":"Renamed Corp","currency":"USD","ticker":"EXMP","status":"ACTIVE","version":0}"""))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(authorities = "ROLE_READ")
  void getByIdReturns200ForAnyAuthenticatedUser() throws Exception {
    UUID id = UUID.randomUUID();
    when(securityMasterService.getById(id)).thenReturn(sampleEquity(id));

    mockMvc.perform(get("/api/securities/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
  }

  @Test
  @WithMockUser(authorities = "ROLE_READ")
  void getByIdReturns404WhenMissing() throws Exception {
    UUID id = UUID.randomUUID();
    when(securityMasterService.getById(id)).thenThrow(new SecurityNotFoundException(id));

    mockMvc.perform(get("/api/securities/{id}", id)).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(authorities = "ROLE_READ")
  void lookupWithIdTypeDelegatesToSpecificLookup() throws Exception {
    UUID id = UUID.randomUUID();
    when(securityMasterService.lookup(IdentifierType.CUSIP, "037833100")).thenReturn(sampleEquity(id));

    mockMvc
        .perform(get("/api/securities/lookup").param("idType", "CUSIP").param("value", "037833100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()));
  }

  @Test
  @WithMockUser(authorities = "ROLE_READ")
  void lookupWithoutIdTypeUsesUniversalLookup() throws Exception {
    UUID id = UUID.randomUUID();
    when(securityMasterService.lookupAny("EXMP")).thenReturn(sampleEquity(id));

    mockMvc
        .perform(get("/api/securities/lookup").param("value", "EXMP"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()));
  }

  @Test
  @WithMockUser(authorities = "ROLE_READ")
  void searchReturnsPagedResults() throws Exception {
    PagedResult<Security> page = new PagedResult<>(List.of(sampleEquity(UUID.randomUUID())), 0, 10, 1);
    when(securityMasterService.search(AssetType.EQUITY, SecurityStatus.ACTIVE, 0, 10)).thenReturn(page);

    mockMvc
        .perform(
            get("/api/securities")
                .param("assetType", "EQUITY")
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCount").value(1))
        .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void deactivateAsAdminReturns200() throws Exception {
    UUID id = UUID.randomUUID();
    Security deactivated = sampleEquity(id).toBuilder().status(SecurityStatus.INACTIVE).version(1).build();
    when(securityMasterService.deactivate(id)).thenReturn(deactivated);

    mockMvc.perform(delete("/api/securities/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  @Test
  @WithMockUser(authorities = "ROLE_WRITE")
  void deactivateAsNonAdminReturns403() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/securities/{id}", id)).andExpect(status().isForbidden());
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    JwtService jwtService() {
      return new JwtService("web-mvc-test-secret-of-sufficient-length-000000", Duration.ofMinutes(60), Clock.systemUTC());
    }
  }
}

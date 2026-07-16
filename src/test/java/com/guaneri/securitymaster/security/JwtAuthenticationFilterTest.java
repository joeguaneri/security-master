package com.guaneri.securitymaster.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtService jwtService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(jwtService);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void validBearerTokenPopulatesSecurityContext() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
    when(jwtService.parseToken("good-token")).thenReturn(new JwtPrincipal("alice", Set.of("ROLE_READ", "ROLE_WRITE")));

    filter.doFilter(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getName()).isEqualTo("alice");
    assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority))
        .containsExactlyInAnyOrder("ROLE_READ", "ROLE_WRITE");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void missingAuthorizationHeaderLeavesContextEmptyButContinuesChain() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void nonBearerAuthorizationHeaderLeavesContextEmptyButContinuesChain() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void invalidTokenLeavesContextEmptyButContinuesChain() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
    when(jwtService.parseToken("bad-token")).thenThrow(new JwtValidationException("invalid", new RuntimeException()));

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }
}

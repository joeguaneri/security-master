package com.guaneri.securitymaster.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "unit-test-secret-key-at-least-32-bytes-long!!";

  private JwtService jwtServiceWithClock(Clock clock) {
    return new JwtService(SECRET, Duration.ofMinutes(60), clock);
  }

  @Test
  void issuedTokenParsesBackToSameUsernameAndRoles() {
    JwtService jwtService = jwtServiceWithClock(Clock.systemUTC());

    String token = jwtService.issueToken("alice", Set.of("ROLE_READ", "ROLE_WRITE"));
    JwtPrincipal principal = jwtService.parseToken(token);

    assertThat(principal.username()).isEqualTo("alice");
    assertThat(principal.roles()).containsExactlyInAnyOrder("ROLE_READ", "ROLE_WRITE");
  }

  @Test
  void expiredTokenIsRejected() {
    Instant issuedAt = Instant.parse("2020-01-01T00:00:00Z");
    Clock issuingClock = Clock.fixed(issuedAt, ZoneOffset.UTC);
    JwtService issuingService = new JwtService(SECRET, Duration.ofMinutes(1), issuingClock);

    String token = issuingService.issueToken("bob", Set.of("ROLE_READ"));

    Clock laterClock = Clock.fixed(issuedAt.plus(Duration.ofMinutes(5)), ZoneOffset.UTC);
    JwtService parsingService = new JwtService(SECRET, Duration.ofMinutes(1), laterClock);

    assertThatThrownBy(() -> parsingService.parseToken(token)).isInstanceOf(JwtValidationException.class);
  }

  @Test
  void tokenSignedWithADifferentSecretIsRejected() {
    JwtService issuingService =
        new JwtService("a-completely-different-secret-of-sufficient-length!!", Duration.ofMinutes(60), Clock.systemUTC());
    String token = issuingService.issueToken("carol", Set.of("ROLE_READ"));

    JwtService parsingService = jwtServiceWithClock(Clock.systemUTC());

    assertThatThrownBy(() -> parsingService.parseToken(token))
        .isInstanceOf(JwtValidationException.class)
        .hasCauseInstanceOf(SignatureException.class);
  }

  @Test
  void malformedTokenIsRejected() {
    JwtService jwtService = jwtServiceWithClock(Clock.systemUTC());

    assertThatThrownBy(() -> jwtService.parseToken("not-a-real-jwt"))
        .isInstanceOf(JwtValidationException.class)
        .hasCauseInstanceOf(MalformedJwtException.class);
  }
}

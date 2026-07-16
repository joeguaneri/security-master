package com.guaneri.securitymaster.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey key;
  private final Duration expiration;
  private final Clock clock;

  public JwtService(String secret, Duration expiration, Clock clock) {
    // Always HS256, regardless of secret length beyond the 256-bit minimum: keeps the algorithm
    // deterministic instead of jjwt's key-length-based auto-selection (which can pick HS384/HS512
    // for a key too short for THAT stronger algorithm even though it's plenty long for HS256).
    this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    this.expiration = expiration;
    this.clock = clock;
  }

  @Autowired
  public JwtService(
      @Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
    this(secret, Duration.ofMinutes(expirationMinutes), Clock.systemUTC());
  }

  public String issueToken(String username, Set<String> roles) {
    Instant now = clock.instant();
    return Jwts.builder()
        .subject(username)
        .claim("roles", roles)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expiration)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  public Duration expiration() {
    return expiration;
  }

  public JwtPrincipal parseToken(String token) {
    try {
      Claims claims =
          Jwts.parser().clock(() -> Date.from(clock.instant())).verifyWith(key).build().parseSignedClaims(token).getPayload();

      String username = claims.getSubject();
      @SuppressWarnings("unchecked")
      List<String> roles = claims.get("roles", List.class);
      return new JwtPrincipal(username, new HashSet<>(roles));
    } catch (JwtException e) {
      throw new JwtValidationException("Invalid JWT: " + e.getMessage(), e);
    }
  }
}

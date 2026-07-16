package com.guaneri.securitymaster.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Throwaway controller used only by {@link SecurityConfigIT} to exercise the security filter
 * chain's permitAll/authenticated/role rules without depending on the real business controllers.
 * Must be a top-level class - nested static classes are not reliably picked up by
 * {@code @WebMvcTest(controllers = ...)}'s classpath scanning.
 */
@RestController
public class SecurityConfigProbeController {

  @GetMapping("/api/auth/public-probe")
  public String publicProbe() {
    return "ok";
  }

  @GetMapping("/api/probe/authenticated")
  public String authenticatedProbe() {
    return "ok";
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/api/probe/admin-only")
  public String adminOnlyProbe() {
    return "ok";
  }
}

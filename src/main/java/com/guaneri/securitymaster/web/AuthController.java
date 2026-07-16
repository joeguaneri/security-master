package com.guaneri.securitymaster.web;

import com.guaneri.securitymaster.domain.AppUser;
import com.guaneri.securitymaster.repository.AppUserRepository;
import com.guaneri.securitymaster.security.JwtService;
import com.guaneri.securitymaster.web.dto.LoginRequest;
import com.guaneri.securitymaster.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.appUserRepository = appUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    AppUser user =
        appUserRepository
            .findByUsername(request.username())
            .filter(AppUser::enabled)
            .filter(u -> passwordEncoder.matches(request.password(), u.passwordHash()))
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

    String token = jwtService.issueToken(user.username(), user.roles());
    return new LoginResponse(token, "Bearer", jwtService.expiration().toSeconds());
  }
}

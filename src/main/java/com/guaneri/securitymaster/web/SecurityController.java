package com.guaneri.securitymaster.web;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.IdentifierType;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import com.guaneri.securitymaster.service.SecurityMasterService;
import com.guaneri.securitymaster.web.dto.CreateSecurityRequest;
import com.guaneri.securitymaster.web.dto.SecurityResponse;
import com.guaneri.securitymaster.web.dto.SecuritySearchResponse;
import com.guaneri.securitymaster.web.dto.UpdateSecurityRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/securities")
public class SecurityController {

  private final SecurityMasterService service;

  public SecurityController(SecurityMasterService service) {
    this.service = service;
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('WRITE','ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public SecurityResponse create(@Valid @RequestBody CreateSecurityRequest request) {
    return SecurityResponse.from(service.create(request.toDomain()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('WRITE','ADMIN')")
  public SecurityResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSecurityRequest request) {
    return SecurityResponse.from(service.update(id, request.toDomain(id)));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public SecurityResponse getById(@PathVariable UUID id) {
    return SecurityResponse.from(service.getById(id));
  }

  @GetMapping("/lookup")
  @PreAuthorize("isAuthenticated()")
  public SecurityResponse lookup(@RequestParam(required = false) IdentifierType idType, @RequestParam String value) {
    Security result = idType == null ? service.lookupAny(value) : service.lookup(idType, value);
    return SecurityResponse.from(result);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public SecuritySearchResponse search(
      @RequestParam(required = false) AssetType assetType,
      @RequestParam(required = false) SecurityStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return SecuritySearchResponse.from(service.search(assetType, status, page, size));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public SecurityResponse deactivate(@PathVariable UUID id) {
    return SecurityResponse.from(service.deactivate(id));
  }
}

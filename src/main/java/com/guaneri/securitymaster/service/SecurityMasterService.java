package com.guaneri.securitymaster.service;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.IdentifierType;
import com.guaneri.securitymaster.domain.PagedResult;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import com.guaneri.securitymaster.repository.SecurityRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SecurityMasterService {

  private final SecurityRepository repository;
  private final SecurityEventPublisher eventPublisher;

  public SecurityMasterService(SecurityRepository repository, SecurityEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  public Security create(Security draft) {
    validate(draft);
    Security persisted = repository.insert(draft);
    eventPublisher.publish(persisted, SecurityChangeType.CREATED);
    return persisted;
  }

  public Security update(UUID id, Security draft) {
    Security toPersist = draft.id() == null ? draft.toBuilder().id(id).build() : draft;
    validate(toPersist);
    Security persisted = repository.update(toPersist);
    eventPublisher.publish(persisted, SecurityChangeType.UPDATED);
    return persisted;
  }

  public Security getById(UUID id) {
    return repository.findById(id).orElseThrow(() -> new SecurityNotFoundException(id));
  }

  public Security lookup(IdentifierType type, String value) {
    return repository
        .findByIdentifier(type, value)
        .orElseThrow(() -> new SecurityNotFoundException("No security found for " + type + "=" + value));
  }

  public Security lookupAny(String value) {
    return repository
        .findByAnyIdentifier(value)
        .orElseThrow(() -> new SecurityNotFoundException("No security found for identifier value " + value));
  }

  public PagedResult<Security> search(AssetType assetType, SecurityStatus status, int page, int size) {
    return repository.search(assetType, status, page, size);
  }

  public Security deactivate(UUID id) {
    Security existing = getById(id);
    Security toDeactivate = existing.toBuilder().status(SecurityStatus.INACTIVE).build();
    Security persisted = repository.update(toDeactivate);
    eventPublisher.publish(persisted, SecurityChangeType.DEACTIVATED);
    return persisted;
  }

  public Security upsertFromFeed(Security incoming) {
    Optional<Security> existing = findMatch(incoming);
    if (existing.isPresent()) {
      Security merged = incoming.toBuilder().id(existing.get().id()).version(existing.get().version()).build();
      validate(merged);
      Security persisted = repository.update(merged);
      eventPublisher.publish(persisted, SecurityChangeType.UPDATED);
      return persisted;
    }
    return create(incoming);
  }

  private Optional<Security> findMatch(Security incoming) {
    if (incoming.paceSecId() != null) {
      Optional<Security> match = repository.findByIdentifier(IdentifierType.PACESEC_ID, incoming.paceSecId());
      if (match.isPresent()) {
        return match;
      }
    }
    if (incoming.sedol() != null) {
      Optional<Security> match = repository.findByIdentifier(IdentifierType.SEDOL, incoming.sedol());
      if (match.isPresent()) {
        return match;
      }
    }
    if (incoming.cusip() != null) {
      Optional<Security> match = repository.findByIdentifier(IdentifierType.CUSIP, incoming.cusip());
      if (match.isPresent()) {
        return match;
      }
    }
    if (incoming.ticker() != null) {
      Optional<Security> match = repository.findByIdentifier(IdentifierType.TICKER, incoming.ticker());
      if (match.isPresent()) {
        return match;
      }
    }
    return Optional.empty();
  }

  private void validate(Security security) {
    AssetType type = security.assetType();

    if (type.name().endsWith("OPTION") && security.strikePrice() == null) {
      throw new InvalidSecurityFieldsException(type + " requires strikePrice");
    }
    if (type == AssetType.BOND && (security.couponRate() == null || security.maturityDate() == null)) {
      throw new InvalidSecurityFieldsException("BOND requires couponRate and maturityDate");
    }
    if (type.name().endsWith("SWAP") && security.notionalCurrency() == null) {
      throw new InvalidSecurityFieldsException(type + " requires notionalCurrency");
    }
    if ((type == AssetType.FXSPOT || type == AssetType.FXFORWARD)
        && (security.baseCurrency() == null || security.quoteCurrency() == null)) {
      throw new InvalidSecurityFieldsException(type + " requires baseCurrency and quoteCurrency");
    }
  }
}

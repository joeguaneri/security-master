package com.guaneri.securitymaster.repository;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.IdentifierType;
import com.guaneri.securitymaster.domain.PagedResult;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import java.util.Optional;
import java.util.UUID;

public interface SecurityRepository {

  Security insert(Security security);

  Security update(Security security);

  Optional<Security> findById(UUID id);

  Optional<Security> findByIdentifier(IdentifierType type, String value);

  Optional<Security> findByAnyIdentifier(String value);

  PagedResult<Security> search(AssetType assetType, SecurityStatus status, int page, int size);
}

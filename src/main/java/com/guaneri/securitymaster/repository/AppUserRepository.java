package com.guaneri.securitymaster.repository;

import com.guaneri.securitymaster.domain.AppUser;
import java.util.Optional;

public interface AppUserRepository {

  Optional<AppUser> findByUsername(String username);
}

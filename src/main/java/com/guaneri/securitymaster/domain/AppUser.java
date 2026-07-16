package com.guaneri.securitymaster.domain;

import java.util.Set;
import java.util.UUID;

public record AppUser(UUID id, String username, String passwordHash, Set<String> roles, boolean enabled) {}

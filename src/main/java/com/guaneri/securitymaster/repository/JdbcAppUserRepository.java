package com.guaneri.securitymaster.repository;

import com.guaneri.securitymaster.domain.AppUser;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAppUserRepository implements AppUserRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public JdbcAppUserRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<AppUser> findByUsername(String username) {
    List<AppUser> results =
        jdbc.query("SELECT * FROM app_users WHERE username = :username", Map.of("username", username), this::mapRow);
    return results.stream().findFirst();
  }

  private AppUser mapRow(ResultSet rs, int rowNum) throws SQLException {
    Set<String> roles =
        Arrays.stream(rs.getString("roles").split(",")).map(String::trim).collect(Collectors.toSet());
    return new AppUser(
        rs.getObject("id", UUID.class), rs.getString("username"), rs.getString("password_hash"), roles, rs.getBoolean("enabled"));
  }
}

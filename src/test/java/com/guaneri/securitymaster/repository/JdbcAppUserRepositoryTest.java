package com.guaneri.securitymaster.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.guaneri.securitymaster.domain.AppUser;
import java.util.Optional;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JdbcAppUserRepositoryTest {

  @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

  static DataSource dataSource;
  JdbcAppUserRepository repository;

  @BeforeAll
  static void migrateSchema() {
    DriverManagerDataSource ds = new DriverManagerDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUsername(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());
    ds.setDriverClassName(POSTGRES.getDriverClassName());
    dataSource = ds;

    Flyway.configure().dataSource(dataSource).load().migrate();
  }

  @BeforeEach
  void setUp() {
    repository = new JdbcAppUserRepository(new NamedParameterJdbcTemplate(dataSource));
  }

  @Test
  void findByUsernameReturnsSeededAdminWithExpectedRoles() {
    Optional<AppUser> admin = repository.findByUsername("admin");

    assertThat(admin).isPresent();
    assertThat(admin.get().username()).isEqualTo("admin");
    assertThat(admin.get().roles()).containsExactlyInAnyOrder("ROLE_READ", "ROLE_WRITE", "ROLE_ADMIN");
    assertThat(admin.get().enabled()).isTrue();
    assertThat(admin.get().passwordHash()).startsWith("$2");
  }

  @Test
  void findByUsernameReturnsSeededReaderWithReadOnlyRole() {
    Optional<AppUser> reader = repository.findByUsername("reader");

    assertThat(reader).isPresent();
    assertThat(reader.get().roles()).containsExactly("ROLE_READ");
  }

  @Test
  void findByUsernameReturnsEmptyWhenNotFound() {
    assertThat(repository.findByUsername("nobody")).isEmpty();
  }
}

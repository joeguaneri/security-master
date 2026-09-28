package com.guaneri.securitymaster.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.IdentifierType;
import com.guaneri.securitymaster.domain.PagedResult;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import com.guaneri.securitymaster.service.DuplicateIdentifierException;
import com.guaneri.securitymaster.service.OptimisticLockException;
import com.guaneri.securitymaster.service.SecurityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JdbcSecurityRepositoryTest {

  @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

  static DataSource dataSource;
  JdbcSecurityRepository repository;

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
    repository = new JdbcSecurityRepository(new NamedParameterJdbcTemplate(dataSource));
    new JdbcTemplate(dataSource).execute("TRUNCATE TABLE securities");
  }

  static Stream<Arguments> representativeSecurities() {
    return Stream.of(
        Arguments.of(
            "equity",
            Security.builder()
                .name("Example Corp")
                .currency("USD")
                .status(SecurityStatus.ACTIVE)
                .assetType(AssetType.EQUITY)
                .ticker("EXMP")
                .build()),
        Arguments.of(
            "bond",
            Security.builder()
                .name("Example Bond 2030")
                .currency("USD")
                .status(SecurityStatus.ACTIVE)
                .assetType(AssetType.BOND)
                .cusip("037833100")
                .couponRate(new BigDecimal("5.250000"))
                .maturityDate(LocalDate.parse("2030-06-01"))
                .parValue(new BigDecimal("1000.0000"))
                .build()),
        Arguments.of(
            "equityOption",
            Security.builder()
                .name("Example Corp Jan25 Call")
                .currency("USD")
                .status(SecurityStatus.ACTIVE)
                .assetType(AssetType.EQUITYOPTION)
                .underlyingIdentifier("EXMP")
                .strikePrice(new BigDecimal("100.000000"))
                .contractMultiplier(new BigDecimal("100.000000"))
                .expirationDate(LocalDate.parse("2025-01-17"))
                .build()),
        Arguments.of(
            "equitySwap",
            Security.builder()
                .name("Example Corp Equity Swap")
                .currency("USD")
                .status(SecurityStatus.ACTIVE)
                .assetType(AssetType.EQUITYSWAP)
                .paceSecId("PACE-EQSWAP-1")
                .ticker("EXMPSWAP")
                .underlyingIdentifier("EXMP")
                .notionalCurrency("USD")
                .build()),
        Arguments.of(
            "currencySwap",
            Security.builder()
                .name("USD/EUR Currency Swap")
                .currency("USD")
                .status(SecurityStatus.ACTIVE)
                .assetType(AssetType.CURRENCYSWAP)
                .notionalCurrency("USD")
                .maturityDate(LocalDate.parse("2028-03-01"))
                .build()),
        Arguments.of(
            "fxSpot",
            Security.builder()
                .name("EUR/USD Spot")
                .currency("USD")
                .status(SecurityStatus.ACTIVE)
                .assetType(AssetType.FXSPOT)
                .baseCurrency("EUR")
                .quoteCurrency("USD")
                .build()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("representativeSecurities")
  void insertThenFindByIdRoundTrips(String label, Security draft) {
    Security persisted = repository.insert(draft);

    assertThat(persisted.id()).isNotNull();
    assertThat(persisted.version()).isZero();

    Optional<Security> found = repository.findById(persisted.id());

    assertThat(found).contains(persisted);
  }

  @Test
  void findByIdReturnsEmptyWhenNotFound() {
    assertThat(repository.findById(UUID.randomUUID())).isEmpty();
  }

  @Test
  void insertRejectsDuplicateTicker() {
    Security first = Security.builder()
        .name("First").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.EQUITY).ticker("DUPE").build();
    Security second = Security.builder()
        .name("Second").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.EQUITY).ticker("DUPE").build();

    repository.insert(first);

    assertThatThrownBy(() -> repository.insert(second)).isInstanceOf(DuplicateIdentifierException.class);
  }

  @Test
  void insertAllowsMultipleSecuritiesWithNullTicker() {
    Security first = Security.builder()
        .name("First").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.EQUITY).cusip("111111111").build();
    Security second = Security.builder()
        .name("Second").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.EQUITY).cusip("222222222").build();

    repository.insert(first);
    Security persistedSecond = repository.insert(second);

    assertThat(persistedSecond.id()).isNotNull();
  }

  @Test
  void updateAppliesChangesWhenVersionMatches() {
    Security persisted = repository.insert(
        Security.builder().name("Original").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.EQUITY).ticker("UPD").build());

    Security updated = persisted.toBuilder().name("Renamed").build();
    Security afterUpdate = repository.update(updated);

    assertThat(afterUpdate.name()).isEqualTo("Renamed");
    assertThat(afterUpdate.version()).isEqualTo(persisted.version() + 1);
    assertThat(repository.findById(persisted.id())).contains(afterUpdate);
  }

  @Test
  void updateThrowsOptimisticLockExceptionOnVersionMismatch() {
    Security persisted = repository.insert(
        Security.builder().name("Original").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.EQUITY).ticker("OPT").build());

    Security staleUpdate = persisted.toBuilder().name("Stale Write").version(persisted.version() + 5).build();

    assertThatThrownBy(() -> repository.update(staleUpdate)).isInstanceOf(OptimisticLockException.class);
  }

  @Test
  void updateThrowsNotFoundWhenIdDoesNotExist() {
    Security phantom = Security.builder()
        .id(UUID.randomUUID())
        .name("Ghost")
        .currency("USD")
        .status(SecurityStatus.ACTIVE)
        .assetType(AssetType.EQUITY)
        .version(0)
        .build();

    assertThatThrownBy(() -> repository.update(phantom)).isInstanceOf(SecurityNotFoundException.class);
  }

  static Stream<Arguments> identifierLookups() {
    return Stream.of(
        Arguments.of(IdentifierType.PACESEC_ID, "PSEC-LOOKUP"),
        Arguments.of(IdentifierType.SEDOL, "1234567"),
        Arguments.of(IdentifierType.CUSIP, "999999999"),
        Arguments.of(IdentifierType.TICKER, "LKUP"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("identifierLookups")
  void findByIdentifierMatchesEachExternalIdentifierType(IdentifierType type, String value) {
    Security.Builder builder =
        Security.builder().name("Lookup Target").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.EQUITY);
    Security draft =
        switch (type) {
          case PACESEC_ID -> builder.paceSecId(value).build();
          case SEDOL -> builder.sedol(value).build();
          case CUSIP -> builder.cusip(value).build();
          case TICKER -> builder.ticker(value).build();
          case INTERNAL_ID -> throw new IllegalStateException("covered separately");
        };
    Security persisted = repository.insert(draft);

    assertThat(repository.findByIdentifier(type, value)).contains(persisted);
  }

  @Test
  void findByIdentifierWithInternalIdTypeLooksUpById() {
    Security persisted = repository.insert(
        Security.builder().name("Internal Lookup").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.EQUITY).build());

    assertThat(repository.findByIdentifier(IdentifierType.INTERNAL_ID, persisted.id().toString())).contains(persisted);
  }

  @Test
  void findByIdentifierReturnsEmptyWhenNoMatch() {
    assertThat(repository.findByIdentifier(IdentifierType.CUSIP, "no-such-cusip")).isEmpty();
  }

  @Test
  void findByAnyIdentifierMatchesRegardlessOfWhichColumnPopulated() {
    Security persisted = repository.insert(
        Security.builder().name("Universal").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.EQUITY).cusip("555555555").build());

    assertThat(repository.findByAnyIdentifier("555555555")).contains(persisted);
  }

  @Test
  void findByAnyIdentifierReturnsEmptyWhenNoColumnMatches() {
    assertThat(repository.findByAnyIdentifier("totally-unknown-value")).isEmpty();
  }

  @Test
  void searchFiltersByAssetTypeAndStatusWithPaging() {
    repository.insert(Security.builder().name("Active Equity 1").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.EQUITY).ticker("AE1").build());
    repository.insert(Security.builder().name("Active Equity 2").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.EQUITY).ticker("AE2").build());
    repository.insert(Security.builder().name("Inactive Equity").currency("USD").status(SecurityStatus.INACTIVE).assetType(AssetType.EQUITY).ticker("IE1").build());
    repository.insert(Security.builder().name("Active Bond").currency("USD").status(SecurityStatus.ACTIVE).assetType(AssetType.BOND)
        .cusip("444444444").couponRate(new BigDecimal("1.000000")).maturityDate(LocalDate.parse("2029-01-01")).build());

    PagedResult<Security> page = repository.search(AssetType.EQUITY, SecurityStatus.ACTIVE, 0, 1);

    assertThat(page.totalCount()).isEqualTo(2);
    assertThat(page.items()).hasSize(1);
    assertThat(page.page()).isZero();
    assertThat(page.size()).isEqualTo(1);
  }
}

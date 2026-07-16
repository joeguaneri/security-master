package com.guaneri.securitymaster.repository;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.IdentifierType;
import com.guaneri.securitymaster.domain.PagedResult;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import com.guaneri.securitymaster.service.DuplicateIdentifierException;
import com.guaneri.securitymaster.service.OptimisticLockException;
import com.guaneri.securitymaster.service.SecurityNotFoundException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSecurityRepository implements SecurityRepository {

  private static final Map<IdentifierType, String> IDENTIFIER_COLUMNS =
      Map.of(
          IdentifierType.PACESEC_ID, "pacesec_id",
          IdentifierType.SEDOL, "sedol",
          IdentifierType.CUSIP, "cusip",
          IdentifierType.TICKER, "ticker");

  private final NamedParameterJdbcTemplate jdbc;
  private final SecurityRowMapper rowMapper = new SecurityRowMapper();

  public JdbcSecurityRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Security insert(Security security) {
    UUID id = security.id() != null ? security.id() : UUID.randomUUID();
    Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

    MapSqlParameterSource params = commonParams(security);
    params.addValue("id", id);
    params.addValue("createdAt", OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
    params.addValue("updatedAt", OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
    params.addValue("version", 0L);

    String sql =
        """
        INSERT INTO securities (
          id, pacesec_id, sedol, cusip, ticker, name, issuer, asset_type, currency, country_of_risk,
          exchange_mic, status, maturity_date, expiration_date, coupon_rate, par_value, strike_price,
          contract_multiplier, underlying_identifier, notional_currency, base_currency, quote_currency,
          unit_of_measure, created_at, updated_at, version)
        VALUES (
          :id, :paceSecId, :sedol, :cusip, :ticker, :name, :issuer, :assetType, :currency, :countryOfRisk,
          :exchangeMic, :status, :maturityDate, :expirationDate, :couponRate, :parValue, :strikePrice,
          :contractMultiplier, :underlyingIdentifier, :notionalCurrency, :baseCurrency, :quoteCurrency,
          :unitOfMeasure, :createdAt, :updatedAt, :version)
        RETURNING *
        """;

    try {
      return jdbc.query(sql, params, rowMapper).get(0);
    } catch (DuplicateKeyException e) {
      throw new DuplicateIdentifierException(e.getMostSpecificCause().getMessage());
    }
  }

  @Override
  public Security update(Security security) {
    MapSqlParameterSource params = commonParams(security);
    params.addValue("id", security.id());
    params.addValue("updatedAt", OffsetDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.MICROS), ZoneOffset.UTC));
    params.addValue("expectedVersion", security.version());

    String sql =
        """
        UPDATE securities SET
          pacesec_id = :paceSecId, sedol = :sedol, cusip = :cusip, ticker = :ticker,
          name = :name, issuer = :issuer, asset_type = :assetType, currency = :currency,
          country_of_risk = :countryOfRisk, exchange_mic = :exchangeMic, status = :status,
          maturity_date = :maturityDate, expiration_date = :expirationDate, coupon_rate = :couponRate,
          par_value = :parValue, strike_price = :strikePrice, contract_multiplier = :contractMultiplier,
          underlying_identifier = :underlyingIdentifier, notional_currency = :notionalCurrency,
          base_currency = :baseCurrency, quote_currency = :quoteCurrency, unit_of_measure = :unitOfMeasure,
          updated_at = :updatedAt, version = version + 1
        WHERE id = :id AND version = :expectedVersion
        RETURNING *
        """;

    List<Security> updated;
    try {
      updated = jdbc.query(sql, params, rowMapper);
    } catch (DuplicateKeyException e) {
      throw new DuplicateIdentifierException(e.getMostSpecificCause().getMessage());
    }

    if (!updated.isEmpty()) {
      return updated.get(0);
    }

    Integer exists =
        jdbc.queryForObject("SELECT COUNT(*) FROM securities WHERE id = :id", Map.of("id", security.id()), Integer.class);
    if (exists == null || exists == 0) {
      throw new SecurityNotFoundException(security.id());
    }

    Long actualVersion =
        jdbc.queryForObject("SELECT version FROM securities WHERE id = :id", Map.of("id", security.id()), Long.class);
    throw new OptimisticLockException(security.id(), security.version(), actualVersion);
  }

  @Override
  public Optional<Security> findById(UUID id) {
    List<Security> results = jdbc.query("SELECT * FROM securities WHERE id = :id", Map.of("id", id), rowMapper);
    return results.stream().findFirst();
  }

  @Override
  public Optional<Security> findByIdentifier(IdentifierType type, String value) {
    if (type == IdentifierType.INTERNAL_ID) {
      return findById(UUID.fromString(value));
    }
    String column = IDENTIFIER_COLUMNS.get(type);
    List<Security> results =
        jdbc.query("SELECT * FROM securities WHERE " + column + " = :value", Map.of("value", value), rowMapper);
    return results.stream().findFirst();
  }

  @Override
  public Optional<Security> findByAnyIdentifier(String value) {
    String sql = "SELECT * FROM securities WHERE pacesec_id = :value OR sedol = :value OR cusip = :value OR ticker = :value";
    List<Security> results = jdbc.query(sql, Map.of("value", value), rowMapper);
    return results.stream().findFirst();
  }

  @Override
  public PagedResult<Security> search(AssetType assetType, SecurityStatus status, int page, int size) {
    StringBuilder where = new StringBuilder(" WHERE 1 = 1");
    MapSqlParameterSource params = new MapSqlParameterSource();
    if (assetType != null) {
      where.append(" AND asset_type = :assetType");
      params.addValue("assetType", assetType.name());
    }
    if (status != null) {
      where.append(" AND status = :status");
      params.addValue("status", status.name());
    }

    Long totalCount = jdbc.queryForObject("SELECT COUNT(*) FROM securities" + where, params, Long.class);

    params.addValue("limit", size);
    params.addValue("offset", page * size);
    List<Security> items =
        jdbc.query("SELECT * FROM securities" + where + " ORDER BY name, id LIMIT :limit OFFSET :offset", params, rowMapper);

    return new PagedResult<>(items, page, size, totalCount == null ? 0 : totalCount);
  }

  private MapSqlParameterSource commonParams(Security s) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("paceSecId", s.paceSecId());
    params.addValue("sedol", s.sedol());
    params.addValue("cusip", s.cusip());
    params.addValue("ticker", s.ticker());
    params.addValue("name", s.name());
    params.addValue("issuer", s.issuer());
    params.addValue("assetType", s.assetType().name());
    params.addValue("currency", s.currency());
    params.addValue("countryOfRisk", s.countryOfRisk());
    params.addValue("exchangeMic", s.exchangeMic());
    params.addValue("status", s.status().name());
    params.addValue("maturityDate", s.maturityDate());
    params.addValue("expirationDate", s.expirationDate());
    params.addValue("couponRate", s.couponRate());
    params.addValue("parValue", s.parValue());
    params.addValue("strikePrice", s.strikePrice());
    params.addValue("contractMultiplier", s.contractMultiplier());
    params.addValue("underlyingIdentifier", s.underlyingIdentifier());
    params.addValue("notionalCurrency", s.notionalCurrency());
    params.addValue("baseCurrency", s.baseCurrency());
    params.addValue("quoteCurrency", s.quoteCurrency());
    params.addValue("unitOfMeasure", s.unitOfMeasure());
    return params;
  }
}

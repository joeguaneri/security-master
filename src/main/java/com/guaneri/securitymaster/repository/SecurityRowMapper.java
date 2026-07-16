package com.guaneri.securitymaster.repository;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

class SecurityRowMapper implements RowMapper<Security> {

  @Override
  public Security mapRow(ResultSet rs, int rowNum) throws SQLException {
    return Security.builder()
        .id(rs.getObject("id", UUID.class))
        .paceSecId(rs.getString("pacesec_id"))
        .sedol(rs.getString("sedol"))
        .cusip(rs.getString("cusip"))
        .ticker(rs.getString("ticker"))
        .name(rs.getString("name"))
        .issuer(rs.getString("issuer"))
        .assetType(AssetType.valueOf(rs.getString("asset_type")))
        .currency(rs.getString("currency"))
        .countryOfRisk(rs.getString("country_of_risk"))
        .exchangeMic(rs.getString("exchange_mic"))
        .status(SecurityStatus.valueOf(rs.getString("status")))
        .maturityDate(rs.getObject("maturity_date", LocalDate.class))
        .expirationDate(rs.getObject("expiration_date", LocalDate.class))
        .couponRate(rs.getBigDecimal("coupon_rate"))
        .parValue(rs.getBigDecimal("par_value"))
        .strikePrice(rs.getBigDecimal("strike_price"))
        .contractMultiplier(rs.getBigDecimal("contract_multiplier"))
        .underlyingIdentifier(rs.getString("underlying_identifier"))
        .notionalCurrency(rs.getString("notional_currency"))
        .baseCurrency(rs.getString("base_currency"))
        .quoteCurrency(rs.getString("quote_currency"))
        .unitOfMeasure(rs.getString("unit_of_measure"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getLong("version"))
        .build();
  }
}

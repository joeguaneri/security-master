package com.guaneri.securitymaster.web.dto;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SecurityResponse(
    UUID id,
    String paceSecId,
    String sedol,
    String cusip,
    String ticker,
    String name,
    String issuer,
    AssetType assetType,
    String currency,
    String countryOfRisk,
    String exchangeMic,
    SecurityStatus status,
    LocalDate maturityDate,
    LocalDate expirationDate,
    BigDecimal couponRate,
    BigDecimal parValue,
    BigDecimal strikePrice,
    BigDecimal contractMultiplier,
    String underlyingIdentifier,
    String notionalCurrency,
    String baseCurrency,
    String quoteCurrency,
    String unitOfMeasure,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public static SecurityResponse from(Security s) {
    return new SecurityResponse(
        s.id(), s.paceSecId(), s.sedol(), s.cusip(), s.ticker(), s.name(), s.issuer(), s.assetType(), s.currency(),
        s.countryOfRisk(), s.exchangeMic(), s.status(), s.maturityDate(), s.expirationDate(), s.couponRate(),
        s.parValue(), s.strikePrice(), s.contractMultiplier(), s.underlyingIdentifier(), s.notionalCurrency(),
        s.baseCurrency(), s.quoteCurrency(), s.unitOfMeasure(), s.createdAt(), s.updatedAt(), s.version());
  }
}

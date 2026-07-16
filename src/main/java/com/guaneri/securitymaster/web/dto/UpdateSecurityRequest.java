package com.guaneri.securitymaster.web.dto;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateSecurityRequest(
    @NotNull AssetType assetType,
    String paceSecId,
    String sedol,
    String cusip,
    String ticker,
    @NotBlank String name,
    String issuer,
    @NotBlank String currency,
    String countryOfRisk,
    String exchangeMic,
    @NotNull SecurityStatus status,
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
    long version) {

  public Security toDomain(UUID id) {
    return Security.builder()
        .id(id)
        .assetType(assetType)
        .paceSecId(paceSecId)
        .sedol(sedol)
        .cusip(cusip)
        .ticker(ticker)
        .name(name)
        .issuer(issuer)
        .currency(currency)
        .countryOfRisk(countryOfRisk)
        .exchangeMic(exchangeMic)
        .status(status)
        .maturityDate(maturityDate)
        .expirationDate(expirationDate)
        .couponRate(couponRate)
        .parValue(parValue)
        .strikePrice(strikePrice)
        .contractMultiplier(contractMultiplier)
        .underlyingIdentifier(underlyingIdentifier)
        .notionalCurrency(notionalCurrency)
        .baseCurrency(baseCurrency)
        .quoteCurrency(quoteCurrency)
        .unitOfMeasure(unitOfMeasure)
        .version(version)
        .build();
  }
}

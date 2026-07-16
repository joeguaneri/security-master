package com.guaneri.securitymaster.jms;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SecurityFeedMessage(
    String paceSecId,
    String sedol,
    String cusip,
    String ticker,
    @NotBlank String name,
    String issuer,
    @NotNull AssetType assetType,
    @NotBlank String currency,
    String countryOfRisk,
    String exchangeMic,
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
    String unitOfMeasure) {

  public Security toDomain() {
    return Security.builder()
        .paceSecId(paceSecId)
        .sedol(sedol)
        .cusip(cusip)
        .ticker(ticker)
        .name(name)
        .issuer(issuer)
        .assetType(assetType)
        .currency(currency)
        .countryOfRisk(countryOfRisk)
        .exchangeMic(exchangeMic)
        .status(SecurityStatus.ACTIVE)
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
        .build();
  }
}

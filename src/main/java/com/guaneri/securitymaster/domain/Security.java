package com.guaneri.securitymaster.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Security(
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

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder()
        .id(id)
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
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .version(version);
  }

  public static final class Builder {
    private UUID id;
    private String paceSecId;
    private String sedol;
    private String cusip;
    private String ticker;
    private String name;
    private String issuer;
    private AssetType assetType;
    private String currency;
    private String countryOfRisk;
    private String exchangeMic;
    private SecurityStatus status;
    private LocalDate maturityDate;
    private LocalDate expirationDate;
    private BigDecimal couponRate;
    private BigDecimal parValue;
    private BigDecimal strikePrice;
    private BigDecimal contractMultiplier;
    private String underlyingIdentifier;
    private String notionalCurrency;
    private String baseCurrency;
    private String quoteCurrency;
    private String unitOfMeasure;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public Builder id(UUID id) {
      this.id = id;
      return this;
    }

    public Builder paceSecId(String paceSecId) {
      this.paceSecId = paceSecId;
      return this;
    }

    public Builder sedol(String sedol) {
      this.sedol = sedol;
      return this;
    }

    public Builder cusip(String cusip) {
      this.cusip = cusip;
      return this;
    }

    public Builder ticker(String ticker) {
      this.ticker = ticker;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder issuer(String issuer) {
      this.issuer = issuer;
      return this;
    }

    public Builder assetType(AssetType assetType) {
      this.assetType = assetType;
      return this;
    }

    public Builder currency(String currency) {
      this.currency = currency;
      return this;
    }

    public Builder countryOfRisk(String countryOfRisk) {
      this.countryOfRisk = countryOfRisk;
      return this;
    }

    public Builder exchangeMic(String exchangeMic) {
      this.exchangeMic = exchangeMic;
      return this;
    }

    public Builder status(SecurityStatus status) {
      this.status = status;
      return this;
    }

    public Builder maturityDate(LocalDate maturityDate) {
      this.maturityDate = maturityDate;
      return this;
    }

    public Builder expirationDate(LocalDate expirationDate) {
      this.expirationDate = expirationDate;
      return this;
    }

    public Builder couponRate(BigDecimal couponRate) {
      this.couponRate = couponRate;
      return this;
    }

    public Builder parValue(BigDecimal parValue) {
      this.parValue = parValue;
      return this;
    }

    public Builder strikePrice(BigDecimal strikePrice) {
      this.strikePrice = strikePrice;
      return this;
    }

    public Builder contractMultiplier(BigDecimal contractMultiplier) {
      this.contractMultiplier = contractMultiplier;
      return this;
    }

    public Builder underlyingIdentifier(String underlyingIdentifier) {
      this.underlyingIdentifier = underlyingIdentifier;
      return this;
    }

    public Builder notionalCurrency(String notionalCurrency) {
      this.notionalCurrency = notionalCurrency;
      return this;
    }

    public Builder baseCurrency(String baseCurrency) {
      this.baseCurrency = baseCurrency;
      return this;
    }

    public Builder quoteCurrency(String quoteCurrency) {
      this.quoteCurrency = quoteCurrency;
      return this;
    }

    public Builder unitOfMeasure(String unitOfMeasure) {
      this.unitOfMeasure = unitOfMeasure;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(Instant updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Builder version(long version) {
      this.version = version;
      return this;
    }

    public Security build() {
      return new Security(
          id, paceSecId, sedol, cusip, ticker, name, issuer, assetType, currency, countryOfRisk, exchangeMic, status,
          maturityDate, expirationDate, couponRate, parValue, strikePrice, contractMultiplier, underlyingIdentifier,
          notionalCurrency, baseCurrency, quoteCurrency, unitOfMeasure, createdAt, updatedAt, version);
    }
  }
}

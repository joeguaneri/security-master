package com.guaneri.securitymaster.service;

import com.guaneri.securitymaster.domain.AssetType;
import com.guaneri.securitymaster.domain.Security;
import com.guaneri.securitymaster.domain.SecurityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Test-only fixtures for building {@link Security} instances per asset type. */
final class SecurityFixtures {

  private SecurityFixtures() {}

  private static Security.Builder base(AssetType assetType) {
    return Security.builder().name("Test Security").currency("USD").status(SecurityStatus.ACTIVE).assetType(assetType);
  }

  static Security validEquity() {
    return base(AssetType.EQUITY).ticker("TEST").build();
  }

  static Security bondMissingRequiredFields() {
    return base(AssetType.BOND).build();
  }

  static Security validBond() {
    return base(AssetType.BOND).couponRate(new BigDecimal("5.25")).maturityDate(LocalDate.parse("2030-01-01")).build();
  }

  static Security optionMissingStrike(AssetType optionType) {
    return base(optionType).underlyingIdentifier("TEST").build();
  }

  static Security validOption(AssetType optionType) {
    return base(optionType).underlyingIdentifier("TEST").strikePrice(new BigDecimal("100.00")).build();
  }

  static Security swapMissingNotionalCurrency(AssetType swapType) {
    return base(swapType).build();
  }

  static Security validSwap(AssetType swapType) {
    return base(swapType).notionalCurrency("USD").build();
  }

  static Security fxMissingCurrencies(AssetType fxType) {
    return base(fxType).build();
  }

  static Security validFx(AssetType fxType) {
    return base(fxType).baseCurrency("EUR").quoteCurrency("USD").build();
  }
}

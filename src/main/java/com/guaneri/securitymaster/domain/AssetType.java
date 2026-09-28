package com.guaneri.securitymaster.domain;

/**
 * Eagle PACE security type: one flat code per PACESECID.
 *
 * <p>An equity swap is {@link #EQUITYSWAP} on that single row. The equity underlier is
 * {@code underlyingIdentifier} on the same record, not a second security type.
 */
public enum AssetType {
  EQUITY,
  BOND,
  FUND,
  COMMODITY,
  EQUITYOPTION,
  INDEXOPTION,
  FUTUREOPTION,
  FUTURE,
  EQUITYSWAP,
  INTERESTRATESWAP,
  CURRENCYSWAP,
  CREDITDEFAULTSWAP,
  TOTALRETURNSWAP,
  FXSPOT,
  FXFORWARD,
  OTHER
}

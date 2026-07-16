CREATE TABLE securities (
    id                    UUID PRIMARY KEY,
    pacesec_id            VARCHAR(32),
    sedol                 VARCHAR(7),
    cusip                 VARCHAR(9),
    ticker                VARCHAR(16),

    name                  VARCHAR(255) NOT NULL,
    issuer                VARCHAR(255),
    asset_type            VARCHAR(32)  NOT NULL,
    currency              VARCHAR(3)   NOT NULL,
    country_of_risk       VARCHAR(2),
    exchange_mic          VARCHAR(4),
    status                VARCHAR(16)  NOT NULL,

    -- instrument-specific fields; populated depending on asset_type
    maturity_date         DATE,
    expiration_date       DATE,
    coupon_rate           NUMERIC(9, 6),
    par_value             NUMERIC(18, 4),
    strike_price          NUMERIC(18, 6),
    contract_multiplier   NUMERIC(18, 6),
    underlying_identifier VARCHAR(32),
    notional_currency     VARCHAR(3),
    base_currency         VARCHAR(3),
    quote_currency        VARCHAR(3),
    unit_of_measure       VARCHAR(32),

    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version               BIGINT       NOT NULL DEFAULT 0
);

-- Partial unique indexes: multiple NULLs are allowed, but a populated value must be unique.
CREATE UNIQUE INDEX ux_securities_pacesec_id ON securities (pacesec_id) WHERE pacesec_id IS NOT NULL;
CREATE UNIQUE INDEX ux_securities_sedol ON securities (sedol) WHERE sedol IS NOT NULL;
CREATE UNIQUE INDEX ux_securities_cusip ON securities (cusip) WHERE cusip IS NOT NULL;
CREATE UNIQUE INDEX ux_securities_ticker ON securities (ticker) WHERE ticker IS NOT NULL;

CREATE INDEX ix_securities_asset_type ON securities (asset_type);
CREATE INDEX ix_securities_status ON securities (status);

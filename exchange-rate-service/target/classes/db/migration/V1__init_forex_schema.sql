-- Flyway Migration V1: Global Currencies, Countries, and Dynamic Exchange Rates Schema

CREATE TABLE currencies (
    code VARCHAR(3) NOT NULL,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    decimal_places INT NOT NULL DEFAULT 2,
    numeric_code VARCHAR(3) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_currencies PRIMARY KEY (code)
);

CREATE TABLE countries (
    country_code VARCHAR(2) NOT NULL,
    alpha3_code VARCHAR(3) NOT NULL,
    country_name VARCHAR(100) NOT NULL,
    dialing_code VARCHAR(10) NOT NULL,
    default_currency VARCHAR(3) NOT NULL,
    iban_pattern VARCHAR(100),
    iban_length INT,
    swift_prefix VARCHAR(4),
    is_sepa BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_countries PRIMARY KEY (country_code),
    CONSTRAINT fk_country_currency FOREIGN KEY (default_currency) REFERENCES currencies(code)
);

CREATE TABLE exchange_rates (
    id VARCHAR(36) NOT NULL,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    mid_rate NUMERIC(19, 6) NOT NULL,
    bid_rate NUMERIC(19, 6) NOT NULL,
    ask_rate NUMERIC(19, 6) NOT NULL,
    spread_percentage NUMERIC(6, 4) NOT NULL DEFAULT 0.0020,
    change_24h_percentage NUMERIC(6, 2) NOT NULL DEFAULT 0.00,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_exchange_rates PRIMARY KEY (id),
    CONSTRAINT uk_fx_pair UNIQUE (from_currency, to_currency),
    CONSTRAINT fk_fx_from FOREIGN KEY (from_currency) REFERENCES currencies(code),
    CONSTRAINT fk_fx_to FOREIGN KEY (to_currency) REFERENCES currencies(code)
);

CREATE INDEX idx_fx_pair ON exchange_rates (from_currency, to_currency);
CREATE INDEX idx_fx_updated ON exchange_rates (last_updated_at DESC);

CREATE TABLE fx_rate_history (
    id VARCHAR(36) NOT NULL,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate NUMERIC(19, 6) NOT NULL,
    tick_type VARCHAR(20) NOT NULL, -- TICK, EOD_CLOSE, INTERBANK_FEED
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_fx_history PRIMARY KEY (id)
);

CREATE INDEX idx_fx_hist_pair_time ON fx_rate_history (from_currency, to_currency, recorded_at DESC);

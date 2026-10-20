CREATE TABLE IF NOT EXISTS exchange_rates (
    exchange_rate_id UUID PRIMARY KEY,
    currency VARCHAR(10) NOT NULL,
    reference_date DATE NOT NULL,
    quote_timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    buy_rate NUMERIC(10, 4) NOT NULL,
    sell_rate NUMERIC(10, 4) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_exchange_rates_currency_timestamp UNIQUE (currency, quote_timestamp)
);

CREATE INDEX IF NOT EXISTS idx_exchange_rates_currency_ref_date ON exchange_rates (currency, reference_date);

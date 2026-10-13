CREATE TABLE market_prices (
    id UUID PRIMARY KEY,
    source_key VARCHAR(64) NOT NULL UNIQUE,
    state VARCHAR(80) NOT NULL,
    district VARCHAR(80),
    market VARCHAR(120) NOT NULL,
    commodity VARCHAR(120) NOT NULL,
    variety VARCHAR(120),
    grade VARCHAR(80),
    arrival_date VARCHAR(24) NOT NULL,
    min_price NUMERIC(12, 2) NOT NULL,
    max_price NUMERIC(12, 2) NOT NULL,
    modal_price NUMERIC(12, 2) NOT NULL,
    source VARCHAR(40) NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_market_prices_location_date ON market_prices (state, district, arrival_date);
CREATE INDEX idx_market_prices_commodity ON market_prices (commodity, arrival_date);

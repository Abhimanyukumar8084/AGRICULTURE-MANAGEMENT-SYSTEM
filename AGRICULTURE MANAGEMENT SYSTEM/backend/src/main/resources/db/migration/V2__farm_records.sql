CREATE TABLE farm_records (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    kind VARCHAR(16) NOT NULL,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(200),
    description VARCHAR(3000),
    quantity NUMERIC(12, 2),
    unit VARCHAR(32),
    price NUMERIC(12, 2),
    location VARCHAR(120),
    status VARCHAR(16) NOT NULL,
    due_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_farm_records_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_farm_records_owner_created ON farm_records (owner_id, created_at);
CREATE INDEX idx_farm_records_kind_status_created ON farm_records (kind, status, created_at);

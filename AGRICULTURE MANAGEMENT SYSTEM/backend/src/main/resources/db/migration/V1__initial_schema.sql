CREATE TABLE users (
    id UUID PRIMARY KEY,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    phone VARCHAR(24),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE contact_messages (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    topic VARCHAR(40) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE crops (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(32) NOT NULL,
    season VARCHAR(24) NOT NULL,
    variety VARCHAR(100) NOT NULL DEFAULT '',
    growing_duration VARCHAR(80),
    water_requirement VARCHAR(16),
    soil_type VARCHAR(60),
    temperature_range VARCHAR(80),
    description VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_crops_name_variety UNIQUE (name, variety)
);

CREATE TABLE consultations (
    id UUID PRIMARY KEY,
    farmer_id UUID NOT NULL,
    expert_id UUID,
    topic VARCHAR(100) NOT NULL,
    crop VARCHAR(100) NOT NULL,
    question VARCHAR(3000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_consultations_farmer FOREIGN KEY (farmer_id) REFERENCES users (id),
    CONSTRAINT fk_consultations_expert FOREIGN KEY (expert_id) REFERENCES users (id)
);

CREATE INDEX idx_consultations_farmer_created ON consultations (farmer_id, created_at);
CREATE INDEX idx_consultations_expert_created ON consultations (expert_id, created_at);

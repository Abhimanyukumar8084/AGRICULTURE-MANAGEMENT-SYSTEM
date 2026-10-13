CREATE TABLE weather_snapshots (
    user_id UUID PRIMARY KEY,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    forecast_json TEXT NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_weather_snapshots_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

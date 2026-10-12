CREATE TABLE platform_settings (
    setting_key VARCHAR(64) PRIMARY KEY,
    setting_value VARCHAR(160) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO platform_settings (setting_key, setting_value, updated_at) VALUES
    ('platform_name', 'Digital Agriculture', CURRENT_TIMESTAMP),
    ('default_language', 'English', CURRENT_TIMESTAMP),
    ('time_zone', 'Asia/Kolkata', CURRENT_TIMESTAMP),
    ('notify_new_farmer', 'true', CURRENT_TIMESTAMP),
    ('notify_feedback', 'true', CURRENT_TIMESTAMP),
    ('notify_market_prices', 'false', CURRENT_TIMESTAMP);

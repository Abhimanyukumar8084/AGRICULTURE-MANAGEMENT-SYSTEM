ALTER TABLE users ADD COLUMN password_reset_token VARCHAR(128);
ALTER TABLE users ADD COLUMN password_reset_expires_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_users_password_reset_token ON users (password_reset_token);

ALTER TABLE usuario ADD COLUMN google_id VARCHAR(255);
ALTER TABLE usuario ADD CONSTRAINT uk_usuario_google_id UNIQUE (google_id);
ALTER TABLE usuario ALTER COLUMN senha_hash DROP NOT NULL;

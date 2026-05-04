ALTER TABLE checkin_semanal
    ADD COLUMN IF NOT EXISTS semana INTEGER;

CREATE INDEX IF NOT EXISTS idx_checkin_usuario_semana
    ON checkin_semanal(usuario_id, semana);

CREATE INDEX IF NOT EXISTS idx_checkin_plano_semana
    ON checkin_semanal(plano_id, semana);

CREATE INDEX IF NOT EXISTS idx_progresso_plano_semana
    ON progresso_semanal(plano_id, numero_semana);

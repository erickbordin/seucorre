CREATE TABLE IF NOT EXISTS checkin_semanal (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    plano_id UUID NOT NULL,
    avaliacao VARCHAR(100),
    data_checkin DATE,
    nivel_esforco INTEGER,
    nivel_dor INTEGER,
    horas_sono_semana INTEGER,
    analise_ia TEXT,
    plano_reescrito BOOLEAN
);

ALTER TABLE checkin_semanal
    ADD COLUMN IF NOT EXISTS usuario_id UUID,
    ADD COLUMN IF NOT EXISTS plano_id UUID,
    ADD COLUMN IF NOT EXISTS avaliacao VARCHAR(100),
    ADD COLUMN IF NOT EXISTS data_checkin DATE,
    ADD COLUMN IF NOT EXISTS nivel_esforco INTEGER,
    ADD COLUMN IF NOT EXISTS nivel_dor INTEGER,
    ADD COLUMN IF NOT EXISTS horas_sono_semana INTEGER,
    ADD COLUMN IF NOT EXISTS analise_ia TEXT,
    ADD COLUMN IF NOT EXISTS plano_reescrito BOOLEAN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'checkin_semanal'
          AND column_name = 'horas_sono_semana'
          AND data_type <> 'integer'
    ) THEN
        ALTER TABLE checkin_semanal
            ALTER COLUMN horas_sono_semana TYPE INTEGER
            USING ROUND(horas_sono_semana)::INTEGER;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_checkin_usuario'
    ) THEN
        ALTER TABLE checkin_semanal
            ADD CONSTRAINT fk_checkin_usuario
                FOREIGN KEY (usuario_id)
                    REFERENCES usuario(id)
                    ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_checkin_plano'
    ) THEN
        ALTER TABLE checkin_semanal
            ADD CONSTRAINT fk_checkin_plano
                FOREIGN KEY (plano_id)
                    REFERENCES plano_treino(id)
                    ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_checkin_usuario_id
    ON checkin_semanal(usuario_id);

CREATE INDEX IF NOT EXISTS idx_checkin_plano_id
    ON checkin_semanal(plano_id);

CREATE TABLE IF NOT EXISTS progresso_semanal (
    id UUID PRIMARY KEY,
    plano_id UUID NOT NULL,
    numero_semana INTEGER,
    data_inicio_semana DATE,
    volume_km NUMERIC(6, 2),
    pace_medio NUMERIC(5, 2),
    fc_repouso_medio INTEGER,
    treinos_concluidos INTEGER,
    treinos_parciais INTEGER,
    treinos_perdidos INTEGER
);

ALTER TABLE progresso_semanal
    ADD COLUMN IF NOT EXISTS plano_id UUID,
    ADD COLUMN IF NOT EXISTS numero_semana INTEGER,
    ADD COLUMN IF NOT EXISTS data_inicio_semana DATE,
    ADD COLUMN IF NOT EXISTS volume_km NUMERIC(6, 2),
    ADD COLUMN IF NOT EXISTS pace_medio NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS fc_repouso_medio INTEGER,
    ADD COLUMN IF NOT EXISTS treinos_concluidos INTEGER,
    ADD COLUMN IF NOT EXISTS treinos_parciais INTEGER,
    ADD COLUMN IF NOT EXISTS treinos_perdidos INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_progresso_plano'
    ) THEN
        ALTER TABLE progresso_semanal
            ADD CONSTRAINT fk_progresso_plano
                FOREIGN KEY (plano_id)
                    REFERENCES plano_treino(id)
                    ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_progresso_plano_id
    ON progresso_semanal(plano_id);

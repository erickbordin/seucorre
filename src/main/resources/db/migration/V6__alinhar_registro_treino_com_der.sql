CREATE TABLE IF NOT EXISTS registro_treino (
    id UUID PRIMARY KEY,
    treino_id UUID NOT NULL,
    status_conclusao VARCHAR(50),
    distancia_km NUMERIC(6, 2),
    fc_media INTEGER,
    fc_max INTEGER,
    pace_medio INTEGER,
    esforco_percebido INTEGER,
    sentiu_dor BOOLEAN,
    local_dor VARCHAR(100),
    doente BOOLEAN,
    viagem BOOLEAN,
    observacao TEXT
);

ALTER TABLE registro_treino
    ADD COLUMN IF NOT EXISTS treino_id UUID,
    ADD COLUMN IF NOT EXISTS status_conclusao VARCHAR(50),
    ADD COLUMN IF NOT EXISTS distancia_km NUMERIC(6, 2),
    ADD COLUMN IF NOT EXISTS fc_media INTEGER,
    ADD COLUMN IF NOT EXISTS fc_max INTEGER,
    ADD COLUMN IF NOT EXISTS pace_medio INTEGER,
    ADD COLUMN IF NOT EXISTS esforco_percebido INTEGER,
    ADD COLUMN IF NOT EXISTS sentiu_dor BOOLEAN,
    ADD COLUMN IF NOT EXISTS local_dor VARCHAR(100),
    ADD COLUMN IF NOT EXISTS doente BOOLEAN,
    ADD COLUMN IF NOT EXISTS viagem BOOLEAN,
    ADD COLUMN IF NOT EXISTS observacao TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_registro_treino_treino'
    ) THEN
        ALTER TABLE registro_treino
            ADD CONSTRAINT fk_registro_treino_treino
                FOREIGN KEY (treino_id)
                    REFERENCES treino(id)
                    ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_registro_treino_treino_id
    ON registro_treino(treino_id);

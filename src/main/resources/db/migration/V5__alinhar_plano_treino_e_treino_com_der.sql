CREATE TABLE IF NOT EXISTS plano_treino (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    total_semanas INTEGER,
    data_inicio DATE,
    data_fim DATE,
    km_totais NUMERIC(7, 2),
    status VARCHAR(50),
    resumo_ia TEXT,
    guia_tipos_treino TEXT
);

ALTER TABLE plano_treino
    ADD COLUMN IF NOT EXISTS usuario_id UUID,
    ADD COLUMN IF NOT EXISTS total_semanas INTEGER,
    ADD COLUMN IF NOT EXISTS data_inicio DATE,
    ADD COLUMN IF NOT EXISTS data_fim DATE,
    ADD COLUMN IF NOT EXISTS km_totais NUMERIC(7, 2),
    ADD COLUMN IF NOT EXISTS status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS resumo_ia TEXT,
    ADD COLUMN IF NOT EXISTS guia_tipos_treino TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_plano_treino_usuario'
    ) THEN
        ALTER TABLE plano_treino
            ADD CONSTRAINT fk_plano_treino_usuario
                FOREIGN KEY (usuario_id)
                    REFERENCES usuario(id)
                    ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_plano_treino_usuario_id
    ON plano_treino(usuario_id);

CREATE TABLE IF NOT EXISTS treino (
    id UUID PRIMARY KEY,
    plano_id UUID NOT NULL,
    numero_semana INTEGER,
    data_prevista DATE,
    tipo VARCHAR(100),
    distancia_km NUMERIC(6, 2),
    duracao_min INTEGER,
    intensidade VARCHAR(50),
    zona_fc_alvo VARCHAR(50),
    pace_alvo NUMERIC(5, 2),
    descricao TEXT
);

ALTER TABLE treino
    ADD COLUMN IF NOT EXISTS plano_id UUID,
    ADD COLUMN IF NOT EXISTS numero_semana INTEGER,
    ADD COLUMN IF NOT EXISTS data_prevista DATE,
    ADD COLUMN IF NOT EXISTS tipo VARCHAR(100),
    ADD COLUMN IF NOT EXISTS distancia_km NUMERIC(6, 2),
    ADD COLUMN IF NOT EXISTS duracao_min INTEGER,
    ADD COLUMN IF NOT EXISTS intensidade VARCHAR(50),
    ADD COLUMN IF NOT EXISTS zona_fc_alvo VARCHAR(50),
    ADD COLUMN IF NOT EXISTS pace_alvo NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS descricao TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_treino_plano'
    ) THEN
        ALTER TABLE treino
            ADD CONSTRAINT fk_treino_plano
                FOREIGN KEY (plano_id)
                    REFERENCES plano_treino(id)
                    ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_treino_plano_id
    ON treino(plano_id);

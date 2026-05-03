CREATE TABLE IF NOT EXISTS perfil_corrida (
    id              UUID PRIMARY KEY,
    usuario_id      UUID NOT NULL UNIQUE,
    pace_5k_min_km  NUMERIC(5, 2),
    pace_10k_min_km NUMERIC(5, 2),
    pace_21k_min_km NUMERIC(5, 2),
    pace_42k_min_km NUMERIC(5, 2),
    vo2_estimado    INTEGER,
    atualizado_em   TIMESTAMP,

    CONSTRAINT fk_perfil_corrida_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario(id)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS condicao_saude (
    id          UUID PRIMARY KEY,
    usuario_id  UUID NOT NULL,
    tipo        VARCHAR(100) NOT NULL,
    descricao   VARCHAR(500),

    CONSTRAINT fk_condicao_saude_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario(id)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS checkin_semanal (
    id                  UUID PRIMARY KEY,
    usuario_id          UUID NOT NULL,
    plano_id            UUID NOT NULL,
    avaliacao           VARCHAR(100),
    data_checkin        DATE,
    nivel_esforco       INTEGER,
    nivel_dor           INTEGER,
    horas_sono_semana   NUMERIC(4, 1),
    analise_ia          TEXT,
    plano_reescrito     BOOLEAN,

    CONSTRAINT fk_checkin_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_checkin_plano
        FOREIGN KEY (plano_id)
            REFERENCES plano_treino(id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_perfil_corrida_usuario_id ON perfil_corrida(usuario_id);
CREATE INDEX IF NOT EXISTS idx_condicao_saude_usuario_id ON condicao_saude(usuario_id);
CREATE INDEX IF NOT EXISTS idx_checkin_usuario_id ON checkin_semanal(usuario_id);
CREATE INDEX IF NOT EXISTS idx_checkin_plano_id ON checkin_semanal(plano_id);

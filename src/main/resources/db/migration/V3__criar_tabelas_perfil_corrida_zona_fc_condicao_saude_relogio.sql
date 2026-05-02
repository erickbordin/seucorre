CREATE TABLE perfil_corrida (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL UNIQUE,
    pace_5k_min_km NUMERIC(5, 2),
    pace_10k_min_km NUMERIC(5, 2),
    pace_21k_min_km NUMERIC(5, 2),
    pace_42k_min_km NUMERIC(5, 2),
    vo2_estimado INTEGER,
    atualizado_em TIMESTAMP,

    CONSTRAINT fk_perfil_corrida_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id)
        ON DELETE CASCADE
);

CREATE TABLE zona_fc (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    zona INTEGER NOT NULL,
    nome VARCHAR(100) NOT NULL,
    fc_min INTEGER NOT NULL,
    fc_max INTEGER NOT NULL,
    personalizada BOOLEAN NOT NULL DEFAULT FALSE,
    atualizado_em TIMESTAMP,

    CONSTRAINT fk_zona_fc_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_zona_fc_usuario_zona
        UNIQUE (usuario_id, zona),

    CONSTRAINT ck_zona_fc_zona
        CHECK (zona BETWEEN 1 AND 5),

    CONSTRAINT ck_zona_fc_frequencia
        CHECK (fc_min > 0 AND fc_max >= fc_min)
);

CREATE TABLE condicao_saude (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),

    CONSTRAINT fk_condicao_saude_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id)
        ON DELETE CASCADE
);

CREATE TABLE relogio (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    plataforma VARCHAR(50) NOT NULL,
    token_acesso VARCHAR(1000),

    CONSTRAINT fk_relogio_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_perfil_corrida_usuario_id ON perfil_corrida(usuario_id);
CREATE INDEX idx_zona_fc_usuario_id ON zona_fc(usuario_id);
CREATE INDEX idx_condicao_saude_usuario_id ON condicao_saude(usuario_id);
CREATE INDEX idx_relogio_usuario_id ON relogio(usuario_id);

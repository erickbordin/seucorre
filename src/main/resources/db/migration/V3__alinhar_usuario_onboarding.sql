ALTER TABLE users RENAME TO usuario;
ALTER TABLE usuario RENAME COLUMN senha TO senha_hash;
ALTER TABLE usuario RENAME COLUMN created_at TO criado_em;

ALTER TABLE usuario
    ADD COLUMN peso_kg NUMERIC(5, 2),
    ADD COLUMN altura_cm NUMERIC(5, 2),
    ADD COLUMN data_nascimento DATE,
    ADD COLUMN genero VARCHAR(50),
    ADD COLUMN nivel_condicionamento VARCHAR(50),
    ADD COLUMN objetivo VARCHAR(50),
    ADD COLUMN ja_corre BOOLEAN,
    ADD COLUMN sedentario BOOLEAN,
    ADD COLUMN horas_sono_media INTEGER,
    ADD COLUMN dias_disponiveis_semana INTEGER,
    ADD COLUMN dias_semana_treino VARCHAR(100),
    ADD COLUMN fc_repouso INTEGER,
    ADD COLUMN fc_maxima INTEGER,
    ADD COLUMN atualizado_em TIMESTAMP;

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
    perfil_corrida_id UUID NOT NULL,
    zona INTEGER NOT NULL,
    nome VARCHAR(100),
    fc_min INTEGER NOT NULL,
    fc_max INTEGER NOT NULL,
    personalizada BOOLEAN NOT NULL DEFAULT FALSE,
    atualizado_em TIMESTAMP,

    CONSTRAINT fk_zona_fc_perfil_corrida
        FOREIGN KEY (perfil_corrida_id)
        REFERENCES perfil_corrida(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_zona_fc_perfil_zona
        UNIQUE (perfil_corrida_id, zona),

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
    ativa BOOLEAN NOT NULL DEFAULT TRUE,

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
    token_expires_at TIMESTAMP,

    CONSTRAINT fk_relogio_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_perfil_corrida_usuario_id ON perfil_corrida(usuario_id);
CREATE INDEX idx_zona_fc_perfil_corrida_id ON zona_fc(perfil_corrida_id);
CREATE INDEX idx_condicao_saude_usuario_id ON condicao_saude(usuario_id);
CREATE INDEX idx_relogio_usuario_id ON relogio(usuario_id);

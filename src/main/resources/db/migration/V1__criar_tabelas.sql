-- =============================================================
-- V1__criar_tabelas.sql
-- Criação completa do schema baseado no DER
-- =============================================================

-- -------------------------------------------------------------
-- USUARIO
-- -------------------------------------------------------------
CREATE TABLE usuario (
     id                      UUID PRIMARY KEY,
     nome                    VARCHAR(255) NOT NULL,
     email                   VARCHAR(255) NOT NULL UNIQUE,
     senha_hash              VARCHAR(255) NOT NULL,
     telefone                VARCHAR(20),
     role                    VARCHAR(50),

-- Dados físicos
     idade                   INTEGER,
     genero                  VARCHAR(50),
     peso_kg                 NUMERIC(5, 2),
     altura_cm               NUMERIC(5, 2),
     data_nascimento         DATE,

-- Perfil atleta
     nivel_condicionamento   VARCHAR(50),
     objetivo                VARCHAR(50),
     ja_corre                BOOLEAN,
     sedentario              BOOLEAN,
     horas_sono_media        INTEGER,
     dias_disponiveis_semana INTEGER,
     dias_semana_treino      VARCHAR(100),

-- FC
     fc_repouso              INTEGER,
     fc_maximo               INTEGER,

-- Auditoria
     criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     atualizado_em           TIMESTAMP
);

-- -------------------------------------------------------------
-- PERFIL_CORRIDA
-- -------------------------------------------------------------
CREATE TABLE perfil_corrida (
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

-- -------------------------------------------------------------
-- ZONA_FC
-- -------------------------------------------------------------
CREATE TABLE zona_fc (
     id              UUID PRIMARY KEY,
     perfil_corrida_id UUID NOT NULL,
     zona            INTEGER NOT NULL,
     nome            VARCHAR(100) NOT NULL,
     fc_min          INTEGER NOT NULL,
     fc_max          INTEGER NOT NULL,
     personalizada   BOOLEAN NOT NULL DEFAULT FALSE,
     atualizado_em   TIMESTAMP,

     CONSTRAINT fk_zona_fc_perfil_corrida
         FOREIGN KEY (perfil_corrida_id)
             REFERENCES perfil_corrida(id)
             ON DELETE CASCADE,

     CONSTRAINT uk_zona_fc_perfil_corrida_zona
         UNIQUE (perfil_corrida_id, zona),

     CONSTRAINT ck_zona_fc_zona
         CHECK (zona BETWEEN 1 AND 5),

     CONSTRAINT ck_zona_fc_frequencia
         CHECK (fc_min > 0 AND fc_max >= fc_min)
);

-- -------------------------------------------------------------
-- CONDICAO_SAUDE
-- -------------------------------------------------------------
CREATE TABLE condicao_saude (
    id          UUID PRIMARY KEY,
    usuario_id  UUID NOT NULL,
    tipo        VARCHAR(100) NOT NULL,
    descricao   VARCHAR(500),
    ativa       BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_condicao_saude_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario(id)
            ON DELETE CASCADE
);

-- -------------------------------------------------------------
-- RELOGIO
-- -------------------------------------------------------------
CREATE TABLE relogio (
     id           UUID PRIMARY KEY,
     usuario_id   UUID NOT NULL,
     plataforma   VARCHAR(50) NOT NULL,
     token_acesso VARCHAR(1000),
     token_expires_at TIMESTAMP,

     CONSTRAINT fk_relogio_usuario
         FOREIGN KEY (usuario_id)
             REFERENCES usuario(id)
             ON DELETE CASCADE
);

-- -------------------------------------------------------------
-- PLANO_TREINO
-- -------------------------------------------------------------
CREATE TABLE plano_treino (
      id               UUID PRIMARY KEY,
      usuario_id       UUID NOT NULL,
      total_semanas    INTEGER,
      data_inicio      DATE,
      data_fim         DATE,
      km_totais        NUMERIC(7, 2),
      status           VARCHAR(50),
      resumo_ia        TEXT,
      guia_tipos_treino TEXT,

      CONSTRAINT fk_plano_treino_usuario
          FOREIGN KEY (usuario_id)
              REFERENCES usuario(id)
              ON DELETE CASCADE
);

-- -------------------------------------------------------------
-- TREINO
-- -------------------------------------------------------------
CREATE TABLE treino (
    id              UUID PRIMARY KEY,
    plano_id        UUID NOT NULL,
    numero_semana   INTEGER,
    data_prevista   DATE,
    tipo            VARCHAR(100),
    distancia_km    NUMERIC(6, 2),
    duracao_min     INTEGER,
    intensidade     VARCHAR(50),
    zona_fc_alvo    VARCHAR(50),
    pace_alvo       NUMERIC(5, 2),
    descricao       TEXT,

    CONSTRAINT fk_treino_plano
        FOREIGN KEY (plano_id)
            REFERENCES plano_treino(id)
            ON DELETE CASCADE
);

-- -------------------------------------------------------------
-- REGISTRO_TREINO
-- -------------------------------------------------------------
CREATE TABLE registro_treino (
     id                  UUID PRIMARY KEY,
     treino_id           UUID NOT NULL,
     status_conclusao    VARCHAR(50),
     distancia_km        NUMERIC(6, 2),
     fc_media            INTEGER,
     fc_max              INTEGER,
     pace_medio          INTEGER,
     esforco_percebido   INTEGER,
     sentiu_dor          BOOLEAN,
     local_dor           VARCHAR(100),
     doente              BOOLEAN,
     viagem              BOOLEAN,
     observacao          TEXT,

     CONSTRAINT fk_registro_treino_treino
         FOREIGN KEY (treino_id)
             REFERENCES treino(id)
             ON DELETE CASCADE
);

-- -------------------------------------------------------------
-- CHECKIN_SEMANAL
-- -------------------------------------------------------------
CREATE TABLE checkin_semanal (
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

-- -------------------------------------------------------------
-- PROGRESSO_SEMANAL
-- -------------------------------------------------------------
CREATE TABLE progresso_semanal (
       id                  UUID PRIMARY KEY,
       plano_id            UUID NOT NULL,
       numero_semana       INTEGER,
       data_inicio_semana  DATE,
       volume_km           NUMERIC(6, 2),
       pace_medio          NUMERIC(5, 2),
       fc_repouso_medio    INTEGER,
       treinos_concluidos  INTEGER,
       treinos_parciais    INTEGER,
       treinos_perdidos    INTEGER,

       CONSTRAINT fk_progresso_plano
           FOREIGN KEY (plano_id)
               REFERENCES plano_treino(id)
               ON DELETE CASCADE
);

-- =============================================================
-- ÍNDICES
-- =============================================================
CREATE INDEX idx_perfil_corrida_usuario_id  ON perfil_corrida(usuario_id);
CREATE INDEX idx_zona_fc_perfil_corrida_id  ON zona_fc(perfil_corrida_id);
CREATE INDEX idx_condicao_saude_usuario_id  ON condicao_saude(usuario_id);
CREATE INDEX idx_relogio_usuario_id         ON relogio(usuario_id);
CREATE INDEX idx_plano_treino_usuario_id    ON plano_treino(usuario_id);
CREATE INDEX idx_treino_plano_id            ON treino(plano_id);
CREATE INDEX idx_registro_treino_treino_id  ON registro_treino(treino_id);
CREATE INDEX idx_checkin_usuario_id         ON checkin_semanal(usuario_id);
CREATE INDEX idx_checkin_plano_id           ON checkin_semanal(plano_id);
CREATE INDEX idx_progresso_plano_id         ON progresso_semanal(plano_id);

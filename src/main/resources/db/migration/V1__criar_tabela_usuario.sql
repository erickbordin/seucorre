CREATE TABLE usuario (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,

    -- Dados Físicos (Flat)
    peso NUMERIC(5, 2),
    altura NUMERIC(5, 2),
    data_nascimento DATE,

    -- Perfil Atleta (Flat)
    experiencia VARCHAR(50),      -- Vai receber o Enum (ex: INICIANTE, INTERMEDIARIO)
    objetivo VARCHAR(50),         -- Vai receber o Enum (ex: PERDER_PESO, MARATONA)
    fc_maxima INTEGER,
    
    -- Pace Alvo (Flat - Dividido para facilitar a reconstrução do nosso VO)
    pace_alvo_minutos INTEGER,
    pace_alvo_segundos INTEGER,

    -- Auditoria básica
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP
);
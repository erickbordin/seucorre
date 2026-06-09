ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS role VARCHAR(50);

UPDATE usuario
SET role = 'USER'
WHERE role IS NULL OR role = '';

ALTER TABLE condicao_saude
    ADD COLUMN IF NOT EXISTS ativa BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE relogio
    ADD COLUMN IF NOT EXISTS token_expires_at TIMESTAMP;

ALTER TABLE zona_fc
    ADD COLUMN IF NOT EXISTS perfil_corrida_id UUID;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'zona_fc'
          AND column_name = 'usuario_id'
    ) THEN
        UPDATE zona_fc z
        SET perfil_corrida_id = pc.id
        FROM perfil_corrida pc
        WHERE z.perfil_corrida_id IS NULL
          AND z.usuario_id = pc.usuario_id;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_zona_fc_perfil_corrida'
    ) THEN
        ALTER TABLE zona_fc
            ADD CONSTRAINT fk_zona_fc_perfil_corrida
                FOREIGN KEY (perfil_corrida_id)
                    REFERENCES perfil_corrida(id)
                    ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_zona_fc_perfil_corrida_zona'
    ) THEN
        ALTER TABLE zona_fc
            ADD CONSTRAINT uk_zona_fc_perfil_corrida_zona
                UNIQUE (perfil_corrida_id, zona);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_zona_fc_perfil_corrida_id
    ON zona_fc(perfil_corrida_id);

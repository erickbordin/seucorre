UPDATE plano_treino
SET status = 'CANCELADO'
WHERE id IN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY usuario_id ORDER BY data_inicio DESC NULLS LAST, id) AS posicao
        FROM plano_treino
        WHERE status = 'ATIVO'
    ) planos_ativos
    WHERE posicao > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_plano_treino_usuario_ativo
    ON plano_treino(usuario_id)
    WHERE status = 'ATIVO';

DROP INDEX IF EXISTS idx_checkin_plano_semana;
CREATE UNIQUE INDEX IF NOT EXISTS uk_checkin_plano_semana
    ON checkin_semanal(plano_id, semana);

DROP INDEX IF EXISTS idx_progresso_plano_semana;
CREATE UNIQUE INDEX IF NOT EXISTS uk_progresso_plano_semana
    ON progresso_semanal(plano_id, numero_semana);

DROP INDEX IF EXISTS idx_registro_treino_treino_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_registro_treino_treino
    ON registro_treino(treino_id);

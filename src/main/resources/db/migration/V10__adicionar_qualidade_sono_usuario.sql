ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS qualidade_sono VARCHAR(20);

UPDATE usuario
SET qualidade_sono = CASE
    WHEN horas_sono_media < 6 THEN 'RUIM'
    WHEN horas_sono_media < 7 THEN 'REGULAR'
    WHEN horas_sono_media < 8 THEN 'BOA'
    ELSE 'OTIMA'
END
WHERE qualidade_sono IS NULL
  AND horas_sono_media IS NOT NULL;

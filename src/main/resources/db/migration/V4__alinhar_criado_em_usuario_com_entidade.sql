ALTER TABLE usuario
    ALTER COLUMN criado_em TYPE DATE
    USING criado_em::date;

ALTER TABLE usuario
    ALTER COLUMN criado_em SET DEFAULT CURRENT_DATE;

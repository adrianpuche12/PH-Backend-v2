-- Agrega campo de observaciones opcionales al cierre de turno.
ALTER TABLE shifts ADD COLUMN IF NOT EXISTS notes VARCHAR(500);

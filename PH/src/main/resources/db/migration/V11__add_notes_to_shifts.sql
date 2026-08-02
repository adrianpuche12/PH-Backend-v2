-- Agrega campo de observaciones opcionales al cierre de turno.
ALTER TABLE shifts ADD COLUMN notes VARCHAR(500);

-- Migración de INTEGER → NUMERIC(10,4) para soportar stock fraccionario
-- Aplicada manualmente en PROD y DEV el 13-Jul-2026 (feature: medio/cuarto de pollo)
-- Esta versión Flyway formaliza el cambio para nuevos ambientes

ALTER TABLE inventory_stock
    ALTER COLUMN quantity TYPE NUMERIC(10,4) USING quantity::NUMERIC(10,4);

ALTER TABLE inventory_movements
    ALTER COLUMN quantity TYPE NUMERIC(10,4) USING quantity::NUMERIC(10,4);

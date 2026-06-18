-- ============================================================
-- V2 — Agrega trazabilidad de edición a sales
-- Permite editar una venta mientras el turno sigue abierto.
-- ============================================================

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS edited BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS edited_at TIMESTAMP;

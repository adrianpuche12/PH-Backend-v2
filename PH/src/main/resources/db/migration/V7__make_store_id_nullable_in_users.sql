-- V7 — store_id en app_users pasa a nullable (2026-07-25)
-- Permite crear usuarios CONTADOR/SOCIO sin local principal asignado.
ALTER TABLE app_users ALTER COLUMN store_id DROP NOT NULL;

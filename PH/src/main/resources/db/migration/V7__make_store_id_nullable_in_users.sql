-- V7 — store_id en app_users pasa a nullable (2026-07-25)
-- Permite crear usuarios CONTADOR/SOCIO sin local principal asignado.
-- Idempotente: solo aplica si la columna es NOT NULL.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'app_users'
          AND column_name = 'store_id'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE app_users ALTER COLUMN store_id DROP NOT NULL;
    END IF;
END $$;

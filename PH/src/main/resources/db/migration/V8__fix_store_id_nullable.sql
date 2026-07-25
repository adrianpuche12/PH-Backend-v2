-- V8 — Asegurar que store_id sea nullable en app_users (2026-07-25)
-- Corrige V7 si no se aplicó correctamente.
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

-- ============================================================
-- V6 — Módulo de permisos avanzado (2026-07-25)
--
-- Agrega email, role, first_login a app_users y crea las
-- tablas de permisos por sección y acceso multi-local.
-- ============================================================

-- Columnas nuevas en app_users (nullable para no romper filas existentes)
ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS email       VARCHAR(255),
    ADD COLUMN IF NOT EXISTS role        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS first_login BOOLEAN NOT NULL DEFAULT FALSE;

-- Tabla de locales accesibles por usuario (muchos a muchos)
CREATE TABLE IF NOT EXISTS user_store_access (
    user_id  BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    store_id BIGINT NOT NULL REFERENCES store(id)     ON DELETE CASCADE,
    PRIMARY KEY (user_id, store_id)
);

-- Tabla de permisos por sección
CREATE TABLE IF NOT EXISTS user_permissions (
    user_id    BIGINT       NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, permission)
);

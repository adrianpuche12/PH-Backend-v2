-- ============================================================
-- V1 — Baseline schema completo (2026-06-16)
--
-- Este script crea todas las tablas para instalaciones nuevas.
-- En DBs ya existentes (dev/prod), spring.flyway.baseline-on-migrate=true
-- marca esta versión como "ya aplicada" sin ejecutarla.
-- Cambios futuros van en V2__*.sql, V3__*.sql, etc.
-- ============================================================

CREATE TABLE IF NOT EXISTS store (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(255),
    phone       VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id             BIGSERIAL    PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    description    VARCHAR(255),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order  INTEGER      DEFAULT 0,
    parent_id      BIGINT       REFERENCES categories(id),
    store_id       BIGINT       NOT NULL REFERENCES store(id),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id           BIGSERIAL     PRIMARY KEY,
    name         VARCHAR(255)  NOT NULL,
    sku          VARCHAR(255),
    type         VARCHAR(255)  NOT NULL DEFAULT 'SIMPLE',
    price        NUMERIC(19,2) NOT NULL,
    min_stock    INTEGER       NOT NULL DEFAULT 0,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    description  VARCHAR(255),
    category_id  BIGINT        REFERENCES categories(id),
    store_id     BIGINT        NOT NULL REFERENCES store(id),
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id              BIGSERIAL     PRIMARY KEY,
    type            VARCHAR(255)  NOT NULL,
    amount          NUMERIC(19,2) NOT NULL,
    date            DATE          NOT NULL,
    description     VARCHAR(255),
    store_id        BIGINT        NOT NULL REFERENCES store(id),
    gasto_admin_id  BIGINT,
    image_uri       VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS gasto_admin (
    id                  BIGSERIAL     PRIMARY KEY,
    fecha               DATE          NOT NULL,
    monto               NUMERIC(10,2) NOT NULL,
    descripcion         VARCHAR(255)  NOT NULL,
    porcentaje_danli    INTEGER,
    porcentaje_paraiso  INTEGER,
    monto_danli         NUMERIC(10,2),
    monto_paraiso       NUMERIC(10,2),
    username            VARCHAR(100)  NOT NULL DEFAULT 'admin_user',
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP
);

CREATE TABLE IF NOT EXISTS closing_deposits (
    id               BIGSERIAL     PRIMARY KEY,
    closings_count   INTEGER,
    amount           NUMERIC(19,2) NOT NULL,
    deposit_date     DATE          NOT NULL,
    period_start     DATE          NOT NULL,
    period_end       DATE          NOT NULL,
    username         VARCHAR(255)  NOT NULL,
    image_uri        VARCHAR(512),
    deposit_status   VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    bank_deposit_id  BIGINT,
    shift_id         BIGINT,
    store_id         BIGINT        NOT NULL REFERENCES store(id)
);

CREATE TABLE IF NOT EXISTS salary_payments (
    id           BIGSERIAL     PRIMARY KEY,
    description  VARCHAR(255)  NOT NULL,
    amount       NUMERIC(19,2) NOT NULL,
    username     VARCHAR(255)  NOT NULL,
    salary_date  DATE          NOT NULL,
    store_id     BIGINT        NOT NULL REFERENCES store(id),
    image_uri    VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS supplier_payments (
    id            BIGSERIAL     PRIMARY KEY,
    supplier      VARCHAR(255),
    description   VARCHAR(255),
    amount        NUMERIC(19,2) NOT NULL,
    payment_date  DATE          NOT NULL,
    username      VARCHAR(255)  NOT NULL,
    store_id      BIGINT        NOT NULL REFERENCES store(id),
    image_uri     VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS app_users (
    id           BIGSERIAL    PRIMARY KEY,
    keycloak_id  VARCHAR(255) NOT NULL UNIQUE,
    full_name    VARCHAR(255) NOT NULL,
    username     VARCHAR(255) NOT NULL UNIQUE,
    store_id     BIGINT       NOT NULL REFERENCES store(id),
    status       VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shifts (
    id                    BIGSERIAL     PRIMARY KEY,
    code                  VARCHAR(255)  NOT NULL UNIQUE,
    username              VARCHAR(255)  NOT NULL,
    status                VARCHAR(255)  NOT NULL DEFAULT 'OPEN',
    store_id              BIGINT        NOT NULL REFERENCES store(id),
    opened_at             TIMESTAMP,
    closed_at             TIMESTAMP,
    total_cash_sales      NUMERIC(12,2) DEFAULT 0,
    total_card_sales      NUMERIC(12,2) DEFAULT 0,
    total_shift_expenses  NUMERIC(12,2) DEFAULT 0,
    opening_cash_amount   NUMERIC(12,2) DEFAULT 0,
    declared_cash_amount  NUMERIC(12,2),
    cash_difference       NUMERIC(12,2),
    deposited             BOOLEAN       DEFAULT FALSE,
    deposit_id            BIGINT
);

CREATE TABLE IF NOT EXISTS sales (
    id              BIGSERIAL     PRIMARY KEY,
    shift_id        BIGINT        NOT NULL REFERENCES shifts(id),
    store_id        BIGINT        NOT NULL REFERENCES store(id),
    username        VARCHAR(255)  NOT NULL,
    sale_date       DATE          NOT NULL,
    status          VARCHAR(255)  NOT NULL DEFAULT 'OPEN',
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0,
    isv             NUMERIC(12,2) NOT NULL DEFAULT 0,
    total           NUMERIC(12,2) NOT NULL DEFAULT 0,
    payment_method  VARCHAR(10)   NOT NULL DEFAULT 'CASH',
    cash_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    card_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sale_items (
    id                     BIGSERIAL     PRIMARY KEY,
    sale_id                BIGINT        NOT NULL REFERENCES sales(id),
    product_id             BIGINT        NOT NULL REFERENCES products(id),
    product_name_snapshot  VARCHAR(255)  NOT NULL,
    unit_price_snapshot    NUMERIC(12,2) NOT NULL,
    quantity               INTEGER       NOT NULL,
    subtotal               NUMERIC(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS shift_expenses (
    id           BIGSERIAL     PRIMARY KEY,
    shift_id     BIGINT        NOT NULL REFERENCES shifts(id),
    description  VARCHAR(255)  NOT NULL,
    amount       NUMERIC(12,2) NOT NULL,
    username     VARCHAR(255)  NOT NULL,
    created_at   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory_movements (
    id          BIGSERIAL    PRIMARY KEY,
    type        VARCHAR(255) NOT NULL,
    quantity    INTEGER      NOT NULL,
    reason      VARCHAR(255),
    notes       VARCHAR(255),
    username    VARCHAR(255),
    product_id  BIGINT       NOT NULL REFERENCES products(id),
    store_id    BIGINT       NOT NULL REFERENCES store(id),
    created_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory_stock (
    id          BIGSERIAL PRIMARY KEY,
    quantity    INTEGER   NOT NULL DEFAULT 0,
    product_id  BIGINT    NOT NULL REFERENCES products(id),
    store_id    BIGINT    NOT NULL REFERENCES store(id),
    updated_at  TIMESTAMP,
    CONSTRAINT uq_inventory_stock_product_store UNIQUE (product_id, store_id)
);

CREATE TABLE IF NOT EXISTS bank_deposits (
    id               BIGSERIAL     PRIMARY KEY,
    created_by       VARCHAR(100)  NOT NULL,
    deposit_date     DATE          NOT NULL,
    store_ids        VARCHAR(255)  NOT NULL,
    shift_ids        VARCHAR(255)  NOT NULL,
    expected_cash    NUMERIC(12,2) NOT NULL,
    declared_amount  NUMERIC(12,2) NOT NULL,
    difference       NUMERIC(12,2) NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    image_uri        VARCHAR(512),
    notes            TEXT,
    created_at       TIMESTAMP
);

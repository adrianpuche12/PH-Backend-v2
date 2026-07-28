-- V9 — Permite eliminar productos aunque tengan historial de ventas (2026-07-29)
--
-- sale_items guarda productNameSnapshot y unitPriceSnapshot al momento de la venta,
-- por lo que el historial se preserva aunque product_id quede en NULL.
ALTER TABLE sale_items ALTER COLUMN product_id DROP NOT NULL;

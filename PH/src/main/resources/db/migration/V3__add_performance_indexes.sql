-- Índices de performance: dashboard + transacciones admin

-- transactions: búsquedas por local y rango de fechas
CREATE INDEX IF NOT EXISTS idx_transactions_store_id  ON transactions(store_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date       ON transactions(date);
CREATE INDEX IF NOT EXISTS idx_transactions_store_date ON transactions(store_id, date DESC);

-- sales: dashboard (ventas de hoy por local, ventas por turno)
CREATE INDEX IF NOT EXISTS idx_sales_store_date        ON sales(store_id, sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_shift_status      ON sales(shift_id, status);

-- shifts: turnos abiertos por local
CREATE INDEX IF NOT EXISTS idx_shifts_store_status     ON shifts(store_id, status);

-- inventory_stock: stock bajo y valor estimado por local
CREATE INDEX IF NOT EXISTS idx_inventory_stock_store   ON inventory_stock(store_id);

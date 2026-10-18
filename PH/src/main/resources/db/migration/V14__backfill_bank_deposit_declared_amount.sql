-- Rellena declared_amount y difference para registros creados antes de que
-- se introdujera el campo (cuando ddl-auto=update no aplicaba la restricción NOT NULL).
-- Para esos depósitos históricos usamos expected_cash como valor declarado,
-- lo que equivale a un depósito exacto (sin diferencia).
UPDATE bank_deposits
SET    declared_amount = expected_cash,
       difference      = 0
WHERE  declared_amount IS NULL;

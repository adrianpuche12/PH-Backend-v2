-- Denormaliza declared_amount y difference del banco directamente en closing_deposits.
-- Elimina la dependencia del join con bank_deposits en el endpoint de todas las operaciones.
ALTER TABLE closing_deposits
    ADD COLUMN IF NOT EXISTS bank_declared_amount NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS bank_difference       NUMERIC(12,2);

-- Backfill para cierres ya depositados
UPDATE closing_deposits cd
SET    bank_declared_amount = bd.declared_amount,
       bank_difference      = bd.difference
FROM   bank_deposits bd
WHERE  cd.bank_deposit_id = bd.id
  AND  cd.deposit_status  = 'DEPOSITED';

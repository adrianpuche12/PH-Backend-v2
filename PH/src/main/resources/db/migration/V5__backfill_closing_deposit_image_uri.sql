-- Backfill image_uri en closing_deposits desde bank_deposits para registros existentes
UPDATE closing_deposits cd
SET image_uri = bd.image_uri
FROM bank_deposits bd
WHERE cd.bank_deposit_id = bd.id
  AND cd.image_uri IS NULL
  AND bd.image_uri IS NOT NULL;
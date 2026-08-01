-- Asigna permisos por defecto a usuarios no-admin sin permisos asignados.
-- Los permisos se guardan como filas en user_permissions (ElementCollection).
-- Solo afecta usuarios que no tienen NINGUNA fila en user_permissions.
INSERT INTO user_permissions (user_id, permission)
SELECT u.id, p.permission
FROM app_users u
CROSS JOIN (VALUES ('POS'), ('SALES_HISTORY'), ('INVENTORY')) AS p(permission)
WHERE u.role != 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM user_permissions up WHERE up.user_id = u.id
  )
ON CONFLICT DO NOTHING;

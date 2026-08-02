-- Asigna permisos por defecto a usuarios no-admin que aún no tienen permisos en user_permissions.
-- Los permisos se guardan en la tabla user_permissions (ElementCollection), no como columna en app_users.
-- Usuarios sin filas en user_permissions tenían acceso total; ahora se restringe explícitamente.
INSERT INTO user_permissions (user_id, permission)
SELECT u.id, p.permission
FROM app_users u
CROSS JOIN (VALUES ('POS'), ('SALES_HISTORY'), ('INVENTORY')) AS p(permission)
WHERE (u.role IS NULL OR u.role != 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM user_permissions up WHERE up.user_id = u.id
  )
ON CONFLICT DO NOTHING;

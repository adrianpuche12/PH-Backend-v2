-- Asigna permisos por defecto a usuarios no-admin que tienen permisos vacíos.
-- Antes de este fix, permissions=[] significaba acceso completo (incluyendo Operaciones).
-- Ahora se establece explícitamente: POS + historial de ventas + inventario.
-- El admin puede ajustar permisos individuales desde el panel de usuarios.
UPDATE app_users
SET permissions = '["POS","SALES_HISTORY","INVENTORY"]'
WHERE (permissions = '[]' OR permissions IS NULL)
  AND role != 'ADMIN';

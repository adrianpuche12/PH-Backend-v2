# Endpoints propuestos

Estos endpoints son una propuesta inicial. No existen necesariamente en el backend actual y deben validarse antes de implementarse.

## Inventario - categorias

| Metodo | Endpoint | Descripcion |
|---|---|---|
| `POST` | `/api/inventory/categories` | Crear categoria. |
| `GET` | `/api/inventory/categories/store/{storeId}` | Listar categorias de un local. |
| `GET` | `/api/inventory/categories/tree/store/{storeId}` | Listar arbol de categorias de un local. |
| `GET` | `/api/inventory/categories/{id}` | Obtener categoria por id. |
| `PUT` | `/api/inventory/categories/{id}` | Editar categoria. |
| `DELETE` | `/api/inventory/categories/{id}` | Desactivar o eliminar categoria segun regla definida. |

Recomendacion:

- `DELETE` deberia hacer eliminacion logica (`active=false`) si hay productos o historico.

## Inventario - productos

| Metodo | Endpoint | Descripcion |
|---|---|---|
| `POST` | `/api/inventory/products` | Crear producto. |
| `GET` | `/api/inventory/products/store/{storeId}` | Listar productos de un local. |
| `GET` | `/api/inventory/products/category/{categoryId}` | Listar productos por categoria. |
| `GET` | `/api/inventory/products/{id}` | Obtener producto por id. |
| `PUT` | `/api/inventory/products/{id}` | Editar producto completo. |
| `DELETE` | `/api/inventory/products/{id}` | Desactivar producto. |
| `PATCH` | `/api/inventory/products/{id}/stock` | Ajustar stock. |
| `PATCH` | `/api/inventory/products/{id}/price` | Cambiar precio. |

Reglas recomendadas:

- Cambios de stock deben registrar motivo.
- Cambios de precio deben auditarse.
- Productos inactivos no deben aparecer en flujo operativo de ventas.

## Ventas - turnos

| Metodo | Endpoint | Descripcion |
|---|---|---|
| `POST` | `/api/sales/shifts/open` | Abrir turno para local. |
| `GET` | `/api/sales/shifts/open/store/{storeId}` | Obtener turno abierto de un local. |
| `GET` | `/api/sales/shifts/store/{storeId}` | Listar turnos de un local. |
| `GET` | `/api/sales/shifts/{id}` | Obtener turno por id. |
| `POST` | `/api/sales/shifts/{id}/close` | Cerrar turno. |
| `PATCH` | `/api/sales/shifts/{id}/reopen` | Reabrir turno, accion admin. |

Reglas recomendadas:

- No permitir cerrar dos veces.
- No permitir ventas en turno cerrado.
- Reabrir debe requerir motivo.

## Ventas

| Metodo | Endpoint | Descripcion |
|---|---|---|
| `POST` | `/api/sales` | Crear venta. |
| `GET` | `/api/sales/{id}` | Obtener venta por id. |
| `GET` | `/api/sales/shift/{shiftId}` | Listar ventas de un turno. |
| `GET` | `/api/sales/store/{storeId}` | Listar ventas de un local. |
| `GET` | `/api/sales/store/{storeId}/date-range` | Listar ventas por local y rango de fechas. |
| `PUT` | `/api/sales/{id}` | Editar venta, accion admin. |
| `DELETE` | `/api/sales/{id}` | Anular venta, preferible `status=VOIDED`. |

Reglas recomendadas:

- `POST /api/sales` no debe aceptar precio final confiable desde frontend.
- Backend calcula precios y totales.
- `DELETE` no deberia borrar fisicamente.

## Reportes futuros

| Metodo | Endpoint | Descripcion |
|---|---|---|
| `GET` | `/api/reports/sales/daily` | Ventas diarias. |
| `GET` | `/api/reports/sales/by-product` | Ventas agrupadas por producto. |
| `GET` | `/api/reports/sales/by-store` | Ventas agrupadas por local. |
| `GET` | `/api/reports/sales/by-employee` | Ventas agrupadas por empleada. |
| `GET` | `/api/reports/inventory/stock-low` | Productos con stock bajo. |

Pendiente de validar:

- Parametros exactos de reportes.
- Exportacion a Excel/PDF.
- Filtros por fecha, local, categoria y empleada.

## Versionado

Pendiente de validar:

- Mantener `/api/...`.
- Introducir `/api/v1/...`.
- Mantener endpoints legacy mientras el frontend migra.

# Modelo de datos propuesto

Este modelo es una propuesta inicial. Debe validarse con el cliente antes de implementarse.

## InventoryCategory

Objetivo:

- Representar categorias y subcategorias infinitas por local.

Campos propuestos:

- `id`
- `store`
- `name`
- `description`
- `parent`
- `active`
- `createdAt`
- `updatedAt`

Relaciones:

- `Store 1:N InventoryCategory`
- `InventoryCategory N:1 InventoryCategory parent`
- `InventoryCategory 1:N InventoryCategory children`
- `InventoryCategory 1:N Product`

Pendiente de validar:

- Si las categorias son globales o por local.
- Si se necesita orden visual.

## Product

Objetivo:

- Representar un producto vendible o controlable por inventario.

Campos propuestos:

- `id`
- `store`
- `category`
- `name`
- `description`
- `sku`
- `price`
- `stockQuantity`
- `minimumStock`
- `active`
- `createdAt`
- `updatedAt`

Relaciones:

- `Store 1:N Product`
- `InventoryCategory 1:N Product`
- `Product 1:N SaleDetail`

Pendiente de validar:

- Si el SKU es obligatorio.
- Si el precio es por local o global.
- Si stock permite decimales.

## SalesShift

Objetivo:

- Representar turno abierto/cerrado de ventas por local.

Campos propuestos:

- `id`
- `store`
- `openedBy`
- `openedAt`
- `closedBy`
- `closedAt`
- `status`
- `expectedCashTotal`
- `actualCashTotal`
- `notes`
- `createdAt`
- `updatedAt`

Relaciones:

- `Store 1:N SalesShift`
- `SalesShift 1:N Sale`

Pendiente de validar:

- Si un local puede tener varios turnos abiertos.
- Si turno pertenece a empleada, local o dia.

## Sale

Objetivo:

- Representar una venta completa.

Campos propuestos:

- `id`
- `store`
- `shift`
- `employeeName`
- `saleDateTime`
- `totalAmount`
- `status`
- `voidReason`
- `voidedBy`
- `voidedAt`
- `createdAt`
- `updatedAt`

Relaciones:

- `SalesShift 1:N Sale`
- `Sale 1:N SaleDetail`
- `Store 1:N Sale`

Pendiente de validar:

- Si se usara empleado autenticado o nombre libre.
- Si la venta puede existir sin turno.

## SaleDetail

Objetivo:

- Representar cada producto vendido dentro de una venta.

Campos propuestos:

- `id`
- `sale`
- `product`
- `quantity`
- `unitPrice`
- `lineTotal`
- `productNameSnapshot`
- `createdAt`

Relaciones:

- `Sale 1:N SaleDetail`
- `Product 1:N SaleDetail`

Regla:

- `unitPrice` debe copiar el precio historico al momento de la venta.

## Futuro StockMovement

Objetivo:

- Auditar movimientos de stock.

Campos propuestos:

- `id`
- `store`
- `product`
- `movementType`
- `quantity`
- `previousStock`
- `newStock`
- `reason`
- `referenceType`
- `referenceId`
- `createdBy`
- `createdAt`

Tipos posibles:

- `SALE`
- `MANUAL_ADJUSTMENT`
- `RETURN`
- `INITIAL_LOAD`
- `CORRECTION`

Pendiente de validar:

- Si se implementa desde la primera version o en fase posterior.

## Futuro AuditLog

Objetivo:

- Registrar cambios sensibles.

Campos propuestos:

- `id`
- `entityType`
- `entityId`
- `action`
- `oldValue`
- `newValue`
- `reason`
- `changedBy`
- `changedAt`

Acciones a auditar:

- Editar venta.
- Anular venta.
- Reabrir turno.
- Cerrar turno.
- Ajustar stock.
- Cambiar precio.
- Desactivar producto.

## Futuro SalesClosing

Objetivo:

- Separar el evento de cierre del turno si el cierre requiere mas detalle.

Campos propuestos:

- `id`
- `shift`
- `store`
- `closedBy`
- `closedAt`
- `expectedTotal`
- `actualCashTotal`
- `difference`
- `notes`
- `status`

Pendiente de validar:

- Si se necesita conciliacion por metodos de pago.
- Si se requiere exportacion o impresion de cierre.

## Relaciones principales

| Relacion | Descripcion |
|---|---|
| `Store 1:N InventoryCategory` | Cada local tiene sus categorias. |
| `Store 1:N Product` | Cada local tiene sus productos/stock. |
| `InventoryCategory self-reference` | Categorias con subcategorias infinitas. |
| `InventoryCategory 1:N Product` | Una categoria agrupa productos. |
| `Store 1:N SalesShift` | Un local tiene muchos turnos. |
| `SalesShift 1:N Sale` | Un turno contiene ventas. |
| `Sale 1:N SaleDetail` | Una venta contiene lineas. |
| `Product 1:N SaleDetail` | Un producto puede aparecer en muchas ventas. |

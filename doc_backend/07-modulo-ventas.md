# Modulo Ventas

## Objetivo

El modulo Ventas debe permitir registrar ventas realizadas por empleadas/locales a partir del inventario disponible del local.

Debe generar datos confiables para:

- Cierres.
- Reportes.
- Control de stock.
- Correcciones administrativas.

## Flujo de empleada

Flujo propuesto:

1. La empleada se identifica o se loguea.
2. El sistema determina el local de trabajo.
3. La empleada ve productos activos del inventario de ese local.
4. La empleada carga cantidades vendidas.
5. El frontend envia producto y cantidad.
6. El backend consulta precios actuales.
7. El backend calcula subtotales y total.
8. El backend registra la venta.
9. El backend descuenta stock si la regla queda validada.

Pendiente de validar:

- Si habra login real de empleadas.
- Si una empleada puede trabajar en mas de un local.
- Si la empleada selecciona local manualmente o lo tiene asignado.

## Modelo recomendado

### Sale

Representa la venta completa.

Campos sugeridos:

- `id`
- `store`
- `shift`
- `employeeName` o `employeeUser`, pendiente de validar.
- `saleDateTime`
- `totalAmount`
- `status`
- `createdAt`
- `updatedAt`

Estados posibles:

- `ACTIVE`
- `VOIDED`
- `CANCELLED`

Pendiente de validar nombres definitivos.

### SaleDetail

Representa cada linea de producto vendida.

Campos sugeridos:

- `id`
- `sale`
- `product`
- `quantity`
- `unitPrice`
- `lineTotal`
- `productNameSnapshot`

## Precio historico

Regla recomendada:

- `SaleDetail.unitPrice` debe guardar el precio usado en la venta.
- Si el precio del producto cambia despues, la venta historica no debe cambiar.

Tambien se recomienda guardar:

- Nombre del producto al momento de la venta.
- Opcionalmente categoria al momento de la venta.

Esto evita que reportes historicos cambien por ediciones futuras.

## No confiar en precio enviado desde frontend

El frontend no debe decidir el precio final.

Regla:

- Frontend envia `productId` y `quantity`.
- Backend lee `Product.price`.
- Backend calcula subtotal y total.

Motivo:

- Evita manipulacion accidental o intencional.
- Asegura consistencia con inventario.
- Simplifica cambios de precio centralizados.

## Relacion con stock

Al crear venta, se deberia decidir si se descuenta stock.

Opcion recomendada:

- Descontar stock automaticamente al confirmar venta.

Pendiente de validar:

- Si se permite stock negativo.
- Que error devolver cuando no hay stock suficiente.
- Si admin puede forzar venta sin stock.
- Si debe haber reserva de stock antes de confirmar.

## Ventas en turnos

Recomendacion:

- Toda venta debe pertenecer a un `SalesShift` abierto.

Reglas:

- No permitir ventas en turno cerrado.
- No permitir ventas sin local.
- No permitir ventas con productos de otro local.

Pendiente de validar:

- Si inicialmente se permitiran ventas sin turno para acelerar entrega.

## CRUD administrativo de ventas

Por la tendencia del cliente a pedir control total, se recomienda disenar endpoints admin para:

- Consultar ventas.
- Editar venta, con restricciones.
- Anular venta.
- Revertir stock al anular, si aplica.
- Registrar motivo de correccion.

No se recomienda borrar fisicamente ventas historicas salvo decision explicita del cliente.

Preferencia:

- `status = VOIDED`
- `voidReason`
- `voidedBy`
- `voidedAt`

## Riesgos de compatibilidad

El modulo de ventas debe crearse separado de los endpoints actuales:

- No reutilizar `/transactions` como venta.
- No mezclar ventas con `ClosingDeposit`, `SupplierPayment` o `SalaryPayment`.
- Si una venta futura debe impactar balance financiero, hacerlo con regla explicita y documentada.

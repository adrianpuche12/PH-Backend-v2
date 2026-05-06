# Fases de implementacion

## Fase 0 - Documentacion y diseno

### Objetivo

Dejar claro el estado actual, vision futura, decisiones pendientes y riesgos antes de tocar codigo.

### Alcance

- Documentar backend actual.
- Documentar flujos actuales.
- Documentar deudas tecnicas.
- Proponer modelo futuro.
- Proponer endpoints.
- Identificar decisiones pendientes del cliente.

### No incluye

- Cambios Java.
- Migraciones de base de datos.
- Nuevos endpoints.
- Cambios de frontend.

### Criterios de aceptacion

- Carpeta `doc_backend/` creada.
- Documentacion en Markdown.
- Sin cambios de codigo.
- Working tree solo contiene docs nuevas.

## Fase 1 - Inventario base

### Objetivo

Crear estructura minima para categorias y productos por local.

### Alcance

- Entidad `InventoryCategory`.
- Entidad `Product`.
- Repositorios.
- Servicios.
- Controladores.
- CRUD admin.
- Listados por store y categoria.
- `active=false`.

### No incluye

- Descuento automatico por venta.
- Auditoria completa.
- Reportes.
- StockMovement avanzado.

### Criterios de aceptacion

- Admin puede crear, listar, editar y desactivar categorias.
- Admin puede crear, listar, editar y desactivar productos.
- Frontend puede consultar productos activos por local.
- No se rompen endpoints existentes.

## Fase 2 - Ventas base

### Objetivo

Permitir registrar ventas usando productos del inventario.

### Alcance

- Entidad `Sale`.
- Entidad `SaleDetail`.
- Endpoint `POST /api/sales`.
- Consulta por id, local y rango de fechas.
- Calculo backend de precios y totales.

### No incluye

- Turnos obligatorios, salvo decision validada.
- Descuento de stock si no esta confirmado.
- Reportes avanzados.
- Auditoria completa.

### Criterios de aceptacion

- Se puede crear venta con productos y cantidades.
- Backend calcula total.
- Precio historico queda guardado en detalle.
- No se acepta precio final confiable desde frontend.

## Fase 3 - Turnos y cierres

### Objetivo

Controlar ventas dentro de turnos abiertos y cerrados.

### Alcance

- Entidad `SalesShift`.
- Abrir turno.
- Consultar turno abierto por local.
- Cerrar turno.
- Reglas de estado `OPEN/CLOSED`.

### No incluye

- Conciliacion compleja.
- Multiples cierres por turno.
- Exportacion de cierre.

### Criterios de aceptacion

- Un local puede abrir turno segun regla definida.
- No se puede cerrar dos veces.
- No se pueden registrar ventas en turno cerrado.

## Fase 4 - Integracion con stock

### Objetivo

Conectar ventas con inventario para descontar stock.

### Alcance

- Descuento automatico al crear venta.
- Validacion de stock suficiente, si se confirma.
- Ajuste/reversion al anular venta.
- Endpoint de ajuste manual de stock.

### No incluye

- Kardex completo si no se implementa `StockMovement`.
- Reportes avanzados.

### Criterios de aceptacion

- Venta descuenta stock segun regla.
- Anulacion revierte stock si aplica.
- No se permite vender productos de otro local.

## Fase 5 - Administracion y auditoria

### Objetivo

Dar control administrativo amplio con trazabilidad.

### Alcance

- Reglas de correccion.
- Motivos obligatorios en acciones sensibles.
- `AuditLog`, si se aprueba.
- Estados `VOIDED`, `CANCELLED` o equivalentes.

### No incluye

- BI avanzado.
- Exportaciones complejas.

### Criterios de aceptacion

- Correcciones sensibles quedan registradas.
- No hay borrado fisico de historico.
- Admin puede corregir sin perder trazabilidad.

## Fase 6 - Reportes

### Objetivo

Crear reportes para administracion y control.

### Alcance

- Ventas diarias.
- Ventas por producto.
- Ventas por local.
- Ventas por empleada.
- Stock bajo.

### No incluye

- Dashboard frontend completo, salvo tarea separada.
- Exportaciones si no estan confirmadas.

### Criterios de aceptacion

- Reportes filtrables por fecha.
- Reportes filtrables por local.
- Datos consistentes con ventas, detalles y stock.

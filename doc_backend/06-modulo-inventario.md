# Modulo Inventario

## Objetivo

El modulo Inventario debe permitir administrar productos disponibles en cada local, con categorias, subcategorias, stock y precios.

Debe servir como base para el futuro modulo de Ventas.

## Inventario por local

Recomendacion inicial:

- Inventario por `Store`.
- Productos asociados a un local.
- Categorias asociadas a un local.

Motivo:

- Cada local puede tener productos distintos.
- Cada local puede manejar stock distinto.
- Cada local podria tener precios distintos.

Pendiente de validar:

- Si el cliente quiere catalogo global compartido.
- Si el precio debe ser global o por local.
- Si las categorias deben ser globales o por local.

## Categorias y subcategorias infinitas

Se recomienda una entidad `InventoryCategory` con autorreferencia:

- `parent`
- `children`

Esto permite:

- Categoria principal.
- Subcategoria.
- Subcategoria de subcategoria.
- Profundidad indefinida.

Ejemplo:

- Bebidas
  - Gaseosas
    - Latas
    - Botellas
  - Jugos
- Comida
  - Snacks
  - Platos preparados

## Productos

Cada producto deberia tener:

- Store/local.
- Categoria.
- Nombre.
- Descripcion opcional.
- SKU o codigo interno, pendiente de validar.
- Precio actual.
- Stock actual.
- Estado activo/inactivo.

## Stock

El stock inicial puede manejarse como campo en `Product`.

Campo sugerido:

- `stockQuantity`

Pendiente de validar:

- Si el stock permite decimales.
- Si hay unidades de medida.
- Si se necesita stock minimo.
- Si se necesita alerta de stock bajo.

## Precio

El precio actual deberia vivir en `Product`.

Regla importante:

- El modulo de ventas no debe confiar en el precio enviado por frontend.
- El backend debe leer el precio del producto al momento de vender.
- El precio historico debe copiarse en `SaleDetail`.

## Eliminacion logica

No se recomienda borrar fisicamente productos o categorias con historico.

Recomendacion:

- `active = false`

Esto permite:

- Ocultar productos del frontend operativo.
- Mantener ventas historicas.
- Evitar errores por FK.

## CRUD administrativo

El admin deberia poder:

- Crear categorias.
- Editar categorias.
- Desactivar categorias.
- Reordenar o cambiar parent, pendiente de validar.
- Crear productos.
- Editar productos.
- Cambiar precio.
- Ajustar stock.
- Desactivar productos.

## Ajustes futuros de stock

En una primera fase puede bastar con `PATCH /stock`.

En una fase posterior conviene crear `StockMovement` para registrar:

- Ajuste manual.
- Venta.
- Devolucion.
- Correccion.
- Inventario fisico.

Cada movimiento deberia registrar:

- Producto.
- Store.
- Cantidad.
- Tipo.
- Motivo.
- Usuario.
- Fecha.

## Relacion con Ventas

Cuando se cree una venta:

1. Frontend envia productos y cantidades.
2. Backend consulta productos activos del local.
3. Backend calcula precios y totales.
4. Backend descuenta stock si la regla queda aprobada.
5. Backend registra `Sale` y `SaleDetail`.

Pendiente de validar:

- Si la venta descuenta stock automaticamente.
- Si se permite vender sin stock suficiente.
- Si se permite stock negativo.

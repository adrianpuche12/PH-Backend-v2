# Reglas de CRUD administrativo

## Principio general

El cliente suele pedir control total sobre los datos. Por eso, cada modulo nuevo debe pensarse con administracion amplia desde el diseno, incluso si el primer frontend solo usa una parte.

## Separar acciones de empleadas y admin

### Empleadas

Las empleadas cargan datos operativos:

- Ventas.
- Cierres.
- Formularios diarios.
- Cantidades vendidas.

Regla:

- La empleada deberia tener flujos simples y guiados.
- No deberia poder modificar historico sensible sin autorizacion.

### Admin

El admin puede:

- Consultar.
- Corregir.
- Editar.
- Anular.
- Desactivar.
- Reabrir, si la regla queda aprobada.

Regla:

- Toda accion sensible de admin debe ser trazable.

## CRUD completo

Para cada recurso importante, disenar:

- `POST` para crear.
- `GET` para listar.
- `GET /{id}` para detalle.
- `PUT /{id}` para editar.
- `DELETE /{id}` para anular/desactivar.
- `PATCH` para acciones especificas cuando corresponda.

Recursos importantes:

- Categorias.
- Productos.
- Ventas.
- Turnos.
- Cierres.
- Stores.

## No borrar fisicamente historico

No borrar fisicamente datos historicos salvo decision explicita del cliente.

Preferir:

- `active=false` para catalogos.
- `status=CANCELLED` para operaciones canceladas.
- `status=VOIDED` para ventas anuladas.
- `status=CLOSED` para turnos cerrados.

Motivo:

- Preserva reportes.
- Evita errores por relaciones.
- Permite auditoria.

## Correcciones sensibles

Toda correccion sensible deberia guardar:

- Usuario que corrige.
- Fecha/hora.
- Motivo.
- Valor anterior.
- Valor nuevo.
- Entidad afectada.

Ejemplos:

- Cambiar precio.
- Cambiar stock.
- Editar venta.
- Anular venta.
- Reabrir turno.
- Cambiar cierre.

## Reglas para endpoints DELETE

`DELETE` puede mapear a eliminacion logica:

- Producto: `active=false`.
- Categoria: `active=false`.
- Venta: `status=VOIDED`.
- Turno: no borrar; usar estado o accion administrativa.

Pendiente de validar:

- Si el cliente exige borrado real para algun recurso sin historico.

## Compatibilidad con frontend actual

No cambiar:

- Rutas actuales.
- Campos actuales.
- Tipos de respuesta actuales.
- Semantica de filtros actuales.

Si un endpoint actual se reemplaza:

1. Crear endpoint nuevo.
2. Mantener endpoint viejo.
3. Migrar frontend.
4. Confirmar no uso.
5. Deprecar.
6. Eliminar solo con aprobacion.

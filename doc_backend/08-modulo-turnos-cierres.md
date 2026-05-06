# Modulo Turnos y Cierres

## Objetivo

El modulo Turnos/Cierres debe controlar el periodo operativo durante el cual se registran ventas en un local y permitir cerrar ese periodo con un responsable y totales.

## Concepto de turno

Un `SalesShift` representa un turno de trabajo o periodo de ventas para un local.

Campos sugeridos:

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

Estados sugeridos:

- `OPEN`
- `CLOSED`

Pendiente de validar:

- Si habra estado `REOPENED`.
- Si habra estado `CANCELLED`.

## Concepto de cierre

El cierre marca el final del turno y consolida ventas.

Informacion esperada:

- Responsable del cierre.
- Fecha/hora de cierre.
- Local.
- Total esperado en caja.
- Total contado real, pendiente de validar.
- Diferencia, pendiente de validar.
- Observaciones.

## Responsable del cierre

Debe registrarse quien cierra:

- Empleada.
- Admin.
- Usuario autenticado, si se implementa login.

Pendiente de validar:

- Si basta con nombre libre.
- Si se requiere usuario real autenticado.

## Reglas operativas

### No cerrar dos veces

Si un turno esta `CLOSED`, no se debe permitir cerrarlo otra vez.

Respuesta sugerida:

- HTTP `409 Conflict`

Pendiente de validar:

- Codigo y mensaje exactos.

### No permitir ventas en turno cerrado

Si una venta referencia un turno cerrado, el backend debe rechazarla.

Regla:

- Solo turnos `OPEN` aceptan ventas nuevas.

### Un turno abierto por local

Recomendacion inicial:

- Permitir un solo turno abierto por local.

Pendiente de validar:

- Si puede haber varios turnos simultaneos por local.
- Si cada empleada abre su propio turno.
- Si el turno pertenece al dia, al local o a la empleada.

## Correcciones administrativas futuras

El admin probablemente necesitara:

- Reabrir un turno.
- Corregir total contado.
- Anular cierre.
- Editar observaciones.
- Anular ventas dentro de turno cerrado.

Estas acciones deben auditarse.

Regla recomendada:

- No cambiar datos cerrados sin `reason`.
- Registrar `changedBy`, `changedAt` y valores anteriores.

## SalesClosing separado

Opcion futura:

- Crear entidad `SalesClosing` separada de `SalesShift`.

Cuando conviene:

- Si un turno puede tener varios cierres parciales.
- Si se requiere auditoria compleja del cierre.
- Si se necesita conciliacion contra caja, transferencias, tarjetas y efectivo.

Recomendacion inicial:

- Empezar con cierre dentro de `SalesShift`.
- Separar a `SalesClosing` si el cliente confirma necesidades de auditoria/conciliacion mayores.

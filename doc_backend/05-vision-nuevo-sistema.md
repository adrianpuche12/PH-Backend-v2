# Vision del nuevo sistema

## Objetivo

La evolucion del backend debe permitir crecer desde el sistema actual de formularios y administracion hacia un sistema operativo mas completo para locales.

El nuevo sistema deberia incluir:

- Modulo actual administrativo/formularios.
- Modulo Inventario.
- Modulo Ventas.
- Modulo Turnos/Cierres.
- Reportes futuros.
- Auditoria futura.

## Principio de compatibilidad

El backend actual ya esta en uso. Por lo tanto:

- No se deben eliminar endpoints existentes sin migracion.
- No se deben cambiar contratos JSON existentes sin validar frontend.
- No se deben modificar semanticas actuales si el cliente depende de ellas.
- Los nuevos modulos deben nacer separados de los flujos actuales.

## Store/local como base

`Store` debe seguir siendo la base para separar datos operativos:

- Inventario por local.
- Ventas por local.
- Turnos por local.
- Reportes por local.
- Permisos futuros por local.

Esto permite que cada local trabaje de forma independiente y que el admin consulte o corrija por local.

## Modulos nuevos separados

Los modulos futuros deberian tener paquetes, entidades, servicios y controladores propios.

Ejemplo conceptual:

- `inventory`
- `sales`
- `shifts`
- `reports`
- `audit`

Pendiente de validar:

- Si se mantendra package base `balance` o se reorganizara gradualmente.
- Si se versionaran endpoints con `/api/v1`.

## Reportes futuros

Los reportes deberian construirse sobre datos operativos confiables:

- Ventas por dia.
- Ventas por producto.
- Ventas por local.
- Ventas por empleada.
- Stock bajo.
- Diferencias entre cierre esperado y cierre real.

## Auditoria futura

Debido a que el cliente suele pedir correccion administrativa amplia, la auditoria deberia ser parte importante del diseno.

Acciones sensibles a auditar:

- Editar ventas.
- Anular ventas.
- Reabrir turnos.
- Cerrar turnos.
- Cambiar stock manualmente.
- Cambiar precios.
- Borrar o desactivar productos/categorias.

## Enfoque recomendado

El sistema deberia evolucionar en fases:

1. Documentar y validar.
2. Crear inventario base.
3. Crear ventas base.
4. Agregar turnos/cierres.
5. Integrar ventas con stock.
6. Agregar administracion y auditoria.
7. Crear reportes.

Este enfoque reduce riesgo y evita mezclar cambios nuevos con el flujo activo actual.

# Decisiones pendientes con el cliente

Estas preguntas deben validarse antes de cerrar el diseno definitivo de inventario, ventas y turnos.

## Usuarios y acceso

- ¿Habra login real de empleadas o solo se ingresara nombre?
- ¿Habra roles formales como admin y empleada?
- ¿Una empleada puede trabajar en mas de un local?
- ¿Una empleada tiene local asignado fijo?
- ¿El admin puede operar sobre todos los locales?

## Turnos y cierres

- ¿Un turno pertenece a empleada, local o dia?
- ¿Puede haber varios turnos abiertos al mismo tiempo por local?
- ¿Puede una empleada abrir mas de un turno?
- ¿Quien puede cerrar un turno?
- ¿El admin puede reabrir cierres?
- ¿Se necesita registrar motivo para reabrir o corregir cierre?
- ¿Se necesita conciliacion contra efectivo real contado?
- ¿Se necesita impresion/exportacion de cierre?
- ¿Se necesita separar `SalesClosing` de `SalesShift`?

## Ventas

- ¿La venta descuenta stock automaticamente?
- ¿Se permite stock negativo?
- ¿Que debe pasar si el stock es insuficiente?
- ¿El admin puede editar ventas cerradas?
- ¿El admin puede anular ventas?
- ¿Anular una venta debe devolver stock?
- ¿Se debe registrar motivo de correccion?
- ¿Se registran metodos de pago?
- ¿Se permiten descuentos?
- ¿Se permiten devoluciones?

## Inventario

- ¿Productos compartidos entre locales o inventario independiente?
- ¿Precio por local o precio global?
- ¿Categorias globales o por local?
- ¿Se necesita SKU/codigo interno?
- ¿Se necesita codigo de barras?
- ¿Stock permite decimales?
- ¿Se necesitan unidades de medida?
- ¿Se necesita stock minimo?
- ¿Se necesitan alertas de stock bajo?
- ¿Se necesita historial de movimientos de stock desde primera version?

## Administracion y auditoria

- ¿Se necesita auditoria desde la primera version?
- ¿Que acciones deben auditarse obligatoriamente?
- ¿El admin puede borrar fisicamente algun dato?
- ¿Debe existir papelera o recuperacion?
- ¿Los cambios administrativos requieren motivo obligatorio?

## Reportes

- ¿Que reportes son prioritarios?
- ¿Reportes por dia, semana, mes?
- ¿Reportes por local?
- ¿Reportes por empleada?
- ¿Reportes por producto/categoria?
- ¿Se necesita exportacion Excel?
- ¿Se necesita PDF?

## Compatibilidad frontend

- ¿Que endpoints usa actualmente el frontend?
- ¿Hay pantallas que dependan de `/transactions`?
- ¿Hay pantallas que dependan de `/api/operations/all`?
- ¿El frontend espera `@CrossOrigin("*")` en todos los controladores?
- ¿Hay clientes externos consumiendo la API?

## Produccion y despliegue

- ¿Cual es el entorno exacto de produccion?
- ¿Se usa profile `prod`, `dev` o `local` en despliegue real?
- ¿Se permite cambiar `ddl-auto=update`?
- ¿Hay backups automaticos?
- ¿Hay ventana de mantenimiento para migraciones?

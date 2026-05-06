# Contexto general

## Que hace actualmente la app

El backend `PH-Backend-v2` es un servicio Spring Boot usado para registrar y consultar operaciones de locales. Actualmente recibe datos desde formularios frontend y permite consultar, editar y borrar operaciones desde una vista administrativa.

Las operaciones actuales observadas incluyen:

- Depositos de cierres.
- Pagos a proveedores.
- Pagos de salarios.
- Gastos administrativos.
- Transacciones financieras directas.
- Locales o stores.

## Entorno activo

El backend ya esta en uso por un cliente en un entorno activo o productivo. Por ese motivo, cualquier cambio futuro debe tratarse como cambio sobre sistema vivo.

Implicaciones:

- No se debe romper compatibilidad con endpoints actuales sin plan de migracion.
- No se deben renombrar rutas, campos JSON o entidades usadas por el frontend sin confirmar impacto.
- Cualquier cambio en persistencia debe ser incremental y reversible en lo posible.
- Las migraciones deben estar documentadas y validadas antes de aplicarse.

## Roles observados

### Empleadas o locales

Las empleadas/locales parecen usar formularios operativos para cargar datos del dia o del local:

- Cierres.
- Pagos.
- Operaciones simples asociadas a un store.

No se observa autenticacion formal en el codigo revisado. El campo `username` existe en varias entidades, pero no se ve un modulo de login/autorizacion.

### Administrador

El administrador parece consultar informacion agregada y corregir datos:

- Lista operaciones combinadas.
- Filtra por fecha y local.
- Edita operaciones por tipo.
- Borra operaciones por tipo.
- Consulta transacciones y balance.
- Administra stores.

## Tendencia esperada del cliente

El cliente tiende a pedir control administrativo amplio. Esto significa que los modulos futuros deben pensarse con capacidades de correccion:

- Crear.
- Consultar.
- Editar.
- Desactivar.
- Anular.
- Reabrir o corregir cuando aplique.

## Criterio para APIs futuras

Las APIs futuras deben disenarse pensando en:

- CRUD administrativo completo.
- Separacion clara entre acciones de empleada y acciones de admin.
- Auditoria para cambios sensibles.
- Compatibilidad con el frontend actual.
- Datos historicos protegidos.
- Eliminacion logica antes que borrado fisico.

Toda decision no confirmada por el cliente debe quedar marcada como **pendiente de validar**.

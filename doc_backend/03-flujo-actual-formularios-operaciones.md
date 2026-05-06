# Flujo actual de formularios y operaciones

## Resumen

El flujo actual se divide en dos grandes grupos:

- Carga operativa desde formularios frontend.
- Consulta y correccion administrativa desde endpoints agregados.

El backend guarda algunas operaciones en tablas propias y otras como `Transaction`. En el caso especial de `GastoAdmin`, guarda en tabla propia y tambien genera transacciones financieras.

## Flujo de carga desde frontend

### 1. Frontend consulta stores

Endpoint probable:

- `GET /api/stores`

Uso:

- Obtener locales disponibles.
- Permitir que la empleada seleccione o trabaje sobre un local.
- Asociar formularios a `store_id`.

### 2. Empleadas/locales cargan formularios

Endpoints principales:

- `POST /api/forms/closing-deposits`
- `POST /api/forms/supplier-payments`
- `POST /api/forms/salary-payments`

Datos esperados:

- Monto.
- Fecha.
- Usuario o nombre de quien carga.
- Store/local.
- Campos especificos segun tipo de operacion.

### 3. Backend guarda operaciones

Cada formulario guarda una entidad JPA:

| Formulario | Entidad | Tabla |
|---|---|---|
| Deposito de cierre | `ClosingDeposit` | `closing_deposits` |
| Pago a proveedor | `SupplierPayment` | `supplier_payments` |
| Pago de salario | `SalaryPayment` | `salary_payments` |

Estas operaciones no parecen generar `Transaction` automaticamente.

### 4. Admin consulta operaciones

Endpoint principal:

- `GET /api/operations/all`

El backend usa `FormsService` para consultar varias tablas y convertirlas a `AllOperationsDTO`.

Operaciones incluidas actualmente en la vista unificada:

- `CLOSING`
- `SUPPLIER`
- `SALARY`

`GASTO_ADMIN` tiene constructor `fromGastoAdmin` en `AllOperationsDTO` y casos en update/delete, pero no se observa incluido en `FormsService.getAllOperations()`.

## GastoAdmin

Endpoint:

- `POST /api/forms/gasto-admin`

Flujo:

1. Recibe `GastoAdminRequestDTO`.
2. Valida que `porcentajeDanli + porcentajeParaiso = 100`.
3. Guarda un registro en `gasto_admin`.
4. Busca stores con IDs fijos:
   - `1L`: Danli.
   - `2L`: El Paraiso.
5. Divide el monto segun porcentajes.
6. Crea una `Transaction` por local si el porcentaje es mayor que cero.
7. Devuelve `GastoAdminResponseDTO` con las transacciones creadas.

Importante:

- No se detecta relacion directa FK entre `GastoAdmin` y las `Transaction` generadas.
- Editar o borrar un `GastoAdmin` no parece recalcular ni revertir automaticamente esas transacciones.

## OperationsController

Responsabilidad:

- Consultar todas las operaciones.
- Filtrar por fecha.
- Filtrar por store.
- Editar por tipo.
- Borrar por tipo.

Tipos soportados en update/delete:

- `CLOSING`
- `SUPPLIER`
- `SALARY`
- `GASTO_ADMIN`

DTO usado:

- `AllOperationsDTO`

Campos importantes del DTO:

- `id`
- `type`
- `amount`
- `date`
- `depositDate`
- `paymentDate`
- `salaryDate`
- `description`
- `username`
- `storeId`
- `storeName`
- `closingsCount`
- `periodStart`
- `periodEnd`
- `supplier`

## Transaction y balance financiero

La entidad `Transaction` se usa para movimientos financieros directos:

- `type`: `income` o `expense`.
- `amount`
- `date`
- `description`
- `store`

Endpoints principales:

- `POST /transactions`
- `GET /transactions`
- `GET /transactions/balance?startDate&endDate`
- `PUT /transactions/{id}`
- `DELETE /transactions/{id}`
- `GET /api/transactions/store/{storeId}`
- `GET /api/transactions/date-range-store?startDate&endDate&storeId`

El balance se calcula como:

- suma de `income`
- menos suma de `expense`

## Separacion actual por uso

### Uso probable por empleadas

- `GET /api/stores`
- `POST /api/forms/closing-deposits`
- `POST /api/forms/supplier-payments`
- `POST /api/forms/salary-payments`

### Uso probable por admin

- `GET /api/operations/all`
- `PUT /api/operations/{type}/{id}`
- `DELETE /api/operations/{type}/{id}`
- `GET /transactions`
- `GET /transactions/balance`
- CRUD de `/api/stores`
- Endpoints de filtros por store o fecha.

Esta separacion es inferida del codigo y debe validarse con el cliente y/o frontend.

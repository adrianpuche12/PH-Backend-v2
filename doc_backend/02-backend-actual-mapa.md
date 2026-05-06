# Backend actual - mapa

## Estructura del repositorio

Raiz del repositorio:

- `.git/`
- `.github/`
- `.idea/`
- `PH/`
- `.gitignore`
- `docker-compose.yml`
- `bash.exe.stackdump`

Carpeta principal del backend:

- `PH/pom.xml`
- `PH/Dockerfile`
- `PH/mvnw`
- `PH/mvnw.cmd`
- `PH/src/main/java/balance`
- `PH/src/main/resources`
- `PH/src/test/java/balance`

## Proyecto Spring Boot

Clase principal:

- `PH/src/main/java/balance/BalanceApplication.java`
- Package base: `balance`

Spring Boot escanea automaticamente `balance` y sus subpackages.

## Capas principales

### controller

Ubicacion:

- `PH/src/main/java/balance/controller`

Controladores detectados:

- `BalanceController`
- `DebugController`
- `FormsController`
- `OperationsController`
- `SalaryPaymentController`
- `StoreController`
- `SupplierPaymentController`
- `TestDbController`
- `TransactionController`

### service

Ubicacion:

- `PH/src/main/java/balance/service`

Servicios detectados:

- `BalanceService`
- `FormsService`
- `SalaryPaymentService`
- `StoreService`
- `SupplierPaymentService`
- `TransactionService`

### repository

Ubicacion:

- `PH/src/main/java/balance/repository`

Repositorios detectados:

- `ClosingDepositRepository`
- `GastoAdminRepository`
- `SalaryPaymentRepository`
- `StoreRepository`
- `SupplierPaymentRepository`
- `TransactionRepository`

### model

Ubicacion:

- `PH/src/main/java/balance/model`

Entidades JPA detectadas:

- `Store`
- `Transaction`
- `ClosingDeposit`
- `SupplierPayment`
- `SalaryPayment`
- `GastoAdmin`

### dto

Ubicacion:

- `PH/src/main/java/balance/dto`

DTOs detectados:

- `AllOperationsDTO`
- `GastoAdminRequestDTO`
- `GastoAdminResponseDTO`

## Modulos actuales

### Stores

Controlador:

- `StoreController`

Base endpoint:

- `/api/stores`

Responsabilidad:

- CRUD basico de locales.
- Provee locales al frontend para asociar operaciones.

### Forms

Controlador:

- `FormsController`

Base endpoint:

- `/api/forms`

Responsabilidad:

- Recibir formularios de cierre, pagos a proveedores, pagos de salario y gastos administrativos.
- Consultar operaciones especificas por fecha o en listas completas.

### Operations

Controlador:

- `OperationsController`

Base endpoint:

- `/api/operations`

Responsabilidad:

- Unificar varias operaciones en `AllOperationsDTO`.
- Permitir consulta, edicion y borrado por tipo.

### Transactions

Controladores:

- `BalanceController`
- `TransactionController`

Bases endpoint:

- `/transactions`
- `/api/transactions`

Responsabilidad:

- Registrar transacciones financieras directas.
- Consultar transacciones.
- Calcular balance.
- Filtrar transacciones por local y rango de fechas.

### Payments

Controladores:

- `SalaryPaymentController`
- `SupplierPaymentController`
- Tambien se manejan desde `FormsController`.

Responsabilidad:

- Consultas especificas de pagos por local.
- El CRUD general de estos controladores esta comentado y parece haberse movido o concentrado en `FormsController` y `OperationsController`.

### Debug/Test

Controladores:

- `DebugController`
- `TestDbController`

Bases endpoint:

- `/debug`
- `/test-db`

Responsabilidad:

- Diagnostico de conexion a base de datos.
- Prueba de escritura en tabla `test_db_info`.

Estos endpoints no deberian estar expuestos en produccion sin proteccion.

## Endpoints actuales conocidos

### `/api/forms`

- `POST /api/forms/closing-deposits`
- `GET /api/forms/closing-deposits/all`
- `GET /api/forms/closing-deposits?startDate&endDate`
- `GET /api/forms/closing-deposits/store/{storeId}`
- `POST /api/forms/supplier-payments`
- `GET /api/forms/supplier-payments/all`
- `GET /api/forms/supplier-payments?startDate&endDate`
- `POST /api/forms/salary-payments`
- `GET /api/forms/salary-payments`
- `POST /api/forms/gasto-admin`
- `GET /api/forms/gasto-admin/all`
- `GET /api/forms/gasto-admin?startDate&endDate`

### `/api/operations`

- `GET /api/operations/all`
- `GET /api/operations/all?startDate&endDate`
- `GET /api/operations/all?storeId`
- `GET /api/operations/all?startDate&endDate&storeId`
- `PUT /api/operations/{type}/{id}`
- `DELETE /api/operations/{type}/{id}`

### `/api/stores`

- `GET /api/stores`
- `GET /api/stores/{id}`
- `POST /api/stores`
- `PUT /api/stores/{id}`
- `DELETE /api/stores/{id}`

### `/api/transactions`

- `GET /api/transactions/store/{storeId}`
- `GET /api/transactions/date-range-store?startDate&endDate&storeId`

### `/transactions`

- `POST /transactions`
- `GET /transactions`
- `GET /transactions/balance?startDate&endDate`
- `PUT /transactions/{id}`
- `DELETE /transactions/{id}`

### `/debug`

- `GET /debug/datasource`

### `/test-db`

- `GET /test-db/info`
- `GET /test-db/test-write`

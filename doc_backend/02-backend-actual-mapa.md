# Backend V2 — Mapa completo de módulos

> **Actualizado:** 05-Jun-2026 — refleja estado real de producción

---

## Estructura `src/main/java/balance/`

```
balance/
├── config/
│   ├── CorsConfig.java              ← CORS global (allowedOriginPatterns)
│   ├── GlobalExceptionHandler.java  ← 400/409/500 + MissingParam + Validation
│   ├── UserStatusInterceptor.java   ← Bloquea requests de usuarios SUSPENDED
│   ├── WebConfig.java               ← Registra interceptor en /api/**
│   ├── DevDataSourceConfig.java
│   └── ProdDataSourceConfig.java
│
├── catalog/                         ← Catálogo de productos
│   ├── controller/
│   │   ├── CategoryController.java  ← GET/POST/PUT/DELETE /api/v2/categories
│   │   ├── ProductController.java   ← GET/POST/PUT/DELETE /api/v2/products
│   │   └── StoreV2Controller.java   ← GET/POST/PUT/DELETE /api/v2/stores
│   ├── dto/
│   │   ├── CategoryRequestDTO.java
│   │   ├── CategoryResponseDTO.java
│   │   ├── ProductRequestDTO.java
│   │   ├── ProductResponseDTO.java
│   │   ├── StoreRequestDTO.java     ← campo sourceStoreId (copiar catálogo)
│   │   └── StoreResponseDTO.java
│   ├── model/
│   │   ├── Category.java            ← árbol jerarquico, max profundidad 10
│   │   └── Product.java             ← SKU único por local, tipo SIMPLE/FABRICATED
│   ├── repository/
│   │   ├── CategoryRepository.java
│   │   └── ProductRepository.java
│   └── service/
│       ├── CategoryService.java     ← árbol en memoria (2 queries, evita N+1)
│       ├── ProductService.java      ← valida SKU único, init stock
│       └── StoreV2Service.java      ← cloneCatalog() al crear local
│
├── controller/                      ← Controllers legado V1 (operaciones)
│   ├── BalanceController.java       ← GET /api/balance
│   ├── FormsController.java         ← CRUD operaciones (cierre, proveedor, salario)
│   ├── OperationsController.java    ← GET /api/operations/all
│   ├── SalaryPaymentController.java ← GET /api/salary-payments/store/{id}
│   ├── SupplierPaymentController.java ← GET /api/supplier-payments/store/{id}
│   ├── StoreController.java         ← Legacy (usar StoreV2Controller)
│   └── TransactionController.java  ← GET /api/transactions/store/{id}
│
├── dashboard/
│   ├── controller/DashboardController.java ← GET /api/v2/dashboard
│   ├── dto/DashboardDTO.java, StoreDashboardDTO.java
│   └── service/DashboardService.java       ← KPIs por local
│
├── deposit/                         ← NUEVO: Depósitos bancarios
│   ├── controller/BankDepositController.java ← /api/v2/deposits
│   ├── dto/
│   │   ├── CreateDepositRequest.java
│   │   ├── DepositResponse.java
│   │   ├── PendingClosingResponse.java
│   │   └── PendingShiftResponse.java
│   ├── model/BankDeposit.java       ← status: PENDING/CONFIRMED/DISCREPANCY
│   ├── repository/BankDepositRepository.java
│   └── service/BankDepositService.java      ← reconciliación + marcar DEPOSITED
│
├── dto/                             ← DTOs compartidos
│   ├── AllOperationsDTO.java        ← agrega CLOSING/SUPPLIER/SALARY/GASTO_ADMIN
│   ├── GastoAdminRequestDTO.java
│   ├── GastoAdminResponseDTO.java
│   └── (otros legacy)
│
├── inventory/
│   ├── controller/InventoryController.java ← /api/v2/stores/{id}/stock
│   ├── dto/StockItemDTO.java, MovementDTO.java, StockAdjustmentDTO.java
│   ├── model/
│   │   ├── InventoryMovement.java   ← tipo: ENTRADA/SALIDA/AJUSTE/VENTA
│   │   └── InventoryStock.java      ← unique constraint (product_id, store_id)
│   ├── repository/
│   │   ├── InventoryMovementRepository.java
│   │   └── InventoryStockRepository.java
│   └── service/InventoryService.java ← adjust() lanza error, adjustSilent() loguea
│
├── model/                           ← Entidades legado V1
│   ├── ClosingDeposit.java          ← depositStatus: PENDING/DEPOSITED
│   ├── GastoAdmin.java
│   ├── SalaryPayment.java
│   ├── Store.java
│   ├── SupplierPayment.java
│   └── Transaction.java
│
├── repository/                      ← Repos entidades legado
│   ├── ClosingDepositRepository.java
│   ├── GastoAdminRepository.java
│   ├── SalaryPaymentRepository.java
│   ├── StoreRepository.java
│   ├── SupplierPaymentRepository.java
│   └── TransactionRepository.java
│
├── sales/                           ← POS: Turnos y Ventas
│   ├── controller/
│   │   ├── SalesController.java     ← /api/v2/shifts/{id}/sales, /closing
│   │   └── ShiftController.java     ← /api/v2/stores/{id}/shifts, /active/{id}
│   ├── dto/
│   │   ├── DailyClosingResponseDTO.java
│   │   ├── DailySummaryDTO.java     ← incluye totalCashSales, totalCardSales
│   │   ├── SaleItemDTO.java / SaleItemRequestDTO.java
│   │   ├── SaleRequestDTO.java      ← paymentMethod: CASH/CARD/MIXED
│   │   ├── SaleResponseDTO.java
│   │   └── ShiftResponseDTO.java
│   ├── model/
│   │   ├── Sale.java                ← status: OPEN/CONFIRMED; cashAmount, cardAmount
│   │   ├── SaleItem.java            ← snapshot de nombre y precio
│   │   ├── Shift.java               ← openingCashAmount, declaredCashAmount, difference
│   │   └── ShiftExpense.java        ← egresos del turno
│   ├── repository/
│   │   ├── SaleRepository.java
│   │   ├── SaleItemRepository.java
│   │   ├── ShiftRepository.java     ← queries derivadas Hibernate 6 compatible
│   │   └── ShiftExpenseRepository.java
│   └── service/
│       ├── SalesService.java        ← ISV=0, deductStock, closeShift+reconciliacion
│       └── ShiftService.java        ← generateCode HHmmss, openShift, closeShift
│
├── service/                         ← Servicios legado V1
│   ├── BalanceService.java
│   ├── FormsService.java            ← CRUD operaciones + saveGastoAdmin multi-local
│   ├── SalaryPaymentService.java
│   ├── SupplierPaymentService.java
│   ├── StoreService.java
│   └── TransactionService.java
│
├── storage/
│   ├── controller/StorageController.java ← POST /api/v2/upload
│   └── service/R2StorageService.java     ← Cloudflare R2 vía AWS S3 SDK
│
├── users/
│   ├── controller/AppUserController.java ← /api/v2/users (CRUD + suspend/activate)
│   ├── dto/AppUserRequestDTO.java, AppUserResponseDTO.java
│   ├── model/AppUser.java            ← status: ACTIVE/SUSPENDED, keycloakId
│   ├── repository/AppUserRepository.java ← findByUsername, findByKeycloakId
│   └── service/
│       ├── AppUserService.java       ← crea en KC + BD, suspend = KC disable + logout
│       └── KeycloakAdminService.java ← Admin REST API con cache de token 50s
│
└── BalanceApplication.java
```

---

## Endpoints principales `/api/v2/`

| Módulo | Prefijo | Operaciones |
|--------|---------|------------|
| Stores | `/api/v2/stores` | CRUD + toggle + delete (protegido si tiene historial) |
| Categories | `/api/v2/stores/{id}/categories` | CRUD árbol |
| Products | `/api/v2/stores/{id}/products` | CRUD + toggle + delete |
| Inventory | `/api/v2/stores/{id}/stock` | GET stock, ajuste, movimientos |
| Shifts | `/api/v2/stores/{id}/shifts` | Historial + filtros fecha/username |
| Sales | `/api/v2/shifts/{id}/sales` | CRUD ventas + cierre de turno |
| Dashboard | `/api/v2/dashboard` | KPIs por local |
| Users | `/api/v2/users` | CRUD + suspend/activate/reassign/reset-password |
| Deposits | `/api/v2/deposits` | Depósitos bancarios + pending-closings |
| Storage | `/api/v2/upload` | Upload a Cloudflare R2 |

---

## Tests: 314 en verde (Jun 2026)

Cobertura ~85%: catalog, sales, inventory, users, dashboard, deposit, storage, services financieros.

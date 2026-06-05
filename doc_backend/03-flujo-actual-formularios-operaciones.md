# Flujo de formularios y operaciones — V2

> **Actualizado:** 05-Jun-2026 — refleja implementación V2 real.

---

## 1. Flujo POS (Ventas del día)

```
Admin/Cajero → Abrir turno (openingCash=1000 default)
    → Registrar ventas (CASH/CARD/MIXED)
    → [Opcional] Registrar egresos del turno
    → Cerrar turno → declarar efectivo en mano
         → Sistema calcula:
              esperado = openingCash + ventas_efectivo - egresos
              diferencia = declarado - esperado
         → Crea ClosingDeposit (status=PENDING)
         → Admin ve flag "Sin depositar" en Operaciones
    → Admin hace depósito bancario (bank-deposit)
         → Selecciona cierres PENDING
         → Declara monto + comprobante (imagen obligatoria)
         → Sistema marca ClosingDeposits como DEPOSITED
         → Admin ve flag "Depositado" en Operaciones
```

### Entidades involucradas
- `Shift` → `Sale` → `SaleItem` → `InventoryMovement`
- `ShiftExpense` (egresos)
- `ClosingDeposit` (creado al cerrar turno)
- `BankDeposit` (creado al depositar)

---

## 2. Flujo de operaciones administrativas (legado V1 activo)

Los formularios de empleados crean registros en estas tablas:

| Tipo de operación | Tabla | Endpoint |
|-------------------|-------|---------|
| Cierre de caja | `closing_deposits` | `POST /api/closing-deposits` |
| Pago proveedor | `supplier_payments` | `POST /api/supplier-payments` |
| Pago salario | `salary_payments` | `POST /api/salary-payments` |
| Gasto administrativo | `gasto_admin` + `transactions` | `POST /api/admin-expenses` |
| Depósito bancario | `bank_deposits` | `POST /api/v2/deposits` |

### Vista admin — Operaciones
El admin ve todas las operaciones en `GET /api/operations/all`:
- Combina: `closing_deposits` + `supplier_payments` + `salary_payments` + `gasto_admin`
- Filtros: por fecha (startDate/endDate) y por local (storeId)
- Cada CLOSING muestra badge: "Depositado" (verde) o "Sin depositar" (amarillo)

---

## 3. Reconciliación al cierre de turno

```
expected = openingCashAmount + totalCashSales - totalShiftExpenses
declared = lo que el cajero cuenta en mano
difference = declared - expected

difference > 0 → sobra dinero (positivo)
difference < 0 → falta dinero (negativo)
difference = 0 → caja cuadrada
```

**Tarjeta:** no entra en la reconciliación física pero se muestra como información separada.

---

## 4. GastoAdmin — distribución multi-local

Al crear un Gasto Administrativo:
1. Se guarda registro en `gasto_admin`
2. Se crea 1 `Transaction` por cada local con monto proporcional al porcentaje
3. Los porcentajes deben sumar exactamente 100%

```
GastoAdmin $1000:
  Danli (60%) → Transaction expense $600
  El Paraíso (40%) → Transaction expense $400
```

---

## 5. Depósito bancario — flujo detallado

1. Cajero busca cierres PENDING de un local en un período
2. Ve lista de cierres con montos de efectivo acumulado
3. Ingresa monto que va a depositar + fecha + comprobante (foto)
4. Sistema crea `BankDeposit`:
   - `expectedCash` = suma montos de los cierres
   - `status` = CONFIRMED si no hay diferencia, DISCREPANCY si hay
5. Todos los `ClosingDeposit` incluidos pasan a `depositStatus=DEPOSITED`
6. En caso de DISCREPANCY, admin puede confirmar manualmente

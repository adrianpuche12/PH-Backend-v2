# Módulo de Depósitos Bancarios — Diseño técnico

## Resumen del problema

El sistema actual registra ventas pero no distingue el método de pago (efectivo vs
tarjeta). Esto impide saber cuánto dinero físico debe depositarse en el banco.

Cuando un empleado va al banco con dinero de varios días, necesita saber exactamente
cuánto efectivo recaudaron esos cierres, declarar cuánto deposita, y el sistema
debe calcular si hay diferencia positiva o negativa.

---

## Flujo completo

```
VENTA
  └── El cajero elige: Efectivo | Tarjeta | Mixto
        └── Si mixto: ingresa cuánto en efectivo y cuánto en tarjeta

CIERRE DE TURNO
  └── El sistema muestra: total ventas / total efectivo / total tarjeta

DEPÓSITO BANCARIO
  └── El empleado selecciona: qué cierres incluye (1 o varios locales)
  └── El sistema calcula: suma de efectivo de esos cierres = ESPERADO
  └── El empleado declara: cuánto va a depositar = DECLARADO
  └── El sistema muestra: diferencia = DECLARADO - ESPERADO
        └── Positiva (+): sobra dinero
        └── Negativa (-): falta dinero
        └── Cero: cuadra perfecto
  └── El empleado confirma el depósito (imagen opcional)
  └── Los cierres quedan marcados como "depositados"
```

---

## Cambios en el modelo de datos

### 1. Tabla `sales` — agregar método de pago

```sql
ALTER TABLE sales ADD COLUMN payment_method VARCHAR(10) NOT NULL DEFAULT 'CASH';
-- Valores: CASH | CARD | MIXED

ALTER TABLE sales ADD COLUMN cash_amount    DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE sales ADD COLUMN card_amount    DECIMAL(12,2) NOT NULL DEFAULT 0;
```

**Reglas:**
- `payment_method = CASH`  → `cash_amount = total`, `card_amount = 0`
- `payment_method = CARD`  → `cash_amount = 0`, `card_amount = total`
- `payment_method = MIXED` → `cash_amount + card_amount = total`

### 2. Tabla `shifts` (turnos/cierres) — agregar resumen de pagos

```sql
ALTER TABLE shifts ADD COLUMN total_cash_sales DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE shifts ADD COLUMN total_card_sales DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE shifts ADD COLUMN deposited        BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE shifts ADD COLUMN deposit_id       BIGINT NULL REFERENCES bank_deposits(id);
```

`deposited = true` cuando el cierre ya fue incluido en un depósito confirmado.
`deposited = false` cuando está pendiente de depositar.

### 3. Nueva tabla `bank_deposits` — depósitos bancarios

```sql
CREATE TABLE bank_deposits (
    id              BIGSERIAL PRIMARY KEY,
    store_ids       BIGINT[]  NOT NULL,          -- uno o varios locales
    created_by      VARCHAR(100) NOT NULL,       -- username del empleado
    deposit_date    DATE NOT NULL,
    shift_ids       BIGINT[] NOT NULL,           -- cierres incluidos
    expected_cash   DECIMAL(12,2) NOT NULL,      -- suma efectivo de los cierres
    declared_amount DECIMAL(12,2) NOT NULL,      -- lo que el empleado dice que deposita
    difference      DECIMAL(12,2) NOT NULL,      -- declared - expected
    status          VARCHAR(20) NOT NULL,        -- PENDING | CONFIRMED | DISCREPANCY
    image_uri       VARCHAR(512) NULL,           -- comprobante bancario (opcional)
    notes           TEXT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);
```

**Status:**
- `PENDING` → registrado pero no confirmado por admin
- `CONFIRMED` → admin validó el depósito
- `DISCREPANCY` → hay diferencia, requiere revisión

---

## Cambios en el backend

### Entidades Java nuevas/modificadas

**`Sale.java`** — agregar:
```java
@Column(name = "payment_method", length = 10)
private String paymentMethod = "CASH"; // CASH | CARD | MIXED

@Column(name = "cash_amount", precision = 12, scale = 2)
private BigDecimal cashAmount = BigDecimal.ZERO;

@Column(name = "card_amount", precision = 12, scale = 2)
private BigDecimal cardAmount = BigDecimal.ZERO;
```

**`Shift.java`** — agregar:
```java
@Column(name = "total_cash_sales", precision = 12, scale = 2)
private BigDecimal totalCashSales = BigDecimal.ZERO;

@Column(name = "total_card_sales", precision = 12, scale = 2)
private BigDecimal totalCardSales = BigDecimal.ZERO;

@Column(name = "deposited")
private Boolean deposited = false;

@ManyToOne
@JoinColumn(name = "deposit_id")
private BankDeposit deposit;
```

**`BankDeposit.java`** — nueva entidad:
```java
@Entity
@Table(name = "bank_deposits")
public class BankDeposit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_ids", columnDefinition = "bigint[]")
    private Long[] storeIds;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "deposit_date")
    private LocalDate depositDate;

    @Column(name = "shift_ids", columnDefinition = "bigint[]")
    private Long[] shiftIds;

    @Column(name = "expected_cash", precision = 12, scale = 2)
    private BigDecimal expectedCash;

    @Column(name = "declared_amount", precision = 12, scale = 2)
    private BigDecimal declaredAmount;

    @Column(name = "difference", precision = 12, scale = 2)
    private BigDecimal difference;

    @Column(name = "status", length = 20)
    private String status; // PENDING | CONFIRMED | DISCREPANCY

    @Column(name = "image_uri", length = 512)
    private String imageUri;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

### Endpoints nuevos

#### Ventas — método de pago
```
POST /api/v2/sales
  Body: { ..., paymentMethod: "CASH|CARD|MIXED", cashAmount: 0, cardAmount: 0 }

GET  /api/v2/shifts/{id}/summary
  Response: { ..., totalCashSales, totalCardSales, deposited }
```

#### Depósitos bancarios
```
GET  /api/v2/deposits
  Query: ?storeId=&status=&page=0&size=20
  Response: lista de depósitos

GET  /api/v2/deposits/pending-shifts
  Query: ?storeIds=1,2,3
  Response: lista de cierres no depositados con su totalCashSales

POST /api/v2/deposits
  Body: {
    storeIds: [1, 2],
    shiftIds: [10, 11, 12, 13],
    depositDate: "2026-05-17",
    declaredAmount: 5000.00,
    imageUri: null,
    notes: ""
  }
  Response: { id, expectedCash, declaredAmount, difference, status }

PUT  /api/v2/deposits/{id}/confirm
  Solo admin — cambia status a CONFIRMED

GET  /api/v2/deposits/{id}
  Detalle completo del depósito con los cierres incluidos
```

### Lógica de negocio

**Al cerrar un turno (`closeShift`):**
```java
// Calcular totales de efectivo y tarjeta de las ventas del turno
BigDecimal totalCash = sales.stream()
    .map(Sale::getCashAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal totalCard = sales.stream()
    .map(Sale::getCardAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

shift.setTotalCashSales(totalCash);
shift.setTotalCardSales(totalCard);
```

**Al crear un depósito:**
```java
// 1. Buscar los shifts por ID y verificar que no estén ya depositados
// 2. Sumar expectedCash = suma de totalCashSales de los shifts seleccionados
BigDecimal expectedCash = shifts.stream()
    .map(Shift::getTotalCashSales)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// 3. Calcular diferencia
BigDecimal difference = declaredAmount.subtract(expectedCash);

// 4. Determinar status
String status = difference.compareTo(BigDecimal.ZERO) == 0
    ? "CONFIRMED"
    : "DISCREPANCY";

// 5. Marcar los shifts como depositados
shifts.forEach(s -> {
    s.setDeposited(true);
    s.setDeposit(deposit);
});
```

---

## Cambios en el frontend

### 1. POSScreen — selector de método de pago

Al confirmar una venta, antes de cerrar el modal de confirmación:

```
┌─────────────────────────────┐
│  Confirmar venta            │
│  Total: L 450.00            │
│                             │
│  Método de pago:            │
│  [Efectivo] [Tarjeta] [Mixto]│
│                             │
│  Si MIXTO:                  │
│  Efectivo: L [___________]  │
│  Tarjeta:  L [___________]  │
│                             │
│  [Cancelar]  [Confirmar]    │
└─────────────────────────────┘
```

### 2. SalesHistoryScreen — mostrar desglose por método

En el resumen del turno mostrar:
```
Total ventas:    L 2,450.00
  Efectivo:      L 1,800.00
  Tarjeta:       L   650.00
Estado depósito: ⏳ Pendiente | ✅ Depositado
```

### 3. Nueva pantalla — Depósitos bancarios

Accesible desde el menú para el rol `user` (empleado) y `admin`.

**Paso 1 — Seleccionar cierres:**
```
Nuevo depósito

Local(es): [Danli ✓] [El Paraíso] [Miraflores]

Cierres pendientes de depósito:
┌──────────────────────────────────────────┐
│ ☐  17 May 2026 · Turno T-0002  L 850.00 │
│ ☐  16 May 2026 · Turno T-0001  L 920.00 │
│ ☐  15 May 2026 · Turno T-0003  L 680.00 │
│ ☐  14 May 2026 · Turno T-0004  L 740.00 │
└──────────────────────────────────────────┘
[Seleccionar todos]

Efectivo esperado: L 3,190.00
```

**Paso 2 — Declarar depósito:**
```
Efectivo esperado:  L 3,190.00
Monto a depositar:  L [__________]

Diferencia:         L 0.00  ✅ Cuadra

Comprobante (opcional): [📷 Agregar foto]
Notas:              [_____________________]

[Registrar depósito]
```

**Paso 3 — Confirmación:**
```
✅ Depósito registrado

Fecha:              17 May 2026
Cierres incluidos:  4
Efectivo esperado:  L 3,190.00
Declarado:          L 3,190.00
Diferencia:         L 0.00
Estado:             Cuadra ✅
```

### 4. AdminScreen — vista de depósitos

El admin ve todos los depósitos con su estado y puede confirmarlos:

```
Depósitos bancarios

[Todos] [Pendientes] [Con diferencia] [Confirmados]

┌─────────────────────────────────────────────────────┐
│ 17 May · gimena_alba · 4 cierres · L 3,190.00  ✅  │
│ 15 May · maria1234   · 2 cierres · L 1,820.00  ⚠️  │
│   Diferencia: -L 50.00                              │
└─────────────────────────────────────────────────────┘
```

---

## Pantallas afectadas

| Pantalla | Cambio |
|----------|--------|
| `POSScreen.tsx` | Agregar selector de método de pago al confirmar venta |
| `SalesHistoryScreen.tsx` | Mostrar desglose efectivo/tarjeta + estado depósito |
| `AdminScreen.tsx` | Nueva tab o sección para gestionar depósitos |
| `UserDashboard.tsx` | Agregar tab "Depósitos" al menú del empleado |
| Nueva: `DepositScreen.tsx` | Flujo completo de registro de depósito |

---

## Orden de implementación

### Fase 1 — Backend (semana 1)
1. Migración SQL: agregar columnas a `sales` y `shifts`, crear tabla `bank_deposits`
2. Actualizar entidades Java
3. Actualizar `SaleService` para recibir `paymentMethod`, `cashAmount`, `cardAmount`
4. Actualizar `ShiftService.closeShift()` para calcular totales efectivo/tarjeta
5. Crear `BankDepositService` y `BankDepositController`

### Fase 2 — Frontend POS (semana 1-2)
6. Agregar selector de método de pago en modal de confirmación de venta
7. Actualizar `SalesHistoryScreen` para mostrar desglose y estado depósito

### Fase 3 — Módulo de depósitos (semana 2)
8. Crear `DepositScreen.tsx` con el flujo de 3 pasos
9. Agregar al menú de usuario y admin
10. Vista admin de depósitos con confirmación

### Fase 4 — QA (semana 3)
11. Probar flujo completo: venta → cierre → depósito → confirmación
12. Probar casos borde: diferencia positiva, negativa, cero
13. Probar con múltiples locales
14. Deploy a DEV → QA → merge a PROD

---

## Reglas de negocio

- Un cierre solo puede estar en UN depósito (una vez depositado, no se puede
  volver a seleccionar)
- No se puede eliminar un depósito confirmado
- Un depósito en estado DISCREPANCY requiere nota explicativa
- El admin puede agregar notas o solicitar corrección en depósitos con diferencia
- Los depósitos se listan por local y fecha descendente
- Solo se pueden depositar cierres con status CLOSED (no abiertos)
- El `declared_amount` no puede ser negativo ni cero

---

## Impacto en módulos existentes

- **POS**: agregar 3 campos al body del POST de ventas (no rompe compatibilidad,
  tienen default CASH / 0 / 0)
- **Shifts**: los campos nuevos son nullable con default false/0, no rompe nada
- **AdminScreen**: se agrega una sección nueva, no se modifica la existente
- **V1**: no se toca

---

## Estimación

| Tarea | Tiempo estimado |
|-------|-----------------|
| Migración DB + entidades Java | 1 día |
| SaleService + ShiftService | 1 día |
| BankDepositService + Controller | 2 días |
| Frontend POS (método de pago) | 1 día |
| SalesHistoryScreen (desglose) | 0.5 días |
| DepositScreen (flujo completo) | 2 días |
| AdminScreen (vista depósitos) | 1 día |
| QA y ajustes | 1.5 días |
| **Total** | **~10 días hábiles** |

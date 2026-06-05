# Estado actual del sistema — Post Go-Live

> **Fecha go-live PROD:** 04-Jun-2026  
> **Última actualización:** 05-Jun-2026

---

## Sistema en producción

**URL:** `https://lospolloshermanos.org`  
**Cliente:** Humberto — locales de pollo frito en Honduras  
**Versión:** v2.2

### Credenciales admin entregadas al cliente
| Usuario | Contraseña |
|---------|-----------|
| `AdminPH-1988` | `Pollos1988#` |
| `AdminPH-1989` | `Pollos1989#` |
| `AdminPH-1990` | `Pollos1990#` |

---

## Funcionalidades en producción (v2.2)

### POS y Ventas
- Apertura y cierre de turno con fondo inicial (default 1000 Lps)
- Ventas con pago CASH / CARD / MIXED (validación efectivo+tarjeta=total)
- Reconciliación al cierre: fondo + efectivo ventas - egresos vs declarado
- Egresos del turno (ShiftExpense)
- Barra flotante del carrito en mobile
- Header y KPI bar colapsables en mobile

### Inventario
- Stock por local con alertas de mínimo
- Ajustes manuales (entrada/salida/ajuste)
- Historial de movimientos
- Categorías y subcategorías (árbol jerárquico)
- Productos con SKU único por local

### Operaciones (admin)
- Cierre de caja con flag "Depositado/Sin depositar"
- Depósito bancario con comprobante (imagen obligatoria)
- Pagos a proveedores, salarios, gastos administrativos
- Filtro por fechas (date range picker)
- Filtro por local (StoreDropdown)

### Usuarios
- CRUD completo con Keycloak integrado
- Suspensión con 3 capas de seguridad
- Reset de contraseña
- Reasignación de local

### Locales
- Crear local con opción de copiar catálogo de otro local
- Toggle activo/inactivo (protegido si tiene historial)

---

## Ambiente DEV

**URL:** `https://dev.lospolloshermanos.org`  
**Admin:** `admin-dev` / `Admin123456!`  
**Rama:** `develop`  
**Deploy:** automático en cada push a develop

---

## Versiones y changelog

### v2.2 — Jun 2026
- Errores inline en 7 modales (reemplaza snackbar negro)
- POS mobile: barra flotante carrito, header colapsable
- Flag depositado/pendiente en operaciones
- Fix filtro fechas historial ventas (Hibernate 6)
- Mensaje de espera en login para cold starts Railway
- Keepalive workflow (pinga Railway cada 10 min)

### v2.1 — Jun 2026
- 9 tareas demo cliente completadas
- StoreDropdown reemplaza chips horizontales
- DynamicFormScreen rediseñado con cards
- Suspensión 3 capas (Keycloak + AuthContext + Interceptor)
- Módulo bank-deposit
- 314 tests en CI (cobertura ~85%)
- Auditoría backend: 8 issues críticos corregidos

### v2.0 — May 2026
- Sistema completo en producción (Railway + Vercel)
- POS, Inventario, Dashboard, Usuarios implementados
- Auth con Keycloak (keycloak.belopia.app)
- Cloudflare R2 para comprobantes

---

## Pendientes y próximos pasos

### En revisión con cliente
- **Fondo fijo 1000 Lps:** el cliente quiere que siempre sean 1000 Lps sin declarar. Rama `feature/cliente-feedback-jun2026` tiene el cambio pendiente de aprobación.

### Planificado para sesión separada
- **Recetas / Productos manufacturados:** vender "Medio pollo" descuenta del "Pollo entero". Requiere tabla `product_recipes`, cambios en `SalesService.deductStock()`.

### Deuda técnica activa
- `ddl-auto=update` en DEV (puede perder datos en restart). Fix: migrar a Flyway.
- `BankDeposit.storeIds` y `shiftIds` guardados como CSV (antipatrón). Fix: tabla `bank_deposit_closings`.
- `DebugController` y `TestDbController` — candidatos a eliminar.

---

## Infraestructura completa

Ver `16-infraestructura-ambientes.md` para URLs, credenciales y CI/CD.

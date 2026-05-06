# Deudas tecnicas detectadas

Este documento lista riesgos y deudas observadas por lectura del codigo. No implica que todas deban resolverse inmediatamente. Varias requieren validacion con el cliente porque el backend ya esta en uso.

## GastoAdmin no parece incluido completamente en operaciones

`AllOperationsDTO` tiene metodo `fromGastoAdmin` y `OperationsController` permite `PUT` y `DELETE` para tipo `GASTO_ADMIN`.

Sin embargo, `FormsService.getAllOperations()`, `getOperationsByDateRange()`, `getOperationsByStore()` y `getOperationsByDateRangeAndStore()` agregan:

- `ClosingDeposit`
- `SupplierPayment`
- `SalaryPayment`

No se observa que agreguen `GastoAdmin`.

Riesgo:

- El admin podria no ver gastos administrativos en la vista unificada.
- Podria haber datos accesibles por endpoints especificos, pero no por el listado principal.

## GastoAdmin genera Transactions sin relacion FK directa

`saveGastoAdmin` crea registros en:

- `gasto_admin`
- `transactions`

Pero no se observa una relacion persistida entre el gasto administrativo y las transacciones generadas.

Riesgo:

- Si se edita un `GastoAdmin`, las `Transaction` originales quedan desactualizadas.
- Si se borra un `GastoAdmin`, las `Transaction` pueden quedar vivas.
- No hay trazabilidad confiable de origen.

## Store IDs hardcodeados

`saveGastoAdmin` asume:

- Store `1L` = Danli.
- Store `2L` = El Paraiso.

Riesgo:

- Si la base tiene otros IDs, el flujo falla.
- Si se crean nuevos locales, la logica no escala.
- Si se borra/recrea un store, cambia la semantica.

## Rutas legacy conviven

Existen dos bases para transacciones:

- `/transactions`
- `/api/transactions`

Riesgo:

- Confusion en frontend.
- Duplicacion de responsabilidades.
- Dificultad para versionar API.

## CRUDs comentados

Se observan CRUDs comentados en:

- `TransactionController`
- `SalaryPaymentController`
- `SupplierPaymentController`

Riesgo:

- El codigo no deja claro si son endpoints legacy, reemplazados o incompletos.
- Pueden confundir futuras implementaciones.

## Debug y test exponen informacion sensible

Endpoints detectados:

- `GET /debug/datasource`
- `GET /test-db/info`
- `GET /test-db/test-write`

Riesgo:

- Exponen URL, usuario, catalogo, schema y metadata de DB.
- `test-write` intenta insertar datos.
- No se observa proteccion por auth o profile.

## CORS disperso

Se detecta uso de:

- `@CrossOrigin(origins = "*")` en varios controladores.

Tambien existe:

- `PH/config/CorsConfig.java`

Ese archivo esta fuera de `src/main/java` y declara package `com.yourpackage.config`, mientras la app principal usa package `balance`.

Riesgo:

- La config global probablemente no se compila ni se detecta.
- CORS depende de anotaciones en controladores.
- Cambiar un controlador nuevo sin `@CrossOrigin` podria romper frontend.

## Falta autenticacion/autorizacion formal visible

No se observa modulo de seguridad, login, roles ni filtros de autorizacion.

Riesgo:

- Endpoints administrativos quedan disponibles si la red los expone.
- Empleadas y admin no estan diferenciados a nivel backend.
- No hay auditoria por usuario real.

## Pocos tests

Solo se observa estructura basica de test:

- `PH/src/test/java/balance/BalanceApplicationTests.java`

Riesgo:

- Cambios en compatibilidad del frontend no se detectan automaticamente.
- Cambios en fechas, DTOs o endpoints pueden romper flujos activos.

## Perfil activo y profiles

`application.properties` contiene:

- `spring.profiles.active=local`

Tambien existen:

- `application-dev.properties`
- `application-prod.properties`

Riesgo:

- Si no existe `application-local.properties`, puede haber comportamiento ambiguo segun variables disponibles.
- Puede haber confusion entre `local`, `dev` y `prod`.

## `ddl-auto=update`

Se observa:

- `spring.jpa.hibernate.ddl-auto=update`

Tambien aparece en perfiles `dev` y `prod`.

Riesgo:

- En produccion, Hibernate puede modificar schema automaticamente.
- Cambios de entidades pueden alterar tablas sin migracion explicita.
- Dificulta control fino de cambios de datos.

## `show-sql` y logging detallado

En `application.properties` y `application-dev.properties` se observa:

- `spring.jpa.show-sql=true`
- logging DEBUG amplio.

Riesgo:

- Logs excesivos.
- Posible exposicion de datos sensibles en logs.
- Dificulta operar en entorno activo.

## Validaciones y fechas

Algunas entidades requieren fecha no nula, pero los servicios intentan completar fechas con `LocalDate.now()` si faltan.

Ejemplo:

- `saveClosingDeposit`
- `saveSupplierPayment`
- `saveSalaryPayment`

Riesgo:

- Si la validacion ocurre antes del servicio, puede fallar antes de aplicar default.
- Si no falla, se podrian guardar fechas actuales sin que el usuario lo haya elegido explicitamente.

## Uso de `BigDecimal.ROUND_HALF_UP`

En `GastoAdmin` y `FormsService` se usa `BigDecimal.ROUND_HALF_UP`, API historica.

Riesgo:

- No es urgente, pero seria mejor usar `RoundingMode.HALF_UP` en futuras limpiezas.

## Comentarios de parche dentro del codigo

Hay comentarios como:

- `AGREGAR ESTOS IMPORTS`
- `MODIFICAR EL METODO`

Riesgo:

- Indican cambios pegados desde instrucciones previas.
- Dificultan lectura profesional del codigo.

## Sin auditoria de cambios

No se observa `AuditLog` ni registro de motivos para editar/borrar.

Riesgo:

- Admin puede corregir datos historicos sin trazabilidad.
- Dificulta responder preguntas del cliente sobre quien cambio que y cuando.

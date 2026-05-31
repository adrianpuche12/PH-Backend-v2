# Roadmap SaaS — De producto interno a plataforma vendible

Este documento describe qué se necesitaría para convertir el sistema Humberto V2
en un producto SaaS comercializable a otras empresas, incluyendo una app mobile
nativa en Expo.

**Estado actual:** producto interno para Pollos Hermanos (Honduras).
**Objetivo:** plataforma SaaS multi-tenant para restaurantes, cadenas y negocios
de retail en LATAM.

---

## 1. Arquitectura multi-tenant

El cambio más profundo y crítico. Hoy la app está hardcodeada para un solo cliente.

### Qué se necesita

- Agregar tabla `tenants` en la base de datos con los datos de cada empresa cliente
- Agregar columna `tenant_id` a todas las tablas principales (stores, products,
  transactions, users, etc.)
- Todos los queries deben filtrar por `tenant_id` — sin excepción
- Keycloak: un realm por tenant O un realm compartido con claim de tenant en el token
- R2: un bucket por tenant O prefijo de carpeta por tenant (ya tenemos la base)
- NeonDB: evaluar si un schema por tenant o una DB por tenant según volumen

### Riesgo si no se hace bien
Un bug de aislamiento puede hacer que un cliente vea datos de otro. Es el error
más crítico posible en un SaaS. Debe testearse exhaustivamente.

### Estimación
4 a 8 semanas de backend + 2 semanas de testing.

---

## 2. Panel de configuración por tenant

Hoy el logo, nombre, colores y configuración están hardcodeados en el frontend.
Cada cliente necesita su identidad visual y su configuración operativa.

### Qué se necesita

**Identidad visual:**
- Logo del negocio (subir a R2)
- Nombre de la empresa
- Color primario (para adaptar el design token `brand`)
- Subdomain propio (`mirestaurante.saas.com`)

**Configuración operativa:**
- ISV configurable por tenant (0% o 15% según país/decisión del negocio)
- Moneda configurable (Lempiras, Quetzales, Pesos, etc.)
- Zona horaria por tenant
- Nombre de los "locales" configurable (sucursal, sede, punto de venta)
- Roles personalizables (hoy solo admin/user)

**Panel de administración:**
- Vista super-admin (solo Nilo Solutions) para gestionar todos los tenants
- Vista tenant-admin para que cada cliente gestione sus propios usuarios y locales

### Estimación
3 a 5 semanas.

---

## 3. Facturación electrónica — SAR Honduras

**Bloqueante legal.** En Honduras la Ley de Facturación Electrónica (SAR) exige
que todo negocio emita facturas electrónicas válidas. Sin esto, el sistema no
puede operar legalmente para otro negocio.

### Qué se necesita

- Integración con el sistema SAR (Servicio de Administración de Rentas de Honduras)
- Generación de CAI (Código de Autorización de Impresión)
- Emisión de facturas en formato XML firmado digitalmente
- Generación de PDF de factura para el cliente final
- Almacenamiento de facturas emitidas (obligatorio por ley)
- Anulación de facturas con proceso formal

### Notas
Si se expande a otros países (Guatemala, El Salvador, Costa Rica), cada uno tiene
su propio sistema de facturación electrónica. Evaluar si el módulo es genérico o
por país.

### Estimación
6 a 10 semanas (incluye trámites con SAR y certificación).

---

## 4. Reportes y exportaciones

Los negocios necesitan reportes para contabilidad, inventario y decisiones.

### Qué se necesita

- Reporte de ventas por período (diario, semanal, mensual)
- Reporte de inventario (stock actual, movimientos, productos bajo mínimo)
- Reporte de operaciones y egresos
- Reporte de cierre de turno en PDF
- Exportación a Excel (ya existe base, ampliar)
- Dashboard con gráficos (ventas, ingresos vs egresos, productos más vendidos)
- Envío automático de reportes por email

### Stack sugerido
- PDF: iText (Java) o Jasper Reports
- Gráficos frontend: Victory Native o Recharts

### Estimación
4 a 6 semanas.

---

## 5. App mobile nativa en Expo

Hoy el frontend es una app web (React Native for Web). Para una experiencia mobile
real se necesita una app nativa publicada en App Store y Google Play.

### Qué se necesita

**Build y publicación:**
- Configurar EAS Build (Expo Application Services)
- Cuenta de desarrollador Apple ($99/año) y Google Play ($25 único)
- Proceso de revisión y aprobación en ambas tiendas (1 a 4 semanas cada una)
- Certificados de firma (iOS: provisioning profiles, Android: keystore)

**Funcionalidades específicas de mobile:**
- Push notifications para alertas de stock bajo, cierres pendientes, etc.
- Cámara nativa para fotos de comprobantes (hoy usa file picker web)
- Modo offline básico: poder registrar ventas sin internet y sincronizar al volver
- Biometría para login (Face ID / huella)
- App icon y splash screen por tenant (white-label)

**Separación de builds:**
- Un build por tenant (white-label) o un build genérico con login de empresa
- Evaluar: app genérica "SaaS" o apps personalizadas por cliente

**Permisos necesarios:**
- Cámara
- Almacenamiento (para guardar comprobantes temporalmente)
- Notificaciones push
- (Opcional) Ubicación para validar que el empleado está en el local

### Stack sugerido
- Expo SDK 52+ (ya en uso)
- EAS Build + EAS Submit para publicación
- Expo Notifications para push
- expo-camera para cámara nativa
- expo-sqlite o MMKV para persistencia offline

### Estimación
6 a 10 semanas (incluye tiempo de revisión de tiendas).

---

## 6. Onboarding automatizado para nuevos clientes

Hoy registrar un nuevo cliente requiere intervención manual (crear realm en Keycloak,
configurar DB, subir logo). Eso no escala.

### Qué se necesita

- Formulario de registro de empresa (nombre, RIF/RTN, logo, plan)
- Creación automática del tenant en DB
- Creación automática de usuario admin del tenant
- Email de bienvenida con credenciales
- Wizard de configuración inicial (locales, productos base, usuarios)
- Trial gratuito de 14 o 30 días automático

### Estimación
3 a 4 semanas.

---

## 7. Billing y planes

Sin cobro automatizado no hay SaaS real.

### Qué se necesita

- Integración con Stripe (acepta tarjetas en LATAM)
- Planes definidos (Free / Básico / Pro / Enterprise)
- Facturación mensual o anual automática
- Portal de cliente para gestionar su suscripción
- Límites por plan (número de usuarios, locales, productos)
- Suspensión automática si no paga

### Planes sugeridos

| Plan | Precio | Usuarios | Locales | Productos |
|------|--------|----------|---------|-----------|
| Free | $0 | 1 | 1 | 50 |
| Básico | $29/mes | 5 | 2 | 500 |
| Pro | $79/mes | 15 | 5 | Ilimitado |
| Enterprise | A convenir | Ilimitado | Ilimitado | Ilimitado |

### Estimación
3 a 5 semanas.

---

## 8. Seguridad y compliance

Un SaaS que maneja dinero y datos de clientes necesita un nivel de seguridad mayor.

### Qué se necesita

- HTTPS obligatorio en todos los ambientes (certificado SSL en servidor)
- Rate limiting en APIs
- Logs de auditoría (quién hizo qué, cuándo, desde dónde)
- Backup automático de DB (NeonDB ya lo tiene, verificar retención)
- Política de contraseñas fuerte por tenant
- 2FA opcional para admins
- Penetration testing básico antes del lanzamiento

### Estimación
3 a 4 semanas.

---

## 9. Soporte y documentación de usuario

Un producto sin soporte no se vende.

### Qué se necesita

- Manual de usuario (PDF o web)
- Videos tutoriales (Loom o YouTube privado)
- Sistema de tickets de soporte (Crisp, Intercom o similar)
- Base de conocimiento (preguntas frecuentes)
- SLA definido por plan (tiempo de respuesta garantizado)
- Canal de comunicación con clientes (WhatsApp Business, email)

### Estimación
2 a 3 semanas.

---

## Resumen de esfuerzo total

| Módulo | Estimación |
|--------|------------|
| Multi-tenant | 6 a 10 sem |
| Panel de configuración | 3 a 5 sem |
| Facturación SAR | 6 a 10 sem |
| Reportes y exportaciones | 4 a 6 sem |
| App mobile nativa (Expo) | 6 a 10 sem |
| Onboarding automatizado | 3 a 4 sem |
| Billing con Stripe | 3 a 5 sem |
| Seguridad y compliance | 3 a 4 sem |
| Soporte y documentación | 2 a 3 sem |
| **TOTAL** | **36 a 57 semanas** |

Trabajando en paralelo con 2 desarrolladores, el tiempo real sería de
**6 a 9 meses** para un producto SaaS completo y listo para vender.

---

## Orden de implementación recomendado

### Fase 1 — Base SaaS (meses 1 a 3)
Prioridad absoluta. Sin esto no hay producto.
1. Multi-tenant (arquitectura de datos)
2. Panel de configuración básico (logo, nombre, colores)
3. Onboarding automatizado
4. Billing con Stripe (al menos facturación básica)

### Fase 2 — Compliance y mobile (meses 3 a 6)
Necesario para operar legalmente y tener presencia mobile.
5. Facturación electrónica SAR
6. App mobile nativa en Expo (EAS Build)
7. Seguridad y HTTPS

### Fase 3 — Crecimiento (meses 6 a 9)
Para retener clientes y crecer.
8. Reportes y exportaciones
9. Push notifications
10. Modo offline
11. Soporte y documentación

---

## Modelo de negocio sugerido

**SaaS con implementación:**
- Cobro mensual por suscripción (Stripe)
- Fee de implementación único por cliente nuevo ($200 a $500)
- Soporte premium opcional

**Mercado objetivo inicial:**
- Restaurantes y cadenas de comida en Honduras y Guatemala
- Negocios con 1 a 10 locales
- Sin sistema actual o con Excel/sistemas obsoletos

**Diferenciador:**
- Único sistema en LATAM con facturación SAR Honduras integrada
- App mobile nativa + web en una sola plataforma
- Precio accesible vs sistemas ERP como SAP o Siigo

---

## Lo que ya existe y no hay que construir

El sistema actual ya tiene una base sólida:

- ✅ Backend Spring Boot + Java 21 (producción)
- ✅ Frontend React Native + Expo (web funcionando)
- ✅ Autenticación con Keycloak (roles admin/user)
- ✅ Módulo de ventas con POS
- ✅ Módulo de inventario con categorías y stock
- ✅ Módulo de operaciones (cierres, proveedores, salarios, gastos)
- ✅ Módulo de usuarios
- ✅ Almacenamiento de comprobantes en R2
- ✅ CI/CD con GitHub Actions
- ✅ Multi-ambiente (DEV/PROD)
- ✅ Design system con tokens
- ✅ Responsive web + mobile

Eso representa aproximadamente el **35 a 40%** del trabajo total para un SaaS.
La base técnica es sólida — no hay que tirar nada, solo expandir.

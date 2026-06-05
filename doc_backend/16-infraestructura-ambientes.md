# Infraestructura y Ambientes — Proyecto Humberto V2

> **Actualizado:** 05-Jun-2026
> **NOTA:** La infraestructura migró de Contabo a Railway + Vercel en mayo 2026. Referencias a Contabo son históricas.

---

## Arquitectura de ambientes

```
PROD  (master)   → lospolloshermanos.org        → Railway PROD + Vercel
DEV   (develop)  → dev.lospolloshermanos.org    → Railway DEV  + Vercel
LOCAL            → localhost:8081 / 8080         → IntelliJ / Expo
```

---

## URLs y acceso

| Ambiente | Frontend | Backend |
|----------|----------|---------|
| **PROD** | `https://lospolloshermanos.org` | Railway (master) |
| **DEV** | `https://dev.lospolloshermanos.org` | `https://ph-backend-dev-staging.up.railway.app` |
| **LOCAL** | `http://localhost:8081` | `http://localhost:8080` |

---

## Autenticación — Keycloak

**Servidor:** `https://keycloak.belopia.app`
**Admin KC:** `kc-admin` / `GeronimoAdmin2024!`

| Realm | Ambiente | client_id |
|-------|----------|-----------|
| `proyecto-h-prod` | PROD | `proyecto-h-client` |
| `proyecto-h-dev` | DEV + LOCAL | `proyecto-h-client` |

### Usuarios admin PROD (entregados al cliente)
| Usuario | Contraseña |
|---------|-----------|
| `AdminPH-1988` | `Pollos1988#` |
| `AdminPH-1989` | `Pollos1989#` |
| `AdminPH-1990` | `Pollos1990#` |

### Usuarios admin DEV / LOCAL
| Usuario | Contraseña |
|---------|-----------|
| `admin-dev` | `Admin123456!` |

### CORS — dominios permitidos en Keycloak
Al agregar nuevo dominio frontend, registrarlo en:
`keycloak.belopia.app` → realm → Clients → `proyecto-h-client` → WebOrigins y RedirectURIs

Dominios actuales registrados: `lospolloshermanos.org`, `dev.lospolloshermanos.org`, `localhost:8081`

---

## Base de datos — NeonDB

**Host:** `ep-red-water-ac3dna23-pooler.sa-east-1.aws.neon.tech`
**Usuario:** `neondb_owner`
**Password:** en `application-local.properties` (no commitear)

| Branch NeonDB | Ambiente |
|---------------|----------|
| `production` | PROD |
| `dev` (branch) | DEV + LOCAL |

---

## Storage — Cloudflare R2

- **Bucket:** `humberto-comprobantes` (Western Europe)
- **URL pública:** `https://pub-7e31005d201d4d34894758b2b1d00d9a.r2.dev`
- **Login Cloudflare:** jorgepuche02@gmail.com (Google)
- Folder DEV: `local/comprobantes/`
- Folder PROD: `prod/comprobantes/`

---

## CI/CD — GitHub Actions

### Backend (`PH-Backend-v2`)
```
push a develop → Unit Tests → Build Docker → Deploy Railway DEV
push a master  → Unit Tests → Build Docker → Deploy Railway PROD
```

### Frontend (`PH-Fronend-v2`)
```
push a develop/master → TypeScript check + Expo web build
Vercel auto-deploya directamente desde GitHub (integración nativa)
```

### Keepalive
`.github/workflows/keepalive.yml` — pinga Railway cada 10 min para evitar cold starts

---

## Levantar entorno LOCAL

```bash
# Backend (IntelliJ o terminal en Windows Git Bash)
cd "C:/Users/jorge/OneDrive/Desktop/PH-Backend-v2/PH"
./mvnw.cmd spring-boot:run -DskipTests
# → http://localhost:8080

# Frontend
cd "C:/Users/jorge/OneDrive/Desktop/PH-Fronend-v2"
npx expo start --web --port 8081
# → http://localhost:8081
```

Prerequisito: `application-local.properties` en `src/main/resources/` con:
- `spring.datasource.url` (NeonDB DEV)
- `keycloak.admin.*`
- `r2.*`

---

## Docker Hub

- Backend: `adrianpuche/humberto-v2-backend`
- Frontend: `adrianpuche/humberto-v2-frontend`
- Tags: `latest` (PROD) / `dev-latest` (DEV)

---

## Repos GitHub

- Backend: `adrianpuche12/PH-Backend-v2`
- Frontend: `adrianpuche12/PH-Fronend-v2`
- Estrategia: feature branches → develop → master

---

## Tests

```bash
cd PH && ./mvnw.cmd test
# Total: 314 tests, 0 fallas (Jun 2026)
# Corren automáticamente en cada push vía GitHub Actions
```

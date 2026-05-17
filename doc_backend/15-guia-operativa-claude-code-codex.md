# Guia operativa Claude Code + Codex

## 1. Proposito

Este documento define como Claude Code debe preparar tareas para Codex y como Codex debe ejecutarlas sin romper el backend `PH-Backend-v2`.

El objetivo es mantener un flujo de trabajo controlado, verificable y compatible con un backend que ya esta en uso por un cliente.

## 2. Principios del flujo

- Obsidian es la fuente de verdad de la documentacion.
- No se ejecutan tareas sin documentacion previa.
- Codex no decide alcance funcional.
- Codex ejecuta tareas concretas.
- Claude Code actua como analista, planner, generador de prompts y revisor.
- Git es la fuente de control de cambios.
- Toda tarea debe ser pequena, verificable y reversible.
- No se modifica codigo productivo sin rama dedicada.

## 3. Roles

### Claude Code

- Lee documentacion.
- Entiende el contexto.
- Divide el trabajo en tareas pequenas.
- Redacta prompts para Codex.
- Revisa diffs.
- Valida contra criterios de aceptacion.
- Documenta resultados.

### Codex

- Trabaja dentro del repo.
- Ejecuta instrucciones especificas.
- No inventa alcance.
- Verifica rama y estado antes de modificar.
- No hace commit salvo instruccion explicita.
- Reporta archivos modificados, comandos ejecutados y tests.

### Usuario/desarrollador

- Valida decisiones de negocio.
- Aprueba cambios.
- Ejecuta commits o PRs cuando corresponda.

## 4. Regla obligatoria antes de cada tarea Codex

Todo prompt para Codex que pueda modificar archivos debe iniciar con:

```text
Antes de modificar archivos, ejecuta:

git branch --show-current
git status --short --branch

Si no estas en la rama esperada o el working tree no esta limpio, detente.
```

Esta regla evita:

- Cambios accidentales en `master`.
- Cambios sobre trabajo sucio.
- Mezclar tareas distintas en el mismo diff.
- Perder control sobre que archivos pertenecen a cada tarea.

## 5. Ramas Git

Reglas:

- Nunca trabajar directamente en `master`.
- Crear ramas por tarea.
- Mantener nombres cortos y descriptivos.

Formato recomendado:

- `docs/nombre-corto`
- `codex/nombre-corto`
- `feature/nombre-corto`
- `fix/nombre-corto`

Ejemplos:

- `codex/test-instalacion`
- `docs/backend-map`
- `feature/inventory-base`
- `fix/cors-config`

Antes de iniciar una tarea nueva:

```bash
git checkout master
git pull
git checkout -b nombre-rama
```

## 6. Estructura estandar de prompt para Codex

Plantilla:

```text
Titulo:

Contexto:

Objetivo:

Alcance:

Archivos o modulos permitidos:

Restricciones:

Criterios de aceptacion:

Comandos recomendados:

Entregable esperado:
```

Ejemplo para documentacion:

```text
Titulo:
Documentar flujo actual de operaciones administrativas.

Contexto:
El backend PH-Backend-v2 ya esta en uso. Existe documentacion en doc_backend/.

Objetivo:
Crear un documento que explique como el admin consulta, edita y borra operaciones.

Alcance:
Solo documentacion Markdown.

Archivos o modulos permitidos:
doc_backend/

Restricciones:
No modificar codigo Java.
No modificar configuracion.
No hacer commit.

Criterios de aceptacion:
El documento explica endpoints actuales, entidades involucradas y riesgos.
El indice se actualiza si corresponde.

Comandos recomendados:
git status --short --branch

Entregable esperado:
Resumen de archivos creados/modificados y confirmacion de que no hubo commit.
```

## 7. Como limitar alcance

Frases obligatorias segun caso:

- No modifiques archivos fuera de...
- No cambies endpoints existentes.
- No cambies modelos actuales.
- No agregues dependencias sin justificar.
- No hagas commit.
- No ejecutes migraciones.
- No borres datos.
- No modifiques configuracion productiva.
- Si encuentras algo fuera de alcance, reportalo y detente.

## 8. Como pedir tareas de solo analisis

Cuando la tarea es de analisis, el prompt debe decir:

- No modifiques archivos.
- No hagas commit.
- No ejecutes comandos destructivos.
- Entrega mapa, riesgos y recomendaciones.
- Indica incertidumbres.

Ejemplo aplicado al backend actual:

```text
Analiza el flujo actual de formularios y operaciones.

No modifiques archivos.
No hagas commit.
No ejecutes comandos destructivos.

Revisa controladores, servicios, entidades, DTOs y repositorios.
Entrega mapa de endpoints, flujo de carga, flujo admin, riesgos y recomendaciones.
Indica cualquier incertidumbre como pendiente de validar.
```

## 9. Como pedir tareas de documentacion

Para documentacion:

- Solo crear o modificar archivos `.md`.
- Preferir `doc_backend/`.
- No tocar codigo.
- Actualizar indice si corresponde.
- No hacer commit.
- Mostrar arbol de archivos creados/modificados.

Ejemplo:

```text
Crea doc_backend/XX-nombre-documento.md.

Restricciones:
Solo Markdown dentro de doc_backend/.
No modificar codigo Java.
No modificar configuracion.
No hacer commit.

Al finalizar:
Ejecuta git status --short --branch.
Muestra archivos creados/modificados.
```

## 10. Como pedir tareas de implementacion

Para implementacion:

- Debe existir documentacion previa.
- Debe existir fase definida.
- Debe indicarse que modulos tocar.
- Debe indicarse que modulos NO tocar.
- Debe pedir tests o compilacion.
- Debe pedir resumen final.
- Debe impedir commits automaticos.

Ejemplo breve para futuro modulo Inventario:

```text
Implementa Fase 1 - Inventario base segun doc_backend/06-modulo-inventario.md y doc_backend/09-modelo-datos-propuesto.md.

Permitido:
Crear entidades, repositorios, servicios y controladores del modulo inventory.

No permitido:
No tocar endpoints actuales de /api/forms, /api/operations, /transactions.
No modificar Dockerfile.
No modificar configuracion productiva.
No hacer commit.

Criterios de aceptacion:
CRUD de categorias y productos funciona.
Productos tienen active=false para eliminacion logica.
Build pasa.
Tests relevantes pasan o se informa si no existen.
```

## 11. Como revisar una tarea terminada por Codex

Checklist:

- `git status --short --branch`
- `git diff --stat`
- `git diff`
- Revisar que solo se tocaron archivos esperados.
- Revisar criterios de aceptacion.
- Ejecutar tests si aplica.
- Verificar que no se modifico `master`.
- Verificar que no se tocaron secretos.
- Verificar que no se modifico configuracion productiva sin permiso.

## 12. Formato de entregable esperado de Codex

Al finalizar una tarea, Codex debe responder con:

- Rama actual.
- Estado de git.
- Archivos creados.
- Archivos modificados.
- Resumen de cambios.
- Comandos ejecutados.
- Tests ejecutados.
- Riesgos o pendientes.
- Confirmacion de que no hizo commit.

## 13. Reglas para commits

- Codex no hace commit salvo instruccion explicita.
- Primero se revisa diff.
- Luego el usuario o Claude Code puede proponer commit.

Mensajes recomendados:

- `docs: ...`
- `feat: ...`
- `fix: ...`
- `refactor: ...`
- `test: ...`

Ejemplo:

```text
docs: documentar mapa general del backend y modulos futuros
```

## 14. Errores comunes que se deben evitar

- Trabajar en `master`.
- Ejecutar Codex desde carpeta equivocada.
- Pedir tareas demasiado grandes.
- No definir criterios de aceptacion.
- Permitir que Codex decida arquitectura sin documentacion.
- Mezclar inventario, ventas y deudas tecnicas en una sola tarea.
- Tocar codigo y documentacion en la misma tarea sin necesidad.
- No revisar el diff.
- Hacer commit antes de validar.
- Modificar endpoints existentes sin confirmar compatibilidad frontend.

## 15. Reglas especificas de este proyecto

- El backend real esta dentro de `PH/`.
- La documentacion esta en `doc_backend/`.
- `Store`/local es entidad central.
- No romper endpoints actuales:
  - `/api/forms`
  - `/api/operations`
  - `/api/stores`
  - `/api/transactions`
  - `/transactions`
- Los modulos futuros deben nacer separados:
  - `/api/inventory`
  - `/api/sales`
  - `/api/reports`
- El cliente suele pedir CRUD administrativo amplio.
- Preferir eliminacion logica para historicos.
- No usar `Transaction` como detalle principal de ventas sin decision documentada.

## 16. Plantillas rapidas

### 1. Prompt de analisis

```text
Antes de analizar, ejecuta:

git branch --show-current
git status --short --branch

Si no estas en la rama esperada, detente.

Tarea de analisis, no modificacion.

Objetivo:
[describir que se debe entender]

Restricciones:
No modifiques archivos.
No hagas commit.
No ejecutes comandos destructivos.

Entregable:
Mapa tecnico.
Riesgos.
Recomendaciones.
Incertidumbres marcadas como pendiente de validar.
```

### 2. Prompt de documentacion

```text
Antes de modificar archivos, ejecuta:

git branch --show-current
git status --short --branch

Si no estas en la rama esperada o el working tree no esta limpio, detente.

Tarea:
Crear documentacion en doc_backend/.

Restricciones:
Solo crear o modificar archivos .md dentro de doc_backend/.
No modificar codigo Java.
No modificar configuracion.
No hacer commit.

Al finalizar:
Ejecuta git status --short --branch.
Muestra archivos creados/modificados.
Resume el contenido documentado.
```

### 3. Prompt de implementacion

```text
Antes de modificar archivos, ejecuta:

git branch --show-current
git status --short --branch

Si no estas en la rama esperada o el working tree no esta limpio, detente.

Tarea:
[describir implementacion]

Documentacion base:
[listar archivos doc_backend relevantes]

Permitido:
[listar modulos/archivos permitidos]

No permitido:
[listar modulos/archivos prohibidos]
No hacer commit.
No modificar configuracion productiva.
No ejecutar migraciones.

Criterios de aceptacion:
[listar criterios verificables]

Verificacion:
[build/tests/lint si aplica]

Entregable:
Archivos modificados.
Resumen.
Comandos ejecutados.
Tests ejecutados.
Riesgos o pendientes.
Confirmacion de que no hizo commit.
```

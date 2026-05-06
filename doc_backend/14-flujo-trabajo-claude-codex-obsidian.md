# Flujo de trabajo Claude Code, Codex y Obsidian

## Objetivo

Definir un proceso ordenado para evolucionar el backend sin romper el sistema activo del cliente.

## Obsidian como fuente de verdad

Obsidian debe ser la fuente de verdad del proyecto:

- Contexto del negocio.
- Decisiones validadas por cliente.
- Modelo de datos acordado.
- Endpoints acordados.
- Fases de implementacion.
- Criterios de aceptacion.
- Riesgos conocidos.

Todo cambio importante debe estar documentado antes de implementarse.

## Rol de Claude Code

Claude Code puede usarse para:

- Leer documentacion.
- Entender contexto.
- Convertir decisiones en tareas tecnicas.
- Generar prompts detallados para Codex.
- Revisar cambios propuestos.
- Comparar implementacion contra criterios de aceptacion.

Claude Code no debe asumir decisiones no validadas por el cliente.

## Rol de Codex

Codex ejecuta tareas sobre el repositorio:

- Inspecciona codigo.
- Hace cambios controlados.
- Ejecuta tests/builds cuando se pida.
- Resume cambios.
- No hace commit salvo instruccion explicita.

Antes de modificar archivos, Codex debe verificar:

- Rama actual.
- Estado del working tree.
- Alcance exacto de la tarea.

## Flujo recomendado

1. Documentar decision o necesidad en Obsidian.
2. Claude Code lee documentacion y arma tarea concreta.
3. La tarea incluye:
   - Objetivo.
   - Archivos permitidos.
   - Archivos prohibidos.
   - Criterios de aceptacion.
   - Comandos a ejecutar.
   - Restricciones de commits.
4. Codex verifica rama y working tree.
5. Codex implementa solo el alcance pedido.
6. Codex ejecuta verificaciones indicadas.
7. Codex resume cambios y riesgos.
8. Claude Code revisa contra documentacion.
9. Se actualiza Obsidian con resultado.

## Reglas

- No ejecutar tareas sin documentacion previa.
- Trabajar en ramas Git.
- Codex debe verificar rama y working tree antes de modificar.
- Codex no debe hacer commit salvo instruccion explicita.
- Cada tarea debe tener criterios de aceptacion.
- Cada cambio debe ser revisado contra documentacion.
- No tocar endpoints activos sin evaluar compatibilidad frontend.
- No borrar datos historicos sin decision explicita.
- No cambiar configuracion de produccion en tareas de feature.
- No mezclar refactors con features salvo aprobacion.

## Ramas

Recomendacion:

- Usar ramas dedicadas por tarea.
- Mantener nombres claros.
- Ejemplo: `codex/inventario-base`.

Para este repo, si una tarea exige rama especifica, Codex debe detenerse si no esta en la rama correcta.

## Commits

Regla:

- Codex no hace commit salvo instruccion explicita.

Cuando se pida commit:

- Usar Conventional Commits.
- Incluir solo archivos del alcance.
- Verificar estado antes y despues.

Ejemplos:

- `docs: add backend technical documentation`
- `feat: add inventory categories`
- `fix: preserve sale detail historical price`

## Revision

Cada cambio debe revisarse contra:

- Documento de contexto.
- Endpoint esperado.
- Modelo de datos esperado.
- Compatibilidad con frontend actual.
- Reglas CRUD administrativas.
- Decisiones pendientes.

Si una decision no esta confirmada, debe marcarse como:

- **pendiente de validar**

## Resultado esperado de cada tarea

Al finalizar, Codex debe informar:

- Que archivos cambio.
- Que no cambio.
- Que verificaciones ejecuto.
- Riesgos o decisiones pendientes.
- Si hizo o no commit.

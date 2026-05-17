# Indice de documentacion backend

Esta carpeta contiene documentacion tecnica del backend actual `PH-Backend-v2` y una propuesta inicial para su evolucion. Su objetivo es servir como base trasladable a Obsidian y como referencia para futuras tareas ejecutadas por Codex, Claude Code u otra herramienta de desarrollo.

Esta documentacion no modifica funcionalidades del backend. Solo describe el estado actual, riesgos, decisiones pendientes y una vision tecnica propuesta.

## Documentos

| Archivo | Proposito |
|---|---|
| `01-contexto-general.md` | Explica el contexto del sistema, roles de uso y criterio general de evolucion. |
| `02-backend-actual-mapa.md` | Mapea estructura del repositorio, modulos actuales, entidades y endpoints conocidos. |
| `03-flujo-actual-formularios-operaciones.md` | Describe como se cargan formularios, como se guardan operaciones y como consulta el admin. |
| `04-deudas-tecnicas-detectadas.md` | Lista deudas tecnicas y puntos de riesgo detectados en el backend actual. |
| `05-vision-nuevo-sistema.md` | Define la vision futura del sistema sin romper compatibilidad actual. |
| `06-modulo-inventario.md` | Propuesta del modulo de inventario por local, categorias, productos y stock. |
| `07-modulo-ventas.md` | Propuesta del modulo de ventas, detalles de venta y relacion con stock. |
| `08-modulo-turnos-cierres.md` | Propuesta para turnos, cierres, estados y reglas operativas. |
| `09-modelo-datos-propuesto.md` | Entidades futuras sugeridas y relaciones principales. |
| `10-endpoints-propuestos.md` | Endpoints propuestos para inventario, ventas, turnos y reportes. |
| `11-reglas-crud-administrativo.md` | Reglas para disenar CRUD amplio sin destruir historico. |
| `12-fases-implementacion.md` | Fases sugeridas para implementar el nuevo sistema gradualmente. |
| `13-decisiones-pendientes-cliente.md` | Preguntas que deben validarse con el cliente antes de cerrar diseno. |
| `14-flujo-trabajo-claude-codex-obsidian.md` | Flujo de trabajo recomendado entre Obsidian, Claude Code y Codex. |

## Alcance

La carpeta `doc_backend/` documenta:

- Backend actual.
- Flujo actual de formularios y operaciones administrativas.
- Transacciones y balance.
- Deudas tecnicas detectadas.
- Vision y diseno preliminar de modulos futuros.
- Reglas de trabajo para cambios controlados.

## Regla principal

Antes de modificar codigo en futuras tareas, esta documentacion debe revisarse y actualizarse si el cliente confirma cambios de alcance.

# Sistema de Misiones Diarias y Semanales

## Objetivo

Agregar un modulo `mission-system` para BigCasares con misiones diarias y semanales en espanol, seleccion aleatoria por jugador, interfaz mediante HUDs de contenedor abiertos por comando, y recompensas monetarias entregadas exclusivamente a traves de Vault.

El sistema debe servir para misiones humoristicas y raras del servidor, pero sin quedar hardcodeado a un set fijo. La implementacion inicial debe traer suficiente catalogo para aproximadamente un mes y medio de rotacion.

## Alcance

- Modulo nuevo dentro del sistema actual de `PluginModule`
- Integracion obligatoria con Vault y un proveedor de economia activo
- Seleccion aleatoria de misiones por jugador
- Pools separados para misiones diarias y semanales
- HUDs con inventarios tipo contenedor para lista, detalle y reclamo
- Comandos bajo `/bigcasares` o `/bc`
- Catalogo inicial de misiones en espanol configurable desde `config.yml`
- Persistencia local de asignaciones, progreso y recompensas reclamadas

## Fuera De Alcance

- NPCs, bloques del mundo o accesos fisicos
- Integracion con bases de datos externas
- Soporte para economia interna fallback
- Misiones compartidas globalmente por todo el servidor
- Editor visual in-game de misiones

## Enfoque Recomendado

Se implementara un motor de misiones declarativas. Cada mision se define por:

- `id`
- `scope`: `daily` o `weekly`
- `title`
- `description`
- `type`
- `goal`
- `reward`
- `params`
- `weight`
- `enabled`

El catalogo vivira en configuracion. El codigo no conocera misiones concretas; solo conocera tipos de mision y como rastrear su progreso. Esto deja el modulo customizable sin recompilar.

## Arquitectura

### 1. MissionModule

Responsable de:

- validar Vault al iniciar
- cargar configuracion
- inicializar catalogo de misiones
- inicializar almacenamiento de progreso
- registrar listeners
- registrar subcomandos
- registrar vistas HUD

Si Vault no esta instalado o no hay proveedor `Economy`, el modulo falla al habilitarse y se deja log explicito.

### 2. VaultEconomyGateway

Adaptador chico alrededor de `net.milkbowl.vault.economy.Economy`.

Responsable de:

- resolver el proveedor desde `ServicesManager`
- formatear montos
- depositar recompensas
- exponer errores claros si una transaccion falla

### 3. MissionCatalog

Carga y valida todas las misiones desde configuracion.

Responsable de:

- parsear definiciones
- validar tipos y parametros
- separar pools diarios y semanales
- exponer seleccion aleatoria filtrando por `enabled`

### 4. MissionTypeRegistry

Registro de handlers por tipo de mision.

Cada tipo define:

- como escuchar eventos relevantes
- como calcular progreso
- como validar completion
- como renderizar progreso textual en HUD

### 5. PlayerMissionService

Servicio principal de dominio.

Responsable de:

- asignar misiones nuevas cuando corresponda
- detectar vencimiento de rotacion
- actualizar progreso
- marcar misiones completadas
- marcar recompensas reclamadas
- evitar pagos duplicados

### 6. MissionStorage

Persistencia local en YAML.

Archivos propuestos:

- `plugins/BigCasares/data/missions/players/<uuid>.yml`

Contenido por jugador:

- misiones diarias activas
- misiones semanales activas
- progreso por mision
- timestamps de expiracion
- estado `completed`
- estado `claimed`

Se prioriza simplicidad y legibilidad por encima de optimizaciones prematuras.

### 7. MissionHudController

Controlador de inventarios GUI.

Vistas:

- `menu principal`
- `lista diarias`
- `lista semanales`
- `detalle de mision`
- `recompensas listas para reclamar`

La UI sera abierta solo por comando y navegable con clicks.

## Comandos

Todos cuelgan del comando existente `bigcasares`.

- `/bc misiones`
  - abre menu principal
- `/bc misiones diarias`
  - abre lista de diarias
- `/bc misiones semanales`
  - abre lista de semanales
- `/bc misiones reclamar`
  - intenta reclamar todas las recompensas listas
- `/bc misiones admin reroll <jugador>`
  - reasigna misiones del jugador
- `/bc misiones admin reset <jugador>`
  - reinicia progreso del jugador

Permisos nuevos propuestos:

- `bigcasares.missions.open`
- `bigcasares.missions.claim`
- `bigcasares.missions.admin`

## UX Del HUD

### Menu principal

Debe mostrar:

- acceso a diarias
- acceso a semanales
- cantidad completada / total
- recompensas pendientes
- tiempo restante hasta refresh

### Lista de misiones

Cada item del inventario debe mostrar:

- nombre en espanol
- descripcion
- progreso actual
- recompensa en dinero
- estado visual: pendiente, completada, reclamada

### Detalle

Debe mostrar:

- objetivo exacto
- progreso numerico o booleano
- recompensa
- categoria
- tiempo restante
- boton de reclamar si corresponde

## Rotacion

### Diarias

- cantidad por jugador configurable
- expiracion cada 24 horas desde asignacion o, preferentemente, por ventana fija configurable

### Semanales

- cantidad por jugador configurable
- expiracion cada 7 dias desde asignacion o, preferentemente, por ventana fija configurable

Decision de implementacion recomendada:

- usar ventanas globales fijas para diario y semanal
- la seleccion sigue siendo individual por jugador

Ventaja:

- comportamiento predecible
- menos edge cases
- UI mas clara al mostrar tiempo restante

## Tipos Iniciales De Mision

Se implementaran tipos suficientes para cubrir el catalogo inicial pedido:

- `crouch_on_sleeping_bed`
- `final_hit_player_with_item`
- `collect_exact_material_count`
- `rename_item_to_exact_name`
- `name_entity_after_online_or_known_player`
- `equip_specific_item`
- `stand_on_block_at_y`
- `kill_entity_with_item_only`
- `wax_block_count`
- `hold_exact_item_count`
- `place_block_count`
- `use_anvil_with_exact_result`
- `interact_while_wearing_item`

## Catalogo Inicial

Se entregara un catalogo base con aproximadamente:

- 45 misiones diarias
- 9 misiones semanales

El set incluira el estilo de ejemplos que pediste:

- agacharse 5 veces sobre la cama de un jugador durmiendo
- matar a un jugador con palo de madera en el golpe final
- conseguir exactamente 67 bloques de toba
- renombrar una azada de netherita a `Tel Aviv 2027`
- ponerle a un cerdo el nombre de un jugador
- colocarse una calabaza con maldicion de ligadura
- pararse sobre diorita en Y 155
- matar un iron golem usando solo kelp
- encerar 67 bloques de cobre

Se agregaran variantes suficientes para cubrir ~6 semanas sin repetir demasiado.

## Seguimiento De Progreso

### Event-driven

Las misiones se actualizan por listeners cuando el tipo lo permite:

- sneak toggles
- entity damage / death
- inventory interactions
- anvil prepare / click
- player interact
- block place
- waxing
- armor equip

### Revalidacion

Para objetivos basados en inventario exacto o estado actual, el sistema debe revalidar tambien al abrir HUD o reclamar, para no depender solo de un evento puntual.

Ejemplo:

- `tener exactamente 67 bloques de toba` no debe quedar desfasado si el jugador modifica el inventario por medios no cubiertos por un unico listener.

## Entrega De Recompensas

Al completar una mision:

- se marca `completed = true`
- no se entrega dinero automaticamente
- queda disponible para reclamar

Al reclamar:

- se llama `Economy#depositPlayer`
- si la respuesta falla, no se marca como reclamada
- si la respuesta es exitosa, se marca `claimed = true`

Esto evita dobles pagos y deja un estado visible en HUD.

## Configuracion

Se agregara una nueva seccion en `config.yml`:

- `modules.mission-system`
- `mission-system.daily.count`
- `mission-system.weekly.count`
- `mission-system.reset.daily-mode`
- `mission-system.reset.weekly-mode`
- `mission-system.reset.daily-hour`
- `mission-system.reset.weekly-day`
- `mission-system.reset.weekly-hour`
- `mission-system.hud.titles`
- `mission-system.rewards.currency-name-fallback`
- `mission-system.missions`

La lista de misiones debe ser editable y permitir desactivar entradas sin borrar codigo.

## Manejo De Errores

- si falta Vault: log y no habilitar modulo
- si falta proveedor de economia: log y no habilitar modulo
- si una mision esta mal configurada: log detallado y omitir solo esa entrada
- si un HUD no puede abrirse por estado inconsistente: mensaje corto al jugador y recuperacion al menu principal
- si falla el deposito: avisar al jugador y mantener recompensa pendiente

## Testing

Se agregaran tests unitarios para:

- carga de catalogo
- seleccion aleatoria filtrando misiones deshabilitadas
- asignacion de rotaciones por jugador
- expiracion diaria y semanal
- marcacion de completada y reclamada
- proteccion contra doble reclamo
- parseo de tipos concretos y parametros requeridos

No se intentara cubrir con unit tests toda la parte Bukkit GUI, pero si la logica de dominio y el contrato del almacenamiento.

## Riesgos

- algunas condiciones raras dependen de eventos finos de Bukkit y pueden requerir revalidaciones extras
- detectar "solo usando kelp" exige rastrear el arma efectiva del dano final con cuidado
- nombres exactos de items y entidades necesitan comparacion estricta y mensajes claros
- la persistencia YAML por jugador es suficiente para este alcance, pero no para analitica pesada o clusters

## Plan De Implementacion Esperado

1. agregar dependencia `VaultAPI` y `softdepend` correspondiente
2. crear esqueleto del modulo y gateway de economia
3. crear modelos de mision, catalogo y almacenamiento
4. agregar tests de dominio para asignacion, expiracion y reclamo
5. implementar tipos de mision base
6. implementar listeners y actualizacion de progreso
7. implementar HUDs de inventario y subcomandos
8. poblar config con el catalogo inicial en espanol
9. verificar compilacion y tests

## Criterios De Aceptacion

- el servidor no habilita `mission-system` sin Vault operativo
- cada jugador recibe misiones diarias y semanales propias
- todo se usa por comandos
- la interfaz usa inventarios tipo contenedor
- las recompensas se depositan por Vault
- el texto visible al jugador esta en espanol
- el catalogo inicial cubre aproximadamente un mes y medio
- nuevas misiones pueden agregarse via configuracion usando tipos existentes

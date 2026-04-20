# Sistema de Bounties PvP

## Objetivo

Agregar un modulo `bounty-system` para BigCasares que cree y mantenga bounties automáticos entre jugadores a partir de muertes PvP.

Cuando un jugador muere a manos de otro jugador, el sistema debe:

- pagar al killer cualquier bounty acumulada que ya tuviera la victima
- volver a calcular una nueva bounty sobre la victima usando su balance restante
- sacar una parte del dinero del servidor para generar bleed economico

La intencion es crear un loop PvP donde matar a un jugador rico genera riesgo persistente, recompensas para cazadores, y drenaje parcial de capital para evitar inflacion.

## Alcance

- Modulo nuevo dentro del sistema actual de `PluginModule`
- Integracion obligatoria con Vault y un proveedor de economia activo
- Creacion automatica de bounty solo en muertes jugador vs jugador
- Persistencia local de bounties activas por jugador
- Acumulacion de bounties en stack sobre el mismo jugador
- Pago automatico de la bounty al killer cuando mata al objetivo
- Nuevo calculo de bounty sobre la misma victima en la misma muerte, usando su balance restante
- Configuracion de porcentajes y minimos desde `config.yml`
- Mensajeria basica para killer y victima
- HUDs, menus o GUI para listar bounties
- Comandos administrativos o de jugador para gestionar bounties manualmente

## Fuera De Alcance

- Rankings, leaderboards o top de cazadores
- Integracion con bases de datos externas
- Economia fallback sin Vault
- Bounties sobre mobs o muertes no PvP

## Reglas Confirmadas

- Las bounties son acumulativas: si un jugador ya tenia bounty activa, una nueva muerte PvP suma mas valor a esa misma bounty.
- Si una victima tenia bounty activa y muere a manos de otro jugador, esa misma kill paga la bounty existente al killer.
- Luego de pagar la bounty existente, esa misma muerte genera una nueva bounty sobre la victima usando el balance restante.
- Split inicial de economia:
  - `30%` del balance elegible se retira al morir
  - `20%` del balance elegible pasa a bounty activa
  - `10%` del balance elegible se elimina del sistema

## Enfoque Recomendado

Se implementara un modulo dedicado `bounty-system`, separado de `mission-system`, pero reutilizando la misma integracion con Vault.

La separacion es importante porque:

- las misiones y las bounties no comparten dominio
- el sistema de bounties necesita persistencia y reglas PvP propias
- mezclarlo con `MissionModule` haria mas dificil testear, balancear y extender el plugin

## Arquitectura

### 1. BountyModule

Responsable de:

- validar Vault al iniciar
- cargar la configuracion del sistema de bounties
- inicializar almacenamiento de bounties
- inicializar el servicio de dominio
- registrar listeners PvP

Si Vault no esta instalado o no hay proveedor `Economy`, el modulo falla al habilitarse y se deja log explicito.

### 2. BountyEconomyGateway

Adaptador chico alrededor de `net.milkbowl.vault.economy.Economy`.

Se implementara como una clase nueva enfocada en bounties, reutilizando solo la resolucion de Vault ya conocida del proyecto como referencia de comportamiento.

Responsable de:

- resolver el proveedor desde `ServicesManager`
- consultar balance de un jugador
- retirar dinero del jugador al morir
- depositar la bounty al killer
- formatear montos para mensajes
- exponer errores claros si una transaccion falla

### 3. BountyStorage

Persistencia local en YAML.

Archivos propuestos:

- `plugins/BigCasares/data/bounties/players/<uuid>.yml`

Contenido por jugador:

- `active-bounty`
- `updated-at`

El almacenamiento solo persiste la bounty activa actual. No intenta guardar historial ni ultimos killers.

### 4. BountyService

Servicio principal de dominio.

Responsable de:

- leer la bounty actual de una victima
- pagar al killer la bounty previa si existe
- consultar el balance restante de la victima
- calcular cuanto se retira, cuanto se convierte en bounty y cuanto se destruye
- retirar el total configurado de la victima
- acumular la nueva bounty sobre la victima
- devolver un resultado estructurado para mensajes y logging

La logica economica no debe vivir en listeners Bukkit. Los listeners solo detectan el evento y delegan.

### 5. BountyListener

Listener de `PlayerDeathEvent`.

Responsable de:

- ignorar muertes sin killer jugador
- ignorar casos donde victima o killer no sean validos para la operacion
- delegar al `BountyService`
- enviar mensajes al killer y a la victima segun el resultado

## Flujo De Eventos

En una muerte PvP:

1. El listener obtiene victima y killer.
2. Se carga la bounty activa actual de la victima.
3. Si la bounty es mayor a cero, se deposita completa al killer.
4. La bounty activa de la victima se reinicia temporalmente a cero.
5. Se consulta el balance restante de la victima despues del payout.
6. Se calcula el monto elegible para retiro segun configuracion.
7. Si el balance restante no alcanza el minimo configurable, no se crea nueva bounty.
8. Se retira del jugador el total configurado para el evento.
9. La porcion destinada a bounty se suma a la bounty activa de la victima.
10. La porcion de bleed se descarta del sistema.
11. Se persiste la nueva bounty activa de la victima.
12. Se notifican payout y nueva bounty.

## Modelo Economico

### Definiciones

- `eligible-balance`: balance actual de la victima despues de pagar bounty previa
- `take-percent`: porcentaje total retirado de la victima por la muerte
- `bounty-percent`: porcentaje del balance elegible que se agrega a la bounty
- `bleed-percent`: `take-percent - bounty-percent`

### Defaults

- `take-percent = 0.30`
- `bounty-percent = 0.20`
- `bleed-percent = 0.10`

### Restricciones

- `take-percent` debe ser mayor que `0`
- `bounty-percent` debe ser mayor o igual que `0`
- `bounty-percent` no puede ser mayor que `take-percent`
- `minimum-balance` define desde que balance una victima puede generar nueva bounty
- `minimum-bounty` define un piso opcional para no crear bounties triviales

### Formula Inicial

Con `balance = eligible-balance`:

- `total-taken = balance * take-percent`
- `new-bounty = balance * bounty-percent`
- `economic-bleed = total-taken - new-bounty`

Si `new-bounty < minimum-bounty`, el sistema no crea nueva bounty y no deberia retirar dinero para ese refresh, porque no tiene sentido destruir economia sin generar objetivo PvP util.

## Persistencia

Se propone un archivo YAML por jugador para mantener el patron ya usado en `mission-system`.

Ejemplo:

```yml
active-bounty: 845.50
updated-at: "2026-04-19T08:34:12Z"
```

El sistema debe tolerar ausencia de archivo y tratarla como bounty cero.

## Configuracion

Se agregara una nueva seccion en `config.yml`.

Propuesta inicial:

```yml
modules:
  bounty-system: true

bounty-system:
  economy:
    take-percent: 0.30
    bounty-percent: 0.20
    minimum-balance: 100.0
    minimum-bounty: 25.0
  messages:
    payout: "&aCobraste {amount} por la bounty de {victim}."
    new-bounty-victim: "&cTu muerte genero una nueva bounty de {amount}."
    new-bounty-killer: "&e{victim} ahora tiene una nueva bounty de {amount}."
    no-refresh: "&7{victim} no tenia balance suficiente para generar nueva bounty."
```

Notas:

- `modules.bounty-system` define si el modulo participa del boot general
- no se agregara `bounty-system.enabled` para evitar doble fuente de verdad
- los mensajes son configurables, pero el diseño no requiere una capa compleja de localizacion

## Casos Limite

### 1. Muerte sin killer jugador

No hay payout ni nueva bounty.

### 2. Victima sin bounty previa

No se paga nada, pero igual puede generarse nueva bounty si el balance restante alcanza.

### 3. Victima con bounty previa y balance restante bajo

Se paga la bounty previa. Si despues del payout el balance no supera `minimum-balance`, no se refresca bounty.

### 4. Victima con bounty previa y balance suficiente

Se paga la bounty previa y se crea una nueva bounty sobre la misma victima en el mismo evento.

### 5. Balance cero o negativo

No se retira dinero ni se crea bounty.

### 6. Error de Vault al depositar o retirar

La operacion debe fallar de forma segura y dejar log claro. El listener no debe duplicar transacciones por reintentos implicitos.

### 7. Bounties stacked

Si una victima ya tenia bounty activa, esa bounty se paga al killer en la muerte actual. Luego, si el refresh genera una nueva bounty, ese nuevo valor queda como la bounty activa persistida de la victima para kills futuras.

## Testing

La mayor parte del valor debe quedar cubierta con tests unitarios puros de dominio.

### Tests de dominio

- crea bounty nueva desde balance y porcentajes configurados
- suma bounty nueva sobre bounty existente
- paga bounty previa al killer y luego refresca con balance restante
- no hace nada en muertes no PvP
- no refresca bounty si el balance queda debajo de `minimum-balance`
- no refresca bounty si `new-bounty` cae debajo de `minimum-bounty`
- valida configuraciones invalidas como `bounty-percent > take-percent`

### Tests de persistencia

- guarda y carga `active-bounty`
- devuelve bounty cero cuando el archivo no existe

### Tests de wiring

- el modulo no habilita sin Vault operativo
- el listener queda registrado cuando el modulo habilita

## Implementacion Esperada

Archivos probables:

- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyModule.java`
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyService.java`
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyListener.java`
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyStorage.java`
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/YamlBountyStorage.java`
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyPlayerState.java`
- `src/test/java/dev/linqfy/bigCasares/modules/bounties/...`

Tambien se espera modificar:

- `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- `src/main/resources/config.yml`

Tambien se espera crear:

- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyEconomyGateway.java`

## Decision Recomendada De Integracion

El modulo de bounties tendra su propio `BountyEconomyGateway`, chico y aislado, para no acoplar el dominio PvP al gateway de recompensas de misiones.

La prioridad es mantener:

- logica de dominio de bounties testeable
- adaptadores Vault chicos
- modulos aislados por responsabilidad

## Riesgos

- errores de redondeo pueden dejar diferencias pequenas si el proveedor de economia maneja precision distinta
- retirar y depositar en el orden incorrecto puede cambiar el balance usado para el refresh
- mezclar payout y refresh sin un resultado estructurado puede volver confuso el messaging
- doble fuente de verdad para enablement del modulo puede generar configuraciones inconsistentes

## Criterios De Aceptacion

- una muerte PvP paga al killer toda la bounty activa previa de la victima
- esa misma muerte puede crear una nueva bounty sobre la victima usando su balance restante
- las nuevas bounties se acumulan sobre el mismo jugador
- el sistema destruye parte del dinero retirado para generar bleed economico
- el estado de bounty sobrevive reinicios del servidor
- el modulo no habilita sin Vault funcional
- las reglas principales quedan cubiertas por tests automatizados

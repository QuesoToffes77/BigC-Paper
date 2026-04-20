# Sistema de Limites de Inventario

## Objetivo

Agregar un modulo `inventory-limit` para BigCasares que limite la cantidad maxima de ciertos items vanilla dentro del inventario del jugador, con configuracion en `config.yml` y soporte de reload por `/bigcasares reload`.

El sistema debe permitir reglas como:

- `TOTEM_OF_UNDYING: 3`

Y aplicar el limite exclusivamente sobre lo que el jugador lleva encima, sin intervenir en cofres, ender chests ni otros contenedores externos.

## Alcance

- Modulo nuevo dentro del sistema actual de `PluginModule`
- Configuracion en `config.yml`
- Soporte solo para items vanilla `Material`
- Enforzamiento sobre inventario del jugador
- Integracion con `/bigcasares reload`
- Prevencion de exceso cuando el evento lo permita
- Remocion y drop exacto del overflow cuando ya no sea posible negar a tiempo

## Fuera De Alcance

- Soporte para custom items en esta primera version
- Limites sobre cofres, ender chests o almacenamiento externo
- UI o comandos de administracion in-game para editar limites
- Reglas por mundo, permiso o grupo
- Paginacion, reportes o logs analiticos avanzados

## Reglas Confirmadas

- El limite aplica solo al inventario del jugador.
- El plugin debe intentar negar la accion primero.
- Si negar no alcanza por el flujo del evento, debe remover solo el overflow real.
- El overflow removido puede dropearse al piso.
- Debe evitarse cualquier posibilidad de dupe.
- La configuracion vive en `config.yml`.
- Esta primera version limita solo `Material` vanilla.

## Enfoque Recomendado

Se implementara un modulo dedicado `inventory-limit` con dos piezas chicas:

- un loader de configuracion para construir el mapa `Material -> maximo`
- un listener con una logica centralizada de enforcement

La logica de contar, calcular overflow y recortar inventario no debe quedar repartida entre varios handlers Bukkit. Tiene que vivir en un servicio utilitario unico para que el comportamiento sea consistente entre pickup, clicks, shop y otras fuentes.

## Arquitectura

### 1. InventoryLimitModule

Responsable de:

- leer la seccion `inventory-limit` desde `config.yml`
- validar materiales y limites
- registrar listeners
- reconstruir estado en memoria al recargar

Si una entrada esta mal configurada, se deja log claro y se ignora solo esa regla.

### 2. InventoryLimitSettingsLoader

Responsable de:

- leer `inventory-limit.limits`
- parsear claves como `Material`
- validar que el maximo sea mayor que cero
- devolver un mapa inmutable de limites validos

Ejemplo:

```yml
modules:
  inventory-limit: true

inventory-limit:
  limits:
    TOTEM_OF_UNDYING: 3
    ENDER_PEARL: 8
```

### 3. InventoryLimitService

Servicio chico de dominio.

Responsable de:

- contar cuantas unidades de un `Material` hay en el inventario del jugador
- calcular cuanto espacio legal queda para un item limitado
- determinar si una accion debe negarse
- remover exactamente el overflow cuando el exceso ya entro
- devolver cuanto overflow fue removido

La prioridad es que toda remocion sea exacta y trazable:

- nunca remover menos del overflow
- nunca remover mas del overflow
- nunca dropear mas items de los que efectivamente se quitaron

### 4. InventoryLimitListener

Listener Bukkit para eventos que pueden meter items en el inventario del jugador.

Cobertura inicial recomendada:

- `EntityPickupItemEvent`
- `InventoryClickEvent`
- `InventoryDragEvent`
- `PlayerSwapHandItemsEvent`

Ademas, el diseño debe dejar una API publica o helper facil de reutilizar para que otros flujos del plugin, como shop o comandos give, puedan llamar una revalidacion posterior si agregan items programaticamente.

## Definicion De Inventario Alcanzado

El limite aplica al inventario del jugador que lleva encima.

En esta primera version, se recomienda contar:

- hotbar
- slots principales del inventario
- offhand

Y excluir:

- armor slots
- ender chest
- cualquier contenedor abierto

La razon es mantener una regla clara de "lo que puede usar o consumir inmediatamente" sin mezclar equipamiento o almacenamiento externo.

## Estrategia De Enforcement

### Camino preferido: negar

Si antes de insertar el item en el inventario ya puede saberse que el jugador esta al limite:

- cancelar el evento
- dejar el item original sin tocar en su origen natural

Esto aplica especialmente a pickups o movimientos claros hacia el inventario del jugador.

### Camino fallback: recortar y dropear

Si por la semantica del evento el inventario ya cambio o parte del stack ya entro:

1. recalcular el total real del `Material`
2. determinar el overflow exacto
3. remover exactamente ese overflow del inventario
4. dropear exactamente esa cantidad removida en la ubicacion del jugador

La implementacion no debe crear un `ItemStack` nuevo para dropear sin antes haber removido la misma cantidad del inventario. Primero se remueve, despues se dropea.

## Integracion Con Otros Flujos

### Shop

Como el shop agrega items directamente al inventario:

- despues de una compra, el shop debe llamar la misma logica de enforcement
- si el inventario excede el limite por esa compra, se recorta el overflow y se dropea lo removido

Esto evita inconsistencias entre items obtenidos del mundo e items comprados por GUI.

### Give y otras entregas programaticas

El diseño debe dejar un metodo reutilizable para validar un jugador despues de darle items programaticamente.

No es obligatorio reescribir todo el arbol de comandos en esta iteracion, pero la API debe quedar lista para que `/bigcasares give` tambien pueda reaprovecharla si se decide cubrirlo ahora.

## Manejo De Errores

- si un material configurado no existe: log y omitir la regla
- si un limite es cero o negativo: log y omitir la regla
- si un evento no apunta al inventario del jugador: ignorar
- si por alguna razon no se puede remover el overflow esperado: dejar log warning y no fabricar drops artificiales

## Testing

Se agregaran tests unitarios para:

- carga de configuracion de limites
- rechazo de materiales invalidos
- rechazo de limites no positivos
- conteo de items limitados en inventario
- calculo de overflow
- remocion exacta de overflow
- wiring estable del modulo

No se intentara cubrir toda la mecanica de eventos Bukkit con tests unitarios puros, pero si la logica que decide cuanto negar, cuanto remover y cuanto dropear.

## Riesgos

- `InventoryClickEvent` tiene muchos caminos y es facil cancelar demasiado o demasiado poco
- si la logica de drop no depende de la cantidad realmente removida, puede aparecer duplicacion
- si el conteo no incluye offhand cuando corresponde, un jugador podria saltear el limite
- si el conteo incluye slots no deseados, el comportamiento puede sentirse injusto

## Plan De Implementacion Esperado

1. crear spec y plan del modulo `inventory-limit`
2. agregar tests del loader y del servicio de overflow
3. implementar loader, servicio y wiring del modulo
4. registrar listener de enforcement
5. integrar revalidacion post-compra en el shop
6. agregar configuracion por defecto en `config.yml`
7. verificar tests y regresiones

## Criterios De Aceptacion

- `config.yml` puede definir limites por `Material`
- el modulo solo afecta el inventario del jugador
- el sistema intenta negar antes de dejar entrar el exceso
- cuando no puede negar, remueve solo el overflow real
- solo se dropea lo que realmente fue removido
- no hay soporte para custom items en esta primera version
- `/bigcasares reload` refresca los limites configurados

# Sistema de Shop con Categorias y Reload

## Objetivo

Agregar un modulo `shop-system` para BigCasares con un shop basado en GUI de contenedor, navegacion por categorias y listado de items, integracion con Vault para comprar y vender, y carga dinamica desde `shop.yml`.

El flujo principal debe ser:

- abrir un menu de categorias con `/shop` o mediante el arbol existente de `/bigcasares`
- entrar a una categoria
- ver items configurados en slots predefinidos
- comprar o vender desde la GUI
- recargar la configuracion con `/bigcasares reload` sin reiniciar el servidor

## Alcance

- Modulo nuevo dentro del sistema actual de `PluginModule`
- Archivo `shop.yml` separado de `config.yml`
- GUI fija de categorias
- GUI fija de listado de items por categoria
- Soporte para items vanilla y items del `CustomItemRegistry`
- Compra y venta usando Vault
- Venta deliberadamente desfavorable, controlada por precio de venta en configuracion
- Hot reload por `/bigcasares reload`

## Fuera De Alcance

- Editor in-game del shop
- Layouts de GUI configurables por archivo
- Paginacion multiple en esta primera version
- Historial de compras o ventas
- Precios dinamicos por oferta/demanda
- NPCs o bloques fisicos para abrir el shop

## Reglas Confirmadas

- El shop tendra principalmente items vanilla, pero debe permitir items del `CustomItemRegistry`.
- La compra y la venta existen en el mismo sistema.
- La venta debe pagar montos muy bajos en comparacion con la compra.
- La GUI de categorias y la GUI de items tienen layout fijo en codigo.
- `shop.yml` solo llena categorias, slots, items, textos y precios.
- El comando de recarga sera `/bigcasares reload`.

## Enfoque Recomendado

Se implementara un modulo dedicado `shop-system` con tres responsabilidades separadas:

- carga y validacion de configuracion de shop
- dominio economico de compra y venta
- control de GUIs e interacciones Bukkit

Ademas, se movera el manejo del comando principal `/bigcasares` a un router compartido para evitar que `CopperAppleModule` siga siendo el dueño accidental de todos los subcomandos del plugin.

## Arquitectura

### 1. ShopModule

Responsable de:

- guardar `shop.yml` por defecto si no existe
- cargar catalogo y vistas del shop
- validar Vault al iniciar
- registrar listeners del inventario
- exponer apertura del menu principal
- exponer un metodo `reload()` seguro para refrescar el estado del modulo

Si Vault no esta disponible, el modulo falla al habilitarse y deja log explicito.

### 2. ShopCatalogLoader

Responsable de:

- leer `shop.yml`
- parsear categorias
- parsear entradas de shop
- validar slots, materiales, ids custom y precios
- omitir entradas invalidas con log claro

El resultado sera un catalogo en memoria ya validado, listo para ser consumido por la GUI y por el servicio de compra/venta.

### 3. ShopCatalog

Modelo inmutable del shop cargado.

Debe exponer:

- categorias en orden estable
- acceso por id de categoria
- entradas por categoria
- metadatos visuales necesarios para renderizar la GUI fija

### 4. ShopEntry

Cada entrada del shop representa un item vendible/comprable.

Campos propuestos:

- `id`
- `slot`
- `display-name`
- `lore`
- `buy-price`
- `sell-price`
- `amount`
- `item-source`

`item-source` sera una union logica de:

- item vanilla por `material`
- item custom por `custom-item-id`

No se permitira una entrada ambigua con ambos al mismo tiempo.

### 5. ShopItemResolver

Adaptador para convertir definiciones del catalogo en objetos reales de Bukkit.

Responsable de:

- resolver `Material`
- resolver `CustomItem` desde `CustomItemRegistry`
- construir el `ItemStack` de vista y el `ItemStack` de entrega
- exponer una forma consistente de validar coincidencia al vender

Para items custom, la venta debe usar el matcher del registry actual, no solo el tipo material.

### 6. ShopEconomyGateway

Adaptador chico alrededor de Vault.

Responsable de:

- retirar dinero al comprar
- depositar dinero al vender
- consultar balance
- formatear montos
- exponer errores transaccionales claros

### 7. ShopService

Servicio principal de dominio.

Responsable de:

- validar si una compra es posible
- ejecutar compra y entrega
- validar si una venta es posible
- remover items coincidentes del inventario
- ejecutar pago de venta
- devolver resultados estructurados para mensajes y GUI

La logica economica y la logica de matching de items no debe vivir en listeners Bukkit.

### 8. ShopGuiController

Controlador de inventarios GUI.

Tendra dos vistas fijas:

- menu de categorias
- menu de items por categoria

Responsable de:

- construir inventarios a partir del catalogo
- manejar clicks
- abrir categoria siguiente
- ejecutar compra o venta segun el click
- cancelar movimientos invalidos dentro del inventario del shop

## Archivo `shop.yml`

`shop.yml` vivira separado de `config.yml` para no mezclar configuracion operacional del plugin con contenido editable del shop.

Estructura propuesta:

```yml
categories:
  blocks:
    name: "&6Bloques"
    icon: STONE
    slot: 10
    items:
      stone_stack:
        slot: 10
        material: STONE
        amount: 64
        display-name: "&fPiedra"
        lore:
          - "&7Stack basico para construir."
        buy-price: 64.0
        sell-price: 4.0
      copper_apple:
        slot: 12
        custom-item-id: copper_apple
        amount: 1
        display-name: "&6Copper Apple"
        lore:
          - "&7Item custom del servidor."
        buy-price: 1500.0
        sell-price: 50.0
```

### Validaciones

- cada categoria debe tener `name`, `icon` y `slot`
- cada categoria debe ocupar un slot valido del layout fijo de categorias
- cada item debe tener `slot`, `amount`, `buy-price` y `sell-price`
- cada item debe definir exactamente uno entre `material` y `custom-item-id`
- `amount` debe ser mayor que cero y no superar el max stack del item entregado
- `buy-price` no puede ser negativo
- `sell-price` no puede ser negativo

## Layouts Fijos

### GUI de categorias

- inventario fijo de 27 slots
- slots de categorias definidos en `shop.yml`
- resto del layout completado con decoracion fija desde codigo

### GUI de items

- inventario fijo de 54 slots
- grilla util definida por codigo
- slots de items definidos en `shop.yml`
- botones fijos de volver y cerrar
- decoracion fija desde codigo

El archivo de configuracion no controla tamano del inventario, fillers, ni forma estructural del menu.

## UX de Interaccion

### Apertura

- `/shop` abre el menu de categorias
- `/bigcasares shop` puede quedar como alias interno del router principal

### Compra

Al hacer click izquierdo sobre una entrada:

- se valida balance
- se retira dinero
- se entrega el stack configurado
- si no entra completo en el inventario, los sobrantes se dropean naturalmente

### Venta

Al hacer click derecho sobre una entrada:

- se busca en el inventario del jugador un stack o combinacion suficiente del item objetivo
- se remueve la cantidad configurada
- se paga el `sell-price`

El diseño deliberadamente permite un sell price muy bajo para desalentar arbitraje.

### Feedback

Mensajes cortos al jugador para:

- compra exitosa
- venta exitosa
- balance insuficiente
- inventario sin suficientes items para vender
- shop temporalmente no disponible

## Comandos

### Nuevo comando

- `/shop`
  - abre el shop para jugadores

### Router principal

- `/bigcasares reload`
  - recarga `config.yml`
  - recarga `shop.yml`
  - reconstruye el estado del `shop-system`

### Permisos propuestos

- `bigcasares.shop.open`
- `bigcasares.shop.reload`

`bigcasares.*` debe incluir ambos.

## Reload

Se agregara un comando administrativo `/bigcasares reload`.

Comportamiento recomendado:

1. `plugin.reloadConfig()`
2. recargar el estado dependiente de `config.yml`
3. recargar `shop.yml`
4. reconstruir el catalogo en memoria del shop
5. responder al sender con resultado claro

Para esta iteracion, el contrato fuerte de hot reload queda garantizado para el shop. Otros modulos existentes podran seguir leyendo `plugin.getConfig()` cuando corresponda, pero no se debe prometer que todo su estado runtime se reconstituye completamente si hoy no fue diseñado para eso.

## Refactor De Comandos

Hoy el executor de `/bigcasares` se registra desde `CopperAppleModule`, lo cual acopla todo el arbol de comandos a un modulo no relacionado.

Se propone:

- crear un `BigCasaresCommand` central
- registrar ese command executor desde `BigCasares` o desde un componente neutral
- delegar subcomandos a helpers chicos

Subcomandos iniciales:

- `give`
- `misiones`
- `shop`
- `reload`

Esto deja base limpia para mas modulos sin seguir inflando `CopperAppleCommand`.

## Manejo De Errores

- si falta Vault: log y no habilitar `shop-system`
- si `shop.yml` tiene entradas invalidas: log detallado y omitir solo esas entradas
- si una categoria queda vacia tras validacion: puede mostrarse vacia o ser omitida del menu segun implementacion, pero el comportamiento debe ser consistente
- si falla una transaccion de Vault: no entregar items ni remover inventario
- si falla una venta despues de remover items: el servicio debe intentar ordenar la operacion para que el retiro de items suceda solo cuando el pago es viable

## Testing

Se agregaran tests unitarios para:

- parseo de `shop.yml`
- validacion de slots y precios
- resolucion de items vanilla
- resolucion de items custom por id
- matching de inventario para venta
- compra con balance suficiente e insuficiente
- wiring estable del modulo
- parsing basico del comando reload

No se intentara cubrir con unit tests toda la GUI Bukkit, pero si la logica de dominio, parsing y matching de inventario.

## Riesgos

- la venta de items custom exige matching exacto para no comprar falsos positivos por material base
- el reload debe evitar dejar referencias viejas abiertas en viewers actuales del inventario
- si el router de comandos se refactoriza mal, puede romper `give` o `misiones`
- layouts fijos simplifican la implementacion, pero exigen documentar bien los slots utiles de cada menu

## Plan De Implementacion Esperado

1. crear el spec y plan del `shop-system`
2. agregar `shop-system` al sistema de modulos
3. extraer un router central para `/bigcasares`
4. agregar el comando `/shop`
5. escribir tests de parser y dominio de shop
6. implementar loader, catalogo y resolver de items
7. implementar servicio de compra/venta
8. implementar GUIs y listener de inventario
9. agregar `shop.yml` por defecto
10. implementar `/bigcasares reload`
11. verificar tests y compilacion

## Criterios De Aceptacion

- `/shop` abre una GUI de categorias
- cada categoria abre una GUI fija de items
- `shop.yml` controla categorias, slots, textos y precios
- el shop soporta items vanilla y custom registry
- click izquierdo compra
- click derecho vende
- la venta paga montos bajos definidos en config
- `/bigcasares reload` refresca `shop.yml` sin reiniciar el servidor
- el comando principal deja de depender de `CopperAppleModule`

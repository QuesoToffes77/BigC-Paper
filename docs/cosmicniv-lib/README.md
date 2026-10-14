# cosmicniv_lib: nivel de equipo de 0 a 200

`cosmicniv_lib` es una librería de BigC que mira lo que un jugador (o un mob) tiene puesto y en la mano, y lo
resume en **un solo número entre 0 y 200**:

- **0**: un jugador sin absolutamente nada.
- **200**: armadura de netherite completa y full encantada, más **una** arma full encantada de cualquiera de estas:
  espada, hacha, maza, lanza o tridente.

Cualquier otro módulo de BigC (eventos, bosses, escalado de mobs, misiones, recompensas...) puede pedir ese número
en lugar de inventar su propia fórmula de "qué tan equipado está este jugador".

Todo se ajusta **sin tocar código**, desde dos archivos en `plugins/BigCasares/`:

| Archivo | Qué tiene |
|---|---|
| `cosmicniv_lib.yml` | **Cómo** se calcula: qué vale 0, qué vale 200, los pesos, la curva y los decimales |
| `cosmicniv_valores.yml` | **Cuánto vale** cada cosa: armaduras, armas, herramientas, encantamientos y objetos custom |

## Índice

1. [Inicio rápido](#1-inicio-rápido)
2. [Cómo se calcula](#2-cómo-se-calcula)
3. [Ejemplos con los valores de fábrica](#3-ejemplos-con-los-valores-de-fábrica)
4. [Curvas](#4-curvas)
5. [`cosmicniv_lib.yml`, clave por clave](#5-cosmicniv_libyml-clave-por-clave)
6. [`cosmicniv_valores.yml`, clave por clave](#6-cosmicniv_valoresyml-clave-por-clave)
7. [Objetos custom](#7-objetos-custom)
8. [Recetas: cambios comunes](#8-recetas-cambios-comunes)
9. [Errores y avisos](#9-errores-y-avisos)
10. [Usar la librería desde otro módulo de BigC](#10-usar-la-librería-desde-otro-módulo-de-bigc)
11. [Límites y preguntas frecuentes](#11-límites-y-preguntas-frecuentes)
12. [Para quien mantiene el código](#12-para-quien-mantiene-el-código)

---

## 1. Inicio rápido

### Dónde está

| Qué | Dónde |
|---|---|
| Prender o apagar el módulo | `config.yml` → `modules.cosmicniv-lib.enabled: true` |
| Cómo se calcula | `plugins/BigCasares/cosmicniv_lib.yml` |
| Cuánto vale cada cosa | `plugins/BigCasares/cosmicniv_valores.yml` |
| Archivos de fábrica | `src/main/resources/cosmicniv_lib.yml` y `cosmicniv_valores.yml` (el servidor nunca pisa tus copias) |
| Código | `src/main/java/dev/linqfy/bigCasares/modules/cosmicniv/` |

Los dos archivos se crean la primera vez que arranca el servidor.

### Comandos

| Comando | Qué hace | Permiso |
|---|---|---|
| `/cosmicniv` | Tu nivel y el detalle (armadura, arma, herramienta, curva) | `bigcasares.cosmicniv.check` (todos) |
| `/cosmicniv <jugador>` | El nivel de otro jugador | `bigcasares.cosmicniv.check.others` (op) |
| `/cosmicniv curva` | Tabla de la curva actual: progreso → nivel | `bigcasares.cosmicniv.admin` (op) |
| `/cosmicniv reload` | Vuelve a leer los dos archivos | `bigcasares.cosmicniv.admin` (op) |

Alias: `/cniv`. `bigcasares.cosmicniv.admin` incluye los otros dos y es hijo de `bigcasares.*`.
`/bigcasares reload` también recarga los archivos.

Ejemplo de lo que muestra `/cosmicniv`:

```
CosmicNiv » Juan: 77.4/200.0
  Armadura: 44% (poder 36.0)
  Arma: espada 31% (minecraft:diamond_sword, poder 7.0)
  Progreso: 0.39 -> curva LINEAR -> 0.39
```

La línea de herramienta aparece cuando las herramientas tienen peso o cuando el jugador tiene una.

### Flujo para ajustar

1. Editá `cosmicniv_valores.yml` (valores) o `cosmicniv_lib.yml` (cálculo).
2. `/cosmicniv reload`. Si hay errores, el comando muestra los primeros 5 (todos van a la consola) y **se sigue usando
   la configuración anterior**.
3. `/cosmicniv curva` para ver la curva y `/cosmicniv` con distintos equipos para probar.

---

## 2. Cómo se calcula

El cálculo tiene cinco pasos. Entre paréntesis, en qué archivo y clave se configura cada cosa.

### Paso 1: poder de cada ítem

```
poder(ítem) = valor del ítem  +  Σ  puntos por nivel × min(nivel, max-level)
              (valores: armor / weapons / tools / custom-items)   (valores: enchantments)
```

- Un ítem que **no está en `cosmicniv_valores.yml`** vale 0, aunque esté encantado (un palo con Filo V no suma).
- Un encantamiento que no está en `enchantments:` no suma nada.
- Los niveles por encima de `max-level` no suman de más: Filo X puesto con comandos cuenta como Filo V.
- Las maldiciones tienen puntos negativos, así que restan.
- Lo que está en `armor:` solo cuenta **puesto**. Lo que está en `weapons:` o `tools:` solo cuenta **en la mano o la
  barra rápida**. Una espada puesta en la cabeza no suma como armadura.

Ejemplo: espada de netherite con Filo V, Aspecto ígneo II, Empuje II, Botín III, Filo arrasador III, Irrompibilidad
III y Reparación:

```
8 (base) + 5×1.0 + 2×1.0 + 2×0.5 + 3×0.5 + 3×0.5 + 3×0.5 + 1×2.0 = 22.5
```

### Paso 2: puntaje de armadura A (entre 0 y 1)

Se suma el poder de las cuatro piezas y se ubica entre las dos referencias:

```
A = (poder armadura − poder armadura de "zero") / (poder armadura de "max" − poder armadura de "zero")
```

y se recorta a [0, 1]. Con los valores de fábrica, "zero" no tiene nada (poder 0) y "max" (netherite full
encantada) tiene poder 82.25, así que `A = poder / 82.25`.

### Paso 3: puntaje de arma W y de herramienta T (entre 0 y 1)

Cada arma pertenece a **una familia** de `weapons:` y cada herramienta a una familia de `tools:`. Cada una se compara
con la referencia **de su propia familia**:

```
N(ítem) = (poder − poder de la referencia "zero" de su familia) / (poder de la "max" de su familia − ...)
W       = el N más alto entre todas las armas que se encuentran            (0 si no hay ninguna)
T       = el N más alto entre todas las herramientas que se encuentran     (0 si no hay ninguna)
```

Por eso una maza full encantada y una espada full encantada dan las dos `W = 1`, aunque una tenga poder 18.5 y la
otra 22.5. Lo que no es arma ni herramienta (bloques, comida, armadura en la mano...) se ignora.

Dónde se buscan lo decide `held-search:` (por defecto la barra rápida y la mano secundaria).

### Paso 4: progreso t (entre 0 y 1)

```
t = (armor × A + weapon × W + tool × T) / (armor + weapon + tool)          (lib: weights)
```

Con los pesos de fábrica (0.6, 0.4 y 0): `t = 0.6·A + 0.4·W`. **Las herramientas no cuentan hasta que les des
peso** (sección 8).

### Paso 5: curva y nivel

```
nivel = maximum × f(t)                                                      (lib: output.maximum, curve)
```

`f` es la curva elegida. **Todas las curvas cumplen `f(0) = 0` y `f(1) = 1`**: la referencia "zero" siempre da 0 y la
"max" siempre da 200. La curva solo cambia cómo se reparten los valores del medio. Al final se redondea a
`output.decimals` decimales.

### Resumen en una línea (valores de fábrica)

```
nivel = 200 · f( 0.6 · clamp(Parmadura / 82.25) + 0.4 · max_armas clamp(Parma / Pmax de su familia) )
```

---

## 3. Ejemplos con los valores de fábrica

Con la curva `LINEAR` y los pesos 0.6 / 0.4 / 0.

### Poder de las referencias "max"

| Referencia | Poder |
|---|---|
| Armadura de netherite full encantada (4 piezas) | 82.25 |
| Espada de netherite full | 22.5 |
| Hacha de netherite full | 22.4 |
| Maza full | 18.5 |
| Lanza de netherite full | 23.0 |
| Tridente full | 19.0 |
| Pico, pala o azada de netherite full | 12.4 |
| Tijeras full | 8.5 |
| Caña de pescar full | 8.5 |

### Niveles de equipos típicos

| Equipo | Nivel |
|---|---|
| Nada | 0 |
| Cuero completo, sin encantar, sin arma | 10.2 |
| Hierro completo, sin encantar, sin arma | 21.9 |
| Diamante completo, sin encantar, sin arma | 40.9 |
| Netherite completo, sin encantar, sin arma | 52.5 |
| Solo el Bate de Sahur (objeto custom, valor 9), sin armadura | 32.0 |
| Netherite completo sin encantar + espada de diamante sin encantar | 77.4 |
| Solo un arma full encantada (cualquier familia), sin armadura | 80.0 |
| Solo la armadura de netherite full encantada, sin arma | 120.0 |
| Armadura full encantada + espada de netherite sin encantar | 148.4 |
| Armadura full encantada + cualquier arma full encantada | **200.0** |

Cuenta del ejemplo de 77.4: `A = 36 / 82.25 = 0.4377` y `W = 7 / 22.5 = 0.3111`, así que
`t = 0.6·0.4377 + 0.4·0.3111 = 0.3871` y el nivel es `200 · 0.3871 = 77.4`.

### Cuánto aporta cada cosa de la armadura

Un punto de poder de armadura vale `120 / 82.25 ≈ 1.46` de nivel (la armadura completa da como mucho 120):

| Cosa | Poder | Nivel que aporta |
|---|---|---|
| Pechera de netherite sin encantar | 12 | ≈ 17.5 |
| Pechera de diamante sin encantar | 10 | ≈ 14.6 |
| Protección IV en una pieza | 6 | ≈ 8.8 |
| Reparación en una pieza | 2 | ≈ 2.9 |
| Irrompibilidad III en una pieza | 1.5 | ≈ 2.2 |

---

## 4. Curvas

La curva toma el progreso `t` (0 a 1) y devuelve qué fracción del máximo se da. Sirve para decidir si los primeros
pasos (cuero → hierro) deben valer mucho o poco comparados con los últimos (diamante → netherite full).

| Tipo | Fórmula | Parámetros | Efecto |
|---|---|---|---|
| `LINEAR` | `f(t) = t` | ninguno | Proporcional |
| `EXPONENTIAL` | `f(t) = (e^(k·t) − 1) / (e^k − 1)` | `strength` = k, entre 0.000001 y 50 en valor absoluto (puede ser negativo) | k > 0: arranca lento y acelera al final. k < 0: al revés |
| `LOGARITHMIC` | `f(t) = ln(1 + k·t) / ln(1 + k)` | `strength` = k, entre 0.000001 y 1 000 000 | Arranca rápido y se frena al final |
| `POWER` | `f(t) = t^p` | `exponent` = p, mayor que 0 y hasta 100 | p > 1: lento al principio. p < 1: rápido (0.5 es la raíz cuadrada) |
| `SIGMOID` | curva en S (logística normalizada) | `strength` = k, entre 0.000001 y 100 (pendiente), y `midpoint` = m mayor que 0 y menor que 1 (dónde está el salto) | Poco cambio en los extremos, salto fuerte cerca de m |
| `POINTS` | rectas entre los puntos que pongas | `points` | Cualquier forma |

Los parámetros que no usa la curva elegida se ignoran, así que podés dejar todos escritos.

### Cómo quedan, con el nivel en t = 0.25, 0.5 y 0.75

| Curva | t = 0.25 | t = 0.5 | t = 0.75 |
|---|---|---|---|
| `LINEAR` | 50.0 | 100.0 | 150.0 |
| `EXPONENTIAL`, strength 3 | 11.7 | 36.5 | 88.9 |
| `EXPONENTIAL`, strength −3 | 111.1 | 163.5 | 188.3 |
| `LOGARITHMIC`, strength 3 | 80.7 | 132.2 | 170.0 |
| `LOGARITHMIC`, strength 9 | 102.4 | 148.1 | 177.9 |
| `POWER`, exponent 2 | 12.5 | 50.0 | 112.5 |
| `POWER`, exponent 0.5 | 100.0 | 141.4 | 173.2 |
| `SIGMOID`, strength 3, midpoint 0.5 | 43.6 | 100.0 | 156.4 |
| `SIGMOID`, strength 10, midpoint 0.5 | 14.0 | 100.0 | 186.0 |
| `POINTS` de fábrica: [0,0] [0.5,0.3] [1,1] | 30.0 | 60.0 | 130.0 |

En todos los casos `t = 0` da 0 y `t = 1` da 200. `/cosmicniv curva` muestra el nivel de la curva que tengas puesta
para t = 0.0, 0.1, 0.2 ... 1.0.

### Ejemplos de configuración

Exponencial (cuesta mucho llegar alto):

```yaml
curve:
  type: EXPONENTIAL
  strength: 3.0
```

Logarítmica (equiparse un poco ya da bastante nivel):

```yaml
curve:
  type: LOGARITHMIC
  strength: 9.0
```

En S, con el salto en el 60 %:

```yaml
curve:
  type: SIGMOID
  strength: 10.0
  midpoint: 0.6
```

Puntos a mano. Tiene que empezar en `[0, 0]` y terminar en `[1, 1]`, la t de cada punto tiene que ser mayor que la
anterior, y el valor nunca puede bajar:

```yaml
curve:
  type: POINTS
  points:
    - [0.0, 0.0]
    - [0.3, 0.1]    # hasta 30 % de progreso, como mucho 20 de nivel
    - [0.8, 0.6]
    - [1.0, 1.0]
```

---

## 5. `cosmicniv_lib.yml`, clave por clave

Las claves de ítems y encantamientos aceptan `netherite_sword`, `NETHERITE_SWORD` o `minecraft:netherite_sword`:
todas se pasan a `minecraft:netherite_sword`. Son los ids de Minecraft en inglés (los mismos de `/give`). Los objetos
custom se escriben `custom:<nombre>`.

### `output`

| Clave | Por defecto | Rango | Qué es |
|---|---|---|---|
| `maximum` | 200.0 | 0.001 a 1 000 000 | Nivel de la referencia "max" |
| `decimals` | 1 | 0 a 6 | Decimales del valor que reciben el comando y los otros módulos |

### `weights`

| Clave | Por defecto | Qué es |
|---|---|---|
| `armor` | 0.6 | Peso de la armadura |
| `weapon` | 0.4 | Peso del arma |
| `tool` | 0.0 | Peso de la herramienta (0 = no cuenta) |

No hace falta que sumen 1: se usan como proporción (`armor: 3, weapon: 2` es lo mismo que 0.6 / 0.4). Cada uno va de
0 a 1 000 y la suma tiene que ser mayor que 0. Una parte con peso 0 no cuenta y no necesita referencias.

### `held-search`

| Valor | Dónde busca las armas y herramientas de un jugador |
|---|---|
| `MAIN_HAND` | Solo la mano principal. Cambia apenas el jugador cambia de slot |
| `HANDS` | Mano principal y secundaria |
| `HOTBAR` (por defecto) | Los 9 slots de la barra rápida y la mano secundaria. Es estable y difícil de esquivar |
| `INVENTORY` | Todo el inventario y la mano secundaria |

Siempre cuenta **la mejor** arma y **la mejor** herramienta encontradas. Los mobs usan siempre sus dos manos.

### `curve`

| Clave | Por defecto | La usan |
|---|---|---|
| `type` | `LINEAR` | todas |
| `strength` | 3.0 | `EXPONENTIAL`, `LOGARITHMIC`, `SIGMOID` |
| `exponent` | 2.0 | `POWER` |
| `midpoint` | 0.5 | `SIGMOID` |
| `points` | — | `POINTS` (lista de `[t, valor]`) |

### `references`

Las dos referencias tienen la misma forma:

```yaml
references:
  zero:                      # lo que vale 0 (opcional; vacío = nada)
    armor: { ... }
    weapons: { ... }
    tools: { ... }
  max:                       # lo que vale 200 (obligatorio)
    armor:
      helmet: ...            # helmet, chestplate, leggings, boots
    weapons:
      sword: ...             # una por cada familia de weapons: de cosmicniv_valores.yml
    tools:
      pickaxe: ...           # una por cada familia de tools: (solo hace falta si weights.tool > 0)
```

Cada ítem se escribe de una de estas dos formas:

```yaml
helmet: leather_helmet                      # sin encantamientos

helmet:                                     # con encantamientos
  item: netherite_helmet
  enchantments: { protection: 4, unbreaking: 3, mending: 1 }
```

Reglas:
- Las piezas de `armor` tienen que estar en `armor:` de `cosmicniv_valores.yml` (o ser un objeto custom de armadura).
- Cada arma o herramienta tiene que ser de esa familia (no podés poner un hacha en `sword:`).
- Si una parte tiene peso, cada una de sus familias necesita su referencia "max".
- La "max" tiene que tener más poder que la "zero" (en cada parte que tenga peso).
- Una familia que no está en `zero` tiene "zero" = nada (poder 0).

**Referencias de fábrica.** Los encantamientos se eligieron con los datos de vanilla 26.2 (qué ítems acepta cada
encantamiento y cuáles son incompatibles entre sí), para que cada referencia se pueda conseguir en survival:

| Pieza | Encantamientos |
|---|---|
| Casco de netherite | Protección IV, Respiración III, Afinidad acuática, Irrompibilidad III, Reparación |
| Pechera de netherite | Protección IV, Irrompibilidad III, Reparación |
| Pantalones de netherite | Protección IV, Sigilo rápido III, Irrompibilidad III, Reparación |
| Botas de netherite | Protección IV, Caída de pluma IV, Agilidad acuática III, Velocidad de alma III, Irrompibilidad III, Reparación |
| Espada de netherite | Filo V, Aspecto ígneo II, Empuje II, Botín III, Filo arrasador III, Irrompibilidad III, Reparación |
| Hacha de netherite | Filo V, Eficiencia V, Fortuna III, Irrompibilidad III, Reparación |
| Maza | Densidad V, Carga de viento III, Aspecto ígneo II, Irrompibilidad III, Reparación |
| Lanza de netherite | Filo V, Embestida III, Aspecto ígneo II, Empuje II, Botín III, Irrompibilidad III, Reparación |
| Tridente | Empalamiento V, Lealtad III, Canalización, Irrompibilidad III, Reparación |
| Pico, pala y azada de netherite | Eficiencia V, Fortuna III, Irrompibilidad III, Reparación |
| Tijeras | Eficiencia V, Irrompibilidad III, Reparación |
| Caña de pescar | Suerte marina III, Atracción III, Irrompibilidad III, Reparación |

Espinas no está en la pechera porque mucha gente no la usa. Si querés que cuente para llegar a 200, agregala a la
referencia: `enchantments: { protection: 4, thorns: 3, unbreaking: 3, mending: 1 }`.

---

## 6. `cosmicniv_valores.yml`, clave por clave

Este es el archivo para cambiar **cuánto vale cada cosa**. Reglas generales:

- Un ítem que no está en el archivo vale 0 y no cuenta.
- **Cada ítem va en un solo lugar**: `armor`, una familia de `weapons`, una familia de `tools` o `custom-items`.
  Por eso el hacha está en `weapons` y no en `tools`.
- Los valores van de −1 000 000 a 1 000 000.
- Los nombres de familia usan `a-z`, `0-9`, `_` y `-`, y un mismo nombre no puede estar en `weapons` y en `tools`.
- **Solo importa la proporción** entre valores: multiplicar todo por 2 no cambia ningún nivel.

### `armor`

```yaml
armor:
  netherite_chestplate: 12.0
  diamond_chestplate: 10.0
```

Valor de cada pieza sin encantar. Los de fábrica salen de los atributos vanilla:

```
valor = puntos de armadura + dureza + 10 × resistencia al empuje
```

Por ejemplo, la pechera de netherite es 8 + 3 + 10 × 0.1 = 12. Los atributos son vanilla, pero cómo se combinan es
un criterio: que 1 de dureza valga lo mismo que 1 de armadura, y que 0.1 de empuje valga 1.

### `weapons`

```yaml
weapons:
  sword:
    iron_sword: 6.0
    netherite_sword: 8.0
  mace:
    mace: 6.0
```

Armas agrupadas por familia. Cada familia necesita su referencia en `references.max.weapons` de `cosmicniv_lib.yml`.
Los valores de fábrica son el daño de ataque vanilla. Las lanzas usan una escala aproximada por material (de 3 a 7),
porque su ataque cargado no se resume en un solo número: conviene ajustarlas probando.

Como cada arma se compara contra **su** familia, solo importa la diferencia dentro de la familia y contra sus
encantamientos. Que una espada valga 8 y un tridente 9 no hace que el tridente "gane".

### `tools`

```yaml
tools:
  pickaxe:
    iron_pickaxe: 3.0
    netherite_pickaxe: 5.0
  fishing_rod:
    fishing_rod: 2.0
```

Igual que `weapons`, pero para herramientas: pico, pala, azada, tijeras y caña de pescar. Los valores de fábrica
siguen el nivel del material: madera y oro 1, piedra 2, cobre 2.5, hierro 3, diamante 4, netherite 5.

Las herramientas **solo cuentan si `weights.tool` es mayor que 0** en `cosmicniv_lib.yml`. Mientras tanto,
`/cosmicniv` igual las muestra si tenés una, y los otros módulos pueden leer su puntaje.

### `enchantments`

```yaml
enchantments:
  protection: { points-per-level: 1.5, max-level: 4 }
```

| Clave | Qué es |
|---|---|
| `points-per-level` | Puntos que suma cada nivel. Puede ser negativo (maldiciones). Entre −1 000 000 y 1 000 000 |
| `max-level` | Nivel más alto que se cuenta (de 1 a 255) |

El mismo encantamiento vale lo mismo en cualquier ítem. Los `max-level` de fábrica son los de vanilla. **Los puntos
por nivel son un criterio**, no salen de una fórmula del juego: se repartieron según cuánto ayuda cada uno.

| Encantamiento | Puntos por nivel | Total al máximo | Por qué |
|---|---|---|---|
| Protección | 1.5 | 6 | Es la que más reduce el daño |
| Protecciones específicas (fuego, explosión, proyectiles) | 0.75 | 3 | Protegen solo contra un tipo de daño |
| Filo | 1.0 | 5 | El daño extra más directo |
| Aspecto ígneo | 1.0 | 2 | Mucho daño extra por nivel |
| Eficiencia | 0.6 | 3 | Lo principal de una herramienta |
| Reparación | 2.0 | 2 | Muy valiosa, pero no protege ni daña |
| Irrompibilidad | 0.5 | 1.5 | Utilidad |
| Caída de pluma, Respiración, Sigilo rápido, Agilidad acuática | 0.5 | 1.5 a 2 | Utilidad |
| Velocidad de alma | 0.25 | 0.75 | Situacional |
| Maldición de ligamiento | −2.0 | −2 | Resta |

Si querés que el material pese más que los encantamientos, subí los valores de `armor` / `weapons` / `tools` o bajá
los `points-per-level`.

### `custom-items`

Ver la sección siguiente.

---

## 7. Objetos custom

Un objeto custom es un ítem que tiene que valer distinto que su material de base. Por ejemplo, el **Bate de Sahur** de
BigC es una espada de hierro por debajo, pero pega mucho más rápido. Sin esta sección valdría lo mismo que una espada
de hierro común (6); con ella vale 9.

```yaml
custom-items:
  sahurs_bat:                       # nombre (a-z, 0-9, _ y -); en las referencias es custom:sahurs_bat
    match:
      bigcasares-item: sahurs_bat   # cómo reconocerlo
    category: weapon                # armor, weapon o tool
    family: sword                   # familia de weapons o tools (no va en armor)
    value: 9.0                      # valor sin encantar
```

### Cómo se reconoce (`match`)

Poné **al menos una** condición. Si ponés varias, se tienen que cumplir **todas**.

| Condición | Qué compara | Para qué sirve |
|---|---|---|
| `bigcasares-item: <id>` | El id del catálogo de BigC, el mismo de `src/main/resources/content/items/<id>.yml` y de `/bigcasares give <id>` | Ítems de BigC |
| `item-model: <ns:modelo>` | El componente `item_model` del ítem (por ejemplo `bigcasares:item/sahurs_bat`) | Ítems de cualquier plugin que use modelos propios |
| `persistent-data: <ns:clave>` | Que el ítem tenga ese dato persistente, como texto | Ítems marcados por otros plugins |
| `persistent-data: { key: <ns:clave>, value: "<texto>" }` | ...y que valga exactamente ese texto (siempre entre comillas: `value: "1"`) | Distinguir variantes de un mismo plugin |
| `material: <material>` | El material de base | Acotar (por ejemplo, solo si es `diamond_pickaxe`) |

Si un ítem coincide con varios objetos custom, **gana el primero de la lista**. Los encantamientos que tenga el ítem
suman igual que en uno normal.

### `category` y `family`

| `category` | Cuenta | `family` |
|---|---|---|
| `armor` | Puesto en casco, pechera, pantalones o botas | No va |
| `weapon` | En la mano o la barra rápida, como arma | Una familia de `weapons:` (la compara contra su referencia) |
| `tool` | En la mano o la barra rápida, como herramienta | Una familia de `tools:` |

### Ejemplos

Un casco de otro plugin reconocido por su modelo:

```yaml
  casco_dragon:
    match:
      item-model: otroplugin:casco_dragon
    category: armor
    value: 9.0
```

Un pico especial, solo si es de diamante y otro plugin lo marcó como "magico":

```yaml
  pico_magico:
    match:
      material: diamond_pickaxe
      persistent-data: { key: "otroplugin:tipo", value: "magico" }
    category: tool
    family: pickaxe
    value: 6.0
```

Usar un objeto custom como referencia (por ejemplo, que el máximo de espada sea el Bate de Sahur full encantado),
en `cosmicniv_lib.yml`:

```yaml
references:
  max:
    weapons:
      sword:
        item: custom:sahurs_bat
        enchantments: { sharpness: 5, unbreaking: 3, mending: 1 }
```

### Cómo averiguar el `item-model` o el dato persistente de un ítem

Con el ítem en la mano, `/data get entity @s SelectedItem` muestra sus componentes: `minecraft:item_model` es el
modelo y `minecraft:custom_data` → `PublicBukkitValues` tiene los datos persistentes (las claves son
`plugin:clave`).

---

## 8. Recetas: cambios comunes

### Que la armadura de cuero completa valga 0

En `cosmicniv_lib.yml`:

```yaml
references:
  zero:
    armor:
      helmet: leather_helmet
      chestplate: leather_chestplate
      leggings: leather_leggings
      boots: leather_boots
```

Ahora la armadura se mide desde el cuero: con cuero o menos, `A = 0`.

### Que las herramientas cuenten

En `cosmicniv_lib.yml`:

```yaml
weights:
  armor: 0.55
  weapon: 0.35
  tool: 0.1
```

Ahora el 200 también pide una herramienta de las referencias de `references.max.tools`. Por ejemplo, armadura y
espada full sin herramienta dan 180, y netherite sin encantar + espada de diamante + pico de hierro dan 74.8.

### Que el máximo sea diamante en vez de netherite

Cambiá las cuatro piezas de `references.max.armor` a `diamond_...` y las armas a `diamond_sword`, `diamond_axe` y
`diamond_spear`. El netherite queda por encima del máximo y se recorta a 200.

### Que el arma pese lo mismo que la armadura

```yaml
weights:
  armor: 1
  weapon: 1
```

### Que los encantamientos pesen menos que el material

En `cosmicniv_valores.yml`, bajá los `points-per-level` (por ejemplo a la mitad todos). Como solo importa la
proporción, es lo mismo que duplicar los valores de `armor`, `weapons` y `tools`.

### Sumar los arcos como familia de armas

En `cosmicniv_valores.yml`:

```yaml
weapons:
  # ... las que ya están ...
  bow:
    bow: 6.0
    crossbow: 6.0

enchantments:
  power: { points-per-level: 1.0, max-level: 5 }
  punch: { points-per-level: 0.5, max-level: 2 }
  flame: { points-per-level: 1.0, max-level: 1 }
  infinity: { points-per-level: 1.0, max-level: 1 }
  quick_charge: { points-per-level: 0.5, max-level: 3 }
  multishot: { points-per-level: 1.0, max-level: 1 }
  piercing: { points-per-level: 0.5, max-level: 4 }
```

Y en `cosmicniv_lib.yml`:

```yaml
references:
  max:
    weapons:
      # ... las que ya están ...
      bow:
        item: bow
        enchantments: { power: 5, punch: 2, flame: 1, infinity: 1, unbreaking: 3 }
```

Ojo: con esto un arco full encantado también da `W = 1`, igual que una espada full.

### Que solo cuente lo que tiene en la mano

```yaml
held-search: MAIN_HAND
```

### Números enteros

```yaml
output:
  decimals: 0
```

---

## 9. Errores y avisos

**Errores.** Pasan cuando algo hace imposible el cálculo:
- una clave que no existe, por ejemplo `bigcasares_item` en vez de `bigcasares-item` o `enchants` en vez de
  `enchantments` (así un error de tipeo nunca se ignora en silencio);
- un ítem o encantamiento mal escrito, o un número que no es número (o que pasa de ±1 000 000);
- una clave repetida (`sword` y `Sword`, o `netherite_sword` y `minecraft:netherite_sword`);
- un ítem en dos lugares;
- una referencia que no tiene valor, un arma en la familia equivocada o usada como armadura;
- la "max" sin más poder que la "zero";
- un objeto custom sin `match`, con una familia que no existe o con categoría inválida;
- una curva con parámetros inválidos;
- un archivo YAML roto.

Cada error dice **el archivo y la ruta exacta**, por ejemplo:

```
[CosmicNiv] cosmicniv_lib.yml > references.max.weapons.sword: minecraft:netherite_axe belongs to weapons.axe, not weapons.sword
[CosmicNiv] cosmicniv_valores.yml > weapons.axe.minecraft:netherite_sword: minecraft:netherite_sword is already listed under weapons.sword.minecraft:netherite_sword; an item can only be in one place
[CosmicNiv] cosmicniv_valores.yml > custom-items.pico_magico.family: unknown tool family pico (use one declared under tools:)
```

Qué pasa cuando hay errores:

| Momento | Qué hace |
|---|---|
| Al arrancar el servidor | Muestra todos los errores y usa los **valores de fábrica** hasta que lo arregles y recargues |
| `/cosmicniv reload` o `/bigcasares reload` | Muestra los errores (el comando, los primeros 5; la consola, todos) y **mantiene la última configuración que funcionó** |

Así, un error de tipeo nunca deja a los otros módulos sin nivel.

**Avisos.** No frenan nada y van a la consola:
- un ítem o encantamiento que no existe en el servidor (por ejemplo `netherite_swrod`): simplemente nunca va a
  coincidir con nada;
- falta la sección `enchantments`;
- una referencia usa un encantamiento que no tiene valor en `enchantments` (suma 0);
- `held-search: MAIN_HAND` con peso en armas y en herramientas: con una sola mano nunca cuentan las dos juntas, así
  que no se puede llegar al máximo.

---

## 10. Usar la librería desde otro módulo de BigC

`cosmicniv_lib` es un módulo **dentro** del jar de BigC. No es un plugin aparte ni una dependencia de `build.gradle`,
así que no hay que declarar nada en `plugin.yml` ni en Gradle. Como pide `GUIDELINES.MD`, los módulos hablan entre sí
por los getters de `BigCasares`.

### Lo básico

```java
import dev.linqfy.bigCasares.modules.cosmicniv.CosmicNivModule;

CosmicNivModule niv = plugin.getCosmicNivModule();
if (niv != null && niv.isAvailable()) {
    double nivel = niv.level(player);          // 0.0 a 200.0, ya redondeado
}
```

`isAvailable()` es `false` si el módulo está apagado en `config.yml`. En ese caso los demás métodos tiran
`IllegalStateException`, así que siempre conviene chequearlo o tener un valor por defecto.

### El detalle

```java
CosmicNivResult r = niv.evaluate(player);

r.value();             // nivel redondeado (lo mismo que level())
r.exactValue();        // nivel sin redondear
r.maximum();           // 200.0, o lo que diga output.maximum
r.armorScore();        // A, de 0 a 1
r.weaponScore();       // W, de 0 a 1
r.toolScore();         // T, de 0 a 1 (se calcula aunque su peso sea 0)
r.progress();          // t, antes de la curva
r.curvedProgress();    // f(t)

r.hasWeapon();         // si encontró un arma que cuente
r.weapon().family();   // "sword", "mace"... o null
r.weapon().item();     // CosmicNivItem con la clave y los encantamientos, o null
r.weapon().power();    // poder de esa arma
r.tool();              // lo mismo para la mejor herramienta
```

### Funciona con mobs

`level` y `evaluate` reciben cualquier `LivingEntity`. Un zombie con armadura de hierro y espada de diamante tiene su
propio nivel. Sirve, por ejemplo, para dar más recompensa por matar mobs bien equipados.

### Evaluar un equipo armado a mano

Para kits, la vista previa de una tienda o una tabla de spawn de un boss, sin que nadie lo tenga puesto:

```java
CosmicNivLoadout kit = CosmicNivLoadout.builder()
    .armor(CosmicNivArmorSlot.HELMET, CosmicNivItem.of("diamond_helmet", Map.of("protection", 4)))
    .armor(CosmicNivArmorSlot.CHESTPLATE, CosmicNivItem.of("diamond_chestplate"))
    .held(CosmicNivItem.of("iron_axe", Map.of("sharpness", 3)))
    .held(CosmicNivItem.of("custom:sahurs_bat"))
    .build();

double nivelDelKit = niv.evaluate(kit).value();
```

### El poder y la categoría de un solo ítem

```java
CosmicNivService s = niv.service();
s.itemPower(CosmicNivItem.of("netherite_sword", Map.of("sharpness", 5)));   // 13.0
s.categoryOf(CosmicNivItem.of("shears"));                                    // TOOL
s.familyOf(CosmicNivItem.of("trident"));                                     // "trident"
```

### Reglas para usarlo bien

- **Hilo principal.** `level(entity)`, `evaluate(entity)` y `readLoadout(entity)` leen el inventario, así que se
  llaman desde el hilo principal (eventos, comandos, tareas sync). `evaluate(loadout)` no toca Bukkit y se puede usar
  desde cualquier hilo.
- **No guardes ni el módulo ni `service()`.** `/bigcasares reload` crea un `CosmicNivModule` nuevo y la instancia
  vieja deja de funcionar (tira `IllegalStateException`). Pedí `plugin.getCosmicNivModule()` cada vez que lo
  necesites; es solo un getter.
- **Es barato.** Son unas sumas sobre como mucho 4 piezas y 37 ítems. Llamarlo en cada evento no pesa, pero si lo
  necesitás cada tick para todos los jugadores, calculalo cada pocos segundos.
- **No hay valores guardados.** El nivel se calcula en el momento. Si querés historial o ranking, eso va en tu módulo.

### Ejemplo completo: escalar la vida de un boss según el equipo de los jugadores cercanos

```java
CosmicNivModule niv = plugin.getCosmicNivModule();
double multiplicador = 1.0;                              // sin la librería, vida normal
if (niv != null && niv.isAvailable() && !jugadores.isEmpty()) {
    double promedio = 0.0;
    for (Player jugador : jugadores) {
        promedio += niv.level(jugador);
    }
    promedio /= jugadores.size();
    multiplicador = 1.0 + promedio / niv.maximum();      // de x1 (nadie equipado) a x2 (todos al máximo)
}
```

---

## 11. Límites y preguntas frecuentes

**¿Cuenta la durabilidad?** No. Una espada a punto de romperse vale lo mismo que una nueva.

**¿Cuentan los efectos de poción, la vida extra o la absorción?** No, solo el equipo. Jeremy tiene su propio cálculo
de poder, que sí los incluye.

**¿Ítems custom de BigC?** Si no están en `custom-items:`, se miden por su material de base y sus encantamientos.
Agregalos ahí para darles su propio valor (sección 7).

**¿Jugadores de Bedrock?** Funciona igual: el cálculo se hace en el servidor con el inventario real.

**Encanté algo de más con comandos y no sube.** Es a propósito: `max-level` corta, y el resultado nunca pasa de 200.

**¿Puede dar más de 200?** No. Lo que supera la referencia "max" se recorta.

**Cambié la curva y la referencia max ya no da 200.** No debería pasar: todas las curvas dan `f(1) = 1`. Revisá con
`/cosmicniv` que el equipo sea exactamente el de la referencia, que las armas estén donde busca `held-search` y, si
las herramientas tienen peso, que también haya una herramienta de referencia.

**¿Por qué mi hacha de diamante sin encantar da tan poco?** Porque se compara contra un hacha de netherite **full
encantada**. Los encantamientos pesan bastante: si querés que pese más el material, ver la receta de la sección 8.

**¿Por qué el hacha no está en `tools`?** Porque cada ítem va en un solo lugar y el hacha es un arma. Si en tu servidor
el hacha es más herramienta que arma, movela de `weapons.axe` a una familia de `tools` y mové también su referencia.

---

## 12. Para quien mantiene el código

### Clases

| Clase | Qué hace |
|---|---|
| `CosmicNivModule` | Ciclo de vida, carga de los dos archivos con respaldo, API pública |
| `CosmicNivService` | El modelo matemático y el reconocimiento de objetos custom. Sin Bukkit, inmutable |
| `CosmicNivSettings` / `CosmicNivSettingsLoader` / `CosmicNivConfigLoadResult` | Configuración, lectura y validación de los dos archivos |
| `CosmicNivCurve` / `CosmicNivCurveType` | Las curvas (interfaz sellada, un record por tipo) |
| `CosmicNivCustomItem` / `CosmicNivCustomMatch` / `CosmicNivItemIdentity` | Objetos custom y cómo se reconocen |
| `CosmicNivItem`, `CosmicNivLoadout`, `CosmicNivArmorSlot`, `CosmicNivItemCategory`, `CosmicNivReference`, `CosmicNivEnchantmentValue`, `CosmicNivHeldSearch`, `CosmicNivResult`, `CosmicNivKeys` | Tipos de valor |
| `BukkitCosmicNivEquipmentReader` | `LivingEntity` → `CosmicNivLoadout` (lee material, `bigcasares:item_id`, `item_model` y datos persistentes) |
| `BukkitCosmicNivKeyAudit` | Avisa de ítems o encantamientos que no existen en el servidor |
| `CosmicNivCommand` | `/cosmicniv` |

### Agregar un tipo de curva

1. Agregá el valor a `CosmicNivCurveType`.
2. Agregá un record en `CosmicNivCurve` que valide sus parámetros en el constructor y cumpla `f(0) = 0`, `f(1) = 1`
   y que nunca baje.
3. Leelo en `CosmicNivSettingsLoader.readCurve`.
4. Sumalo a `everyCurve()` en `CosmicNivCurveTest`: los tests de extremos y de que nunca baje lo cubren solos.
5. Documentalo en la sección 4 de esta guía y en los comentarios de `cosmicniv_lib.yml`.

### Agregar una forma de reconocer objetos custom

1. Agregá el dato a `CosmicNivItemIdentity` y la condición a `CosmicNivCustomMatch` (incluido `matches`).
2. Leelo del `ItemStack` en `BukkitCosmicNivEquipmentReader.toItem`.
3. Leelo del YAML en `CosmicNivSettingsLoader.readMatch`.
4. Testealo en `CosmicNivCustomItemTest` y documentalo en la sección 7.

### Tests

```bash
./gradlew test --tests "dev.linqfy.bigCasares.modules.cosmicniv.*"
```

- `CosmicNivCurveTest`: extremos, que nunca bajen, fórmulas y parámetros inválidos.
- `CosmicNivSettingsLoaderTest`: los dos archivos de fábrica y cada tipo de error con su archivo y ruta.
- `CosmicNivServiceTest`: 0 sin nada, 200 con cada arma máxima, pesos, herramientas, normalización por familia,
  topes, maldiciones, referencias "zero" propias, curvas, redondeo y **los ejemplos de esta guía**. Si cambiás
  valores de fábrica, ese test te avisa que hay que actualizar las tablas de la sección 3.
- `CosmicNivCustomItemTest`: cómo se reconocen los objetos custom y dónde cuentan.
- `CosmicNivModuleWiringTest`: id estable, archivos incluidos en el jar, `plugin.yml`, `config.yml` y registro en
  `BigCasares`.

Diseño completo: `docs/superpowers/specs/2026-10-13-cosmicniv-lib-design.md`.

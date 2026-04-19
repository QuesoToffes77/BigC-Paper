# CopperApple Plugin v1.1 — Documentación Completa

## Índice
1. [Estructura del proyecto](#1-estructura)
2. [Arquitectura de seguridad PDC](#2-seguridad)
3. [Cómo compilar](#3-compilar)
4. [Instalación en el servidor](#4-instalacion)
5. [Resource Pack](#5-resourcepack)
6. [Receta de crafteo](#6-receta)
7. [Sistema de cooldown](#7-cooldown)
8. [Permisos y comandos](#8-comandos)
9. [Cómo agregar más ítems](#9-extensibilidad)
10. [Notas técnicas](#10-notas)

---

## 1. Estructura del proyecto

```
CopperApplePlugin/
├── pom.xml
├── src/main/
│   ├── java/com/copperapple/
│   │   ├── CopperApplePlugin.java           ← Clase principal + receta + NamespacedKeys
│   │   ├── items/
│   │   │   ├── CustomItem.java              ← Interfaz base con verificación PDC+CMD
│   │   │   ├── CustomItemRegistry.java      ← Registro central
│   │   │   └── CopperAppleItem.java         ← Implementación del ítem
│   │   ├── listeners/
│   │   │   ├── ItemConsumeListener.java     ← Consumo + cooldown anti-abuso
│   │   │   └── CraftListener.java          ← PrepareItemCraftEvent (blindaje del crafteo)
│   │   └── commands/
│   │       └── CopperAppleCommand.java      ← /copperapple
│   └── resources/
│       └── plugin.yml
└── resourcepack/
    ├── pack.mcmeta
    └── assets/minecraft/
        ├── models/item/
        │   ├── apple.json                   ← Override custom_model_data 1001
        │   └── copper_apple.json            ← Modelo del ítem custom
        └── textures/item/
            └── copper_apple.png             ← (crear manualmente, 16×16 px)
```

---

## 2. Arquitectura de seguridad (PDC)

### Por qué PDC y no solo CustomModelData

CustomModelData puede ser replicado por jugadores con acceso a un anvil o cliente con NBT editors.
PersistentDataContainer es metadata server-side pura: el servidor la escribe, ningún cliente puede modificarla.

### Doble capa de verificación (en CustomItem.matches)

```
  ItemStack recibido
        │
        ▼
  ¿item == null?  ──YES──▶  false (rechazado)
        │NO
        ▼
  ¿hasItemMeta?   ──NO──▶   false
        │YES
        ▼
  ¿PDC contiene   ──NO──▶   false  ◀── CAPA PRINCIPAL
  NamespacedKey?             (falsificación bloqueada aquí)
        │YES
        ▼
  ¿PDC.get() == 1 ──NO──▶   false
        │YES
        ▼
  ¿hasCustomModel ──NO──▶   false
  Data?
        │YES
        ▼
  ¿CMD == 1001?   ──NO──▶   false  ◀── DEFENSA SECUNDARIA
        │YES
        ▼
       true ✓ (ítem auténtico)
```

### NamespacedKey centralizada

La key `"copperapple:copper_apple"` se define UNA vez en `CopperApplePlugin.onEnable()` y se inyecta a todos los ítems vía constructor. Todos los componentes usan exactamente la misma key.

---

## 3. Cómo compilar

**Requisitos:** Java JDK 17+ y Apache Maven 3.8+

```bash
cd CopperApplePlugin
mvn clean package
# Resultado: target/CopperApple-1.0.0.jar
```

Para cambiar la versión objetivo de Paper, editar en `pom.xml`:
```xml
<paper.version>1.21.1-R0.1-SNAPSHOT</paper.version>
```

---

## 4. Instalación en el servidor

```
tu-servidor/plugins/CopperApple-1.0.0.jar
```

Reiniciar (NO usar /reload):
```
stop
# volver a iniciar
```

Salida esperada en consola:
```
[CopperApple] ╔══════════════════════════════════╗
[CopperApple] ║   CopperApple Plugin activado!   ║
[CopperApple] ║   Items registrados: 1           ║
[CopperApple] ╚══════════════════════════════════╝
[CopperApple] Receta de Manzana de Cobre registrada correctamente.
```

---

## 5. Resource Pack

### Crear la textura

1. Crear imagen 16×16 px (GIMP, Aseprite, Paint.NET, etc.)
2. Diseñar la textura (tonos naranja/cobre)
3. Guardar como `copper_apple.png` en `resourcepack/assets/minecraft/textures/item/`

### pack_format por versión de Minecraft

| Minecraft | pack_format |
|-----------|-------------|
| 1.20.1    | 15          |
| 1.20.2    | 18          |
| 1.20.3-4  | 22          |
| 1.21.x    | 34          |
| 1.21.4    | 46          |

### Empaquetar como ZIP

Seleccionar el CONTENIDO de `resourcepack/` y comprimir. El ZIP debe tener esta estructura interna:
```
pack.mcmeta
assets/minecraft/models/item/apple.json
assets/minecraft/models/item/copper_apple.json
assets/minecraft/textures/item/copper_apple.png
```

### Vincular en server.properties

```properties
resource-pack=https://tudominio.com/CopperApple_RP.zip
resource-pack-sha1=<hash SHA1 del zip>
resource-pack-prompt=§6Instalar resource pack para texturas personalizadas
require-resource-pack=false
```

Obtener SHA1:
```bash
sha1sum CopperApple_RP.zip           # Linux/Mac
Get-FileHash CopperApple_RP.zip -Algorithm SHA1  # Windows PowerShell
```

---

## 6. Receta de crafteo

```
C C C
C A C       C = COPPER_INGOT
C C C       A = APPLE (vanilla)
```

El resultado es el ítem auténtico con PDC + CustomModelData.
`CraftListener` (PrepareItemCraftEvent) verifica que el resultado siempre
tenga el PDC correcto, incluso si otro plugin interfirió con el inventario.

---

## 7. Sistema de cooldown

- **Duración por defecto:** 5 segundos (`COOLDOWN_MS = 5_000L` en `ItemConsumeListener`)
- **Por jugador** (UUID): el cooldown es individual, no global
- **Comportamiento:** el ítem se consume normalmente, pero los efectos se bloquean durante el cooldown (evita el exploit de devolver el ítem)
- **Reset al reiniciar:** el mapa de cooldowns vive en RAM

Para cambiar la duración, editar en `ItemConsumeListener.java`:
```java
private static final long COOLDOWN_MS = 10_000L; // 10 segundos
```

---

## 8. Permisos y comandos

### Comandos

| Comando | Descripción |
|---------|-------------|
| `/copperapple` | 1 ítem para el ejecutor |
| `/copperapple <jugador>` | 1 ítem para otro jugador |
| `/copperapple <jugador> <cantidad>` | N ítems (1–64) |
| `/ca` o `/capple` | Alias |

### Permisos

| Permiso | Default | Descripción |
|---------|---------|-------------|
| `copperapple.*` | OP | Todos los permisos |
| `copperapple.give` | OP | Uso para sí mismo |
| `copperapple.give.others` | OP | Dar a otros (incluye .give) |

### LuckPerms

```bash
lp group default permission set copperapple.give true
lp group admin permission set copperapple.* true
```

---

## 9. Cómo agregar más ítems personalizados

### Paso 1: Crear la clase

```java
package com.copperapple.items;

import com.copperapple.CopperApplePlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.Arrays;

public class IronAppleItem implements CustomItem {

    public static final int MODEL_DATA = 1002;
    private final NamespacedKey namespacedKey;

    public IronAppleItem(CopperApplePlugin plugin) {
        this.namespacedKey = new NamespacedKey(plugin, "iron_apple"); // key única
    }

    @Override public int getCustomModelData() { return MODEL_DATA; }
    @Override public NamespacedKey getNamespacedKey() { return namespacedKey; }

    @Override
    public ItemStack buildItemStack() {
        ItemStack item = new ItemStack(Material.APPLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7Manzana de Hierro");
        meta.setLore(Arrays.asList("§8Resistencia de acero"));
        meta.setCustomModelData(MODEL_DATA);
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.INTEGER, 1); // OBLIGATORIO
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onConsume(Player player, ItemStack item) {
        // lógica de efectos aquí
    }
}
```

### Paso 2: Registrar en CustomItemRegistry

```java
register(new IronAppleItem(plugin));
```

### Paso 3: Override en apple.json del resource pack

```json
"overrides": [
  { "predicate": { "custom_model_data": 1001 }, "model": "minecraft:item/copper_apple" },
  { "predicate": { "custom_model_data": 1002 }, "model": "minecraft:item/iron_apple" }
]
```

### Paso 4: Recompilar

```bash
mvn clean package
```

---

## 10. Notas técnicas

**¿Por qué el ítem se consume en cooldown sin devolver?**
Si se cancelara el evento o se devolviera el ítem, sería explotable: coleccionar ítems sin gastarlos. Al consumir normalmente pero bloquear efectos, el sistema es seguro.

**¿Por qué strikeLightningEffect y no strikeLightning?**
`strikeLightning` causa daño, incendia bloques y asusta mobs. `strikeLightningEffect` es solo animación visual. Para efectos cosméticos siempre usar `strikeLightningEffect`.

**¿Por qué Bukkit.removeRecipe en onDisable?**
Previene duplicación de recetas si el plugin se recarga (PlugMan, etc.).

**¿Por qué ignoreCancelled = true en los listeners?**
Si otro plugin canceló el evento (región protegida, anti-spam), no aplicamos efectos en ese contexto bloqueado.

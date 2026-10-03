## Revisión de Código Completa (Code Review) — PR #11

**Estado de la revisión:** APROBADO (con parches de estabilización y balance aplicados)

Esta pull request representa una integración de gran escala (463 archivos) que incorpora múltiples sistemas centrales para el servidor: **Acid Rain, Blood Moon, Jeremy, Glider, Grappling Hook, Copper Apple y Airdrop**, junto con la renovación de anatomía y animaciones para los modelos BetterModel de criaturas ácidas.

A continuación se detalla la auditoría técnica por módulo, el diagnóstico de calidad y las correcciones de balance/estabilidad aplicadas para su despliegue en producción.

---

### 1. Auditoría Técnica por Módulo

#### A. Acid Rain (Lluvia Ácida) & Modelos
* **Modelos y Texturas:** Los ajustes anatómicos (orden Euler ZYX, articulaciones de patas, dientes, unión de cuerda del arco) están correctos y verificados con las pruebas geométricas de regresión.
* **Jugabilidad & Balance:**
  * La versión inicial introducía una erosión agresiva de hasta 6000 bloques/evento sobre materiales de construcción de jugadores (madera, piedra, vidrio, cobre) sin integración con plugins de claims.
  * Se configuró **la destrucción de bloques en `false`** para garantizar 100% la integridad de las bases de los jugadores.
  * La duración se acotó a **3-4 minutos** con aviso de 30s y frecuencia de 90-150 min (100% predecible).
  * El daño se redujo a 0.5 - 1.0 corazón sin efecto de veneno paralizante (`POISON`), haciendo el evento casual, atmosférico y sobrevivible con comida básica.
  * Se añadió soporte de armadura completa de **diamante (70% de protección)** junto a cuero (25%), hierro (50%) y netherite (85%).
  * Se desactivó el ActionBar redundante, dejando la **BossBar** como canal limpio de HUD.
* **Comando Reload:** Se desacopló `/acidrain reload` de `plugin.reloadConfig()`; ahora lee su sección de forma aislada sin desincronizar los ajustes en memoria del resto de módulos.

#### B. Blood Moon (Luna de Sangre)
* **Lifecycle & Despawn:** Se corrigió la fuga de entidades donde `spawner.clearWorld()` solo borraba el ledger en memoria al terminar la noche. Ahora se iteran los UUIDs registrados en el ledger y se remueven del mundo las entidades vivas marcadas antes de resetear el estado.
* **Configuración Inicial:** Se configuró la probabilidad automática en 0.0 por defecto hasta realizar pruebas de carga con población real.

#### C. Airdrop
* **Despawn Race Condition:** Se solucionó el race condition en `scheduleDespawn()`, capturando el `UUID dropId` y validando coincidencia de ID y posición antes de remover el cofre, además de cancelar explícitamente la tarea al reclamar el airdrop.
* **Aterrizaje Dinámico:** `AirdropFallingTask` ahora revalida el suelo en tiempo real al impactar (`getHighestBlockYAt()`) y busca espacio de aire seguro para evitar cofres enterrados, flotantes o sobreescritura de bloques.
* **Presupuesto de Chunks:** `AirdropService` y `BukkitAirdropWorldGateway` ahora respetan `isChunkLoaded()` para evitar congelamientos por carga masiva de chunks distantes; el cleanup de defensores omite chunks descargados.

#### D. Grappling Hook
* **Progresión de Tiers:** Se corrigió `config.yml` donde los valores (10/35/500/100/125/150) rompían la escala de distancia y provocaban clamping forzado. La progresión quedó alineada con el enum: 50 / 75 / 100 / 125 / 150 / 200 bloques.

#### E. Aislamiento entre Módulos y MobScaling
* **Compatibilidad de Mobs Custom:** Se implementó `isEventOrSystemMob()` en `MobScalingListener`, reconociendo etiquetas PDC de Blood Moon, Acid Rain, Airdrop, PvE Bosses, Shop NPCs y tumbas, evitando que `MobScaling` duplique o sobre-escale estadísticas en mobs de sistema.

#### F. Integración Continua (CI)
* Se actualizó `.github/workflows/build-jar.yml` de `clean jar` a `clean build`, garantizando que GitHub Actions ejecute los 967+ tests en cada commit y suba artefactos con reportes de prueba en caso de fallo.

---

### 2. Verificación de Compilación y Pruebas

* **Compilación:** Java 25 / Paper 26.2 (`26.2.build.56-alpha`) — `BUILD SUCCESSFUL`.
* **Pruebas Automatizadas:** 100% de la suite de pruebas unitarias e integración ejecutadas y aprobadas (`clean test` exitoso en 29s sin fallos).
* **Compatibilidad de Recursos:** Resource packs Java (`bigcasares-java.zip`) y Bedrock (`bigcasares-bedrock.mcpack`) generados correctamente sin discrepancias.

---

### 3. Conclusión

El trabajo presentado en esta PR es sólido y aporta un valor sustancial al servidor. Con los parches de balance, seguridad ambiental y corrección de bugs aplicados en la rama principal, el código queda aprobado y listo para merge.

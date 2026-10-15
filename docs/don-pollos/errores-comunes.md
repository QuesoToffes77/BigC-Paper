# Errores que cometimos en Don Pollos (y cómo arreglarlos)

Lista corta de los errores que linqfy marcó en la revisión del PR #12 (y uno del CI), con la regla para no repetirlos.
Sirve para revisar cualquier módulo nuevo de BigC antes de abrir el PR.

## Seguridad y trampas (exploits)

1. **Devolver la apuesta si el jugador se desconecta.** Quien se iba justo antes de perder no perdía nada, y se podía
   duplicar cualquier objeto.
   *Regla:* si el resultado ya está decidido, se paga según ese resultado, esté conectado o no.
2. **El sonido o la animación delatan el resultado.** La duración del giro dependía de dónde frenaba la bola.
   *Regla:* lo que el cliente ve u oye no tiene que depender del resultado antes de tiempo.
3. **Aceptar todas las teclas como respuesta correcta** (`contains`). Apretar todo a la vez siempre acertaba, y se
   podía ganar con una macro.
   *Regla:* validar la respuesta exacta, no "incluye la correcta".
4. **El premio lo podía cobrar cualquiera si el ganador estaba desconectado.** Se comparaba con
   `Bukkit.getPlayer(uuid)`, que es `null` si el jugador está offline.
   *Regla:* autorizar siempre comparando el UUID guardado, nunca con "el jugador está conectado".
5. **Saltarse un casco con Maldición de ligamiento** usando un ítem que cambia el casco.
   *Regla:* todo lo que reemplaza equipo respeta la maldición (salvo en creativo).
6. **Escaparse de un encuentro** con perlas, fruta chorus, comandos de teletransporte, elytras, tridente con Corriente o
   cargas de viento, y dejar el encuentro trabado.
   *Regla:* si un encuentro encierra al jugador, bloquear todas las salidas y cerrar el encuentro si queda vacío.

## Ciclo de vida del módulo (apagar, recargar, reiniciar)

7. **Menús abiertos después de apagar el módulo.** Quedaban sin protección de clicks y se podía sacar el ítem del menú.
   *Regla:* cerrar los menús antes de sacar sus listeners. Las limpiezas de BigC corren **al revés** del orden en que se
   registran: lo que tiene que correr primero se registra último.
8. **Trabajo asíncrono que termina después del apagado** (carga de chunks): programaba tareas y reservaba chunks en un
   módulo ya apagado.
   *Regla:* envolver los callbacks con el guard del ciclo de vida de BigC (`registrations.guard`, basado en
   `RuntimeGeneration`), no con una bandera propia.
9. **Un registro guardado a medias rompía el arranque.** Una clave `manual` se leía como número
   (`NumberFormatException`) y el módulo no arrancaba nunca más.
   *Regla:* un registro roto se anota en el log y se saltea; nunca debe impedir que arranque el módulo.

## Datos guardados

10. **Guardar datos como texto armado a mano** (`uuid;4;-2=hecha`, `x,z,giro,...`) y cortarlo después.
    *Regla:* un archivo con campos con nombre y un número de `version:`, y migrar los viejos (guardando una copia).
11. **Medir distancias desde (0, 0)** en vez de desde el spawn real del mundo.
    *Regla:* toda distancia "al spawn" usa el spawn del mundo.

## Terreno y mundo

12. **Restaurar bloques en el orden equivocado.** Los pozos de chunks vecinos comparten paredes: restaurarlos en el orden
    en que se cavaron borraba terreno original.
    *Regla:* deshacer en orden inverso (último cambio primero), y recién después mover jugadores.
13. **Pisar el terreno de otro boss** y **cavar en zonas protegidas.**
    *Regla:* antes de modificar terreno, consultar las zonas protegidas (nexus) y las áreas que ya usa otro encuentro;
    si no se puede, buscar el lugar permitido más cercano.

## Combate

14. **Daño al jugador equivocado.** El láser dañaba al objetivo original aunque otro jugador se cruzara delante, incluso
    detrás de una pared.
    *Regla:* el daño va a quien el rayo realmente toca, una sola vez por jugador.

## Calidad de código

15. **Un ternario que convierte cualquier producto nuevo en galleta.**
    *Regla:* usar un `switch` completo sobre el enum: al agregar un caso, el código no compila hasta decidirlo.
16. **Descargas de plugins no reproducibles ni seguras** en `build.gradle`: `latest`, sin checksum, y un archivo cortado
    se quedaba como válido.
    *Regla:* versión fija, checksum oficial (sha256/sha512), bajar a un archivo temporal, verificar y recién ahí mover.
    Si upstream solo publica en un tag mutable (`latest`), fijar el id del archivo más su digest: el build falla si lo
    republican, en vez de cambiar en silencio.
17. **Una clase gigante** (el coordinador del boss tenía unas 1800 líneas).
    *Regla:* dividir por responsabilidad (ataques, terreno, fases, premio) moviendo el código sin cambiarlo, y comprobar
    que no cambió el comportamiento.
18. **Tests solo de reglas puras.** Nada probaba lo que pasa con eventos de Bukkit.
    *Regla:* además de las reglas puras, probar los listeners con eventos reales y objetos falsos (`Proxy`), como ya se
    hace en otros módulos de BigC.

## Balance (decisiones de diseño, no bugs)

19. **Recompensas repetibles sin límite** (5 diamantes por baile, 4 stacks de cualquier cosa, 256 bloques de netherite).
    *Regla:* premios chicos, un ítem de cada uno, sin materiales que rompen la progresión, y dificultad que crece con
    cada victoria.
20. **Un jackpot que casi no paga.** El verde de la ruleta (2/50) pagaba x2: devolvía el 8% en promedio.
    *Regla:* calcular el retorno esperado (chance × multiplicador) y compararlo con el resto de las apuestas.
21. **Invocar al jefe era casi gratis** (5 panes).
    *Regla:* el costo de lo que invoca un jefe tiene que estar a la altura de lo que da.

## CI y entregas

22. **Un archivo ignorado por `.gitignore` nunca llegó al PR.** Un `*.zip` global ignoraba `geysermodelengine.zip`; en la
    computadora local el test pasaba, en el CI fallaba.
    *Regla:* antes de abrir el PR, probar en una copia limpia (`git clean -fdx` y `./gradlew clean build`) y mirar
    `git status --ignored` en las carpetas de recursos.
23. **Dependencia que faltaba** (`packetevents` para GeyserModelEngine) y **un archivo que el plugin de Geyser reescribe
    en cada arranque** (`config.json`), que hacía pedir reinicio siempre.
    *Regla:* anotar las dependencias de cada plugin externo, y no pisar archivos que otro plugin reescribe.
24. **Conflictos de merge por tocar los mismos lugares** (`BigCasares.java`, `config.yml`) que otro módulo.
    *Regla:* ante un conflicto de módulos, dejar los dos y correr el build completo con ambos.
25. **La descripción del PR decía algo que ya no era cierto** ("sin cambios en `build.gradle`").
    *Regla:* releer la descripción del PR cada vez que se agregan commits.

## Antes de abrir un PR en BigC

- [ ] `./gradlew clean build` en una copia limpia.
- [ ] Ningún listener sin su `unregister`, y los menús se cierran antes.
- [ ] Todo callback de otro hilo va con el guard del módulo.
- [ ] Ninguna comparación con `Bukkit.getPlayer(...)` para decidir permisos.
- [ ] Ningún valor del resultado se filtra antes de tiempo (sonidos, animaciones, mensajes).
- [ ] Ningún dato guardado a mano: campos con nombre y versión.
- [ ] Probado en el juego, y si no se pudo, decirlo en el PR.

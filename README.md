```                                        
                       ▒▒▒▒▒▒▒                      
                 ▒▒▒▒▒▒▒▒▒▒▒░░░                     
                 ░░▒▒▒░░░░░░░▒▒                     
                ░░░░▒░░░░░░░░░▒▒                    
                ░░░░░░░░░░░░░░▒▒▒░░░░░░▒▒▒▒         
                 ░░░░░░░░░░░░░░▒▒░░░░▒▒▒▒▒▒▒        
         ▒▒░░░░░ ░░░░▒▒▒▒▒░▒░░▒▒▒░░▒▒▒▒▒▒▒▒▒▒       
        ▒▒░░░░░░░░░▒▒░▒▒▒▒░▒░▒▒▒▒░▒▒▒▒▒░░░░▒▒▒      
      ▒▒▒▒▒░░░░░░▒░░▓▒▓▓▓▓▓▓▓▒▒▒▒░▒░░░░░░░▒▒▒       
      ▒▒▒▒▒░░░░░░▒▒▒▒▒▒▓▓██▓▓▒▒▒▒░░░░░░▒▒▒▒▒▒       
       ▒▒▒▒░░░░░░░░░░▒▒▓█▓▓█▓▓▒░░░░░▒▒▒▒▒▒▒         
      ▒▒▒▒░░░░░░░▒░░▒▒▒▓▓██▓▓▓▓▓▒▒▒░▒▒▒▒▒▒          
      ▒▒▒▒░░░░░░░░░▒▒▒▒▓▓▓▓▓▓▓▓▒▒▒▒▒▒▒▒▒▒▒          
       ▒▒░░░░░░░░░░▒▒▒▒▒▒▒▓▒▓▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒        
         ░▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒░▒▓▒▒▒▒▒▒▒▒▒▒▒▒▒▒        
                ▒▒▒▒▒▒▒▒▒▒▒░░▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒       
                ▒▒▒▒▒▒▒▒▒▒░░░▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒        
                ░▒▒▒▒▒▒▒░░░░▒▒▒▒▒▓▒▒▒▒▒▒▒▓▓         
                ░░▒▒░░░░░░░▒▒▒  ▓▓▒▒▒▒▒▒            
                 ▒▒░▒░░░▒▒▒▒▒                       
                  ▒▒▒▒▒▒▒▒▒                         
```

## Plataforma soportada

- Minecraft Java 26.2.
- Paper 26.2 build 56 (`26.2.build.56-alpha`).
- Java 25.
- Gradle Wrapper 9.2.1.

Paper todavía publica 26.2 como build alpha. El plugin está fijado al build 56 para que las compilaciones sean reproducibles.

## Build

```powershell
.\gradlew.bat clean build
```

El build valida y genera automáticamente:

- `build/generated-resourcepacks/bigcasares-java.zip`
- `build/generated-resourcepacks/bigcasares-bedrock.mcpack`
- `build/libs/BigCasares-0.0.1.jar`

Vault y un proveedor de economía son necesarios para Mission, Bounty y Entity Shop. PlaceholderAPI y Geyser son integraciones opcionales con fallback seguro.

## cosmicniv_lib

Librería compartida (módulo `cosmicniv-lib`) que resume el equipo de un jugador o un mob en un nivel de **0 a 200**:
0 es no tener nada y 200 es netherite full encantada con un arma full encantada (espada, hacha, maza, lanza o tridente).

- **Config:** `plugins/BigCasares/cosmicniv_lib.yml` define cómo se calcula: qué vale 0, qué vale 200, los pesos y la curva (lineal, exponencial, logarítmica, potencia, en S o por puntos).
- **Valores:** `plugins/BigCasares/cosmicniv_valores.yml` define cuánto vale cada armadura, arma, herramienta, encantamiento y objeto custom (de BigC o de otros plugins), sin tocar código.
- **Comando:** `/cosmicniv [jugador|curva|reload]` (alias `/cniv`).
- **Desde otro módulo:** `plugin.getCosmicNivModule().level(player)`.

Guía completa (fórmula, ejemplos, cada clave del archivo y la API): [`docs/cosmicniv-lib/README.md`](docs/cosmicniv-lib/README.md).

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

# Herramientas de Mutango

Se corren desde la raiz del repo, con Python 3 y Pillow (`pip install pillow`). Cada generador da **exactamente** los
mismos archivos que ya estan en el proyecto (los `.bbmodel` salen byte por byte iguales; el zip de Bedrock y las ruinas
tambien). Las fuentes que leen (texturas de referencia y el litematic) estan en `fuentes/`.

## Los modelos (escriben en `src/main/resources/bettermodel/models/`)

| Comando | Que genera |
|---|---|
| `python tools/mutango/generar_mutango.py` | Lef, Lihuen y Lican (`bigcasares_boss_mutango*.bbmodel`) y sus estatuas dormidas (`*_dormido`), con sus animaciones. |
| `python tools/mutango/generar_neguechen.py` | Neguechen y sus dos manos sueltas. |
| `python tools/mutango/generar_sol_luna.py` | El Sol y la Luna (las caras estan en `caras_sol_luna.py`; las referencias en `fuentes/sol.jpg` y `fuentes/luna.jpg`). |
| `python tools/mutango/generar_bloque_ruinas.py` | El cubo del santuario (con `fuentes/TEXTURA.png`). |
| `python tools/mutango/generar_armaduras_mutango.py` | Las 16 versiones con armadura (cobre, hierro, diamante, netherite) de los tres Mutangos y de Neguechen. Va **despues** de los dos primeros. |

`modelo_base.py` es lo comun (colores, uuid estables, la clase `Model` que arma la textura y escribe el `.bbmodel`). Los
uuid salen del prefijo `donpollos/` a proposito: de ahi vienen los que ya tienen los modelos; si se cambia, cambian todos.

La armadura de Neguechen incluye un arreglo (`AJUSTES_NEGUECHEN` en `generar_armaduras_mutango.py`): el cuello, el cuello
del vestido y el dibujo del vestido se corren un poco para atras, y la pechera de abajo se achica en profundidad, para
que las caras del cuerpo y de la armadura no queden en el mismo plano y se pisen.

## Bedrock y las ruinas

| Comando | Que hace |
|---|---|
| `python tools/mutango/generar_bedrock_mutango.py` | Convierte los `.bbmodel` de Mutango al formato de GeyserModelEngine y escribe `src/main/resources/mutango/bedrock/geysermodelengine.zip` (solo los de Mutango: la carpeta de modelos es la de todo BigC). |
| `python tools/mutango/convertir_litematic.py` | Convierte `fuentes/Ruined World Spawn.litematic` a `src/main/resources/mutango/structures/ruinas.json.gz`. |

## Cuando cambies un modelo

1. Cambialo en su generador (o en Blockbench, si es un arreglo a mano: ojo, el proximo `generar_*` lo pisa).
2. Si es un Mutango o Neguechen: `python tools/mutango/generar_armaduras_mutango.py`.
3. `python tools/mutango/generar_bedrock_mutango.py`.
4. `gradlew build` (los tests `MutangoModelsTest` y `MutangoBedrockTest` avisan si algo quedo viejo).

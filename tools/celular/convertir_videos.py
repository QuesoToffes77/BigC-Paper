"""Convierte los videos de tools/celular/videos/ en texturas animadas para la pantalla del celular.

Uso:  python tools/celular/convertir_videos.py   (necesita ffmpeg en el PATH y haber corrido generar_celular.py)

Los videos originales no se suben al repo (tools/celular/videos/ esta en .gitignore).

Por cada video escribe en resourcepack/java/assets/celular/:
  textures/item/video_<n>.png (+ .mcmeta)   todos los cuadros de 72x128 apilados, a 10 por segundo
  models/item/celular_video_<n>.json         el celu con el video en la pantalla
  items/video_<n>.json                       en la mano muestra el video; en el inventario, el celu quieto
y la lista de videos en src/main/resources/celular/videos.yml (la lee el modulo).

Toma todos los videos de la carpeta, ordenados por cuando se agregaron (los nuevos van al final).

Genera todo primero en una carpeta temporal y lo valida; recien al final reemplaza los archivos del pack. Si falta
ffmpeg, si un video esta roto o si algo falla a mitad de camino, los archivos que ya estaban quedan como estaban.

Para probar con otra carpeta de videos: python tools/celular/convertir_videos.py --videos <carpeta>
"""

import argparse
import json
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import generar_celular  # noqa: E402

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
VIDEOS = HERE / "videos"
ASSETS = ROOT / "resourcepack" / "java" / "assets" / "celular"
LISTA = ROOT / "src" / "main" / "resources" / "celular" / "videos.yml"

ANCHO, ALTO = 72, 128     # 9:16, el lado largo topeado en 128
FPS = 10                  # 1 cuadro cada 2 ticks de Minecraft
MAX_SEGUNDOS = 60         # los videos mas largos se cortan: cada cuadro ocupa memoria en el cliente
NIVELES = 32              # niveles por color: lo deja un poquito mas pixelado (y el PNG pesa menos)

EXTENSIONES = {".mp4", ".webm", ".mkv", ".mov"}

# Pantalla del celu (ver generar_celular.py): x 5..11, y 1..15, frente en z = 8.75.
PANTALLA_X = (5.0, 11.0)
PANTALLA_Y = (1.0, 15.0)
VIDEO_ALTO = (PANTALLA_X[1] - PANTALLA_X[0]) * ALTO / ANCHO
VIDEO_Y = (8.0 - VIDEO_ALTO / 2, 8.0 + VIDEO_ALTO / 2)
NEGRO = [0.3, 0.3, 0.45, 0.45]  # un pixel del borde negro del frente, en el atlas del celu

MANOS = ["firstperson_righthand", "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand"]


def videos(carpeta):
    """Todos los videos de la carpeta, del mas viejo al mas nuevo."""
    if not carpeta.is_dir():
        raise SystemExit(f"No existe la carpeta de videos {carpeta}")
    lista = [v for v in carpeta.iterdir() if v.suffix.lower() in EXTENSIONES]
    if not lista:
        raise SystemExit(f"No hay videos en {carpeta}")
    return sorted(lista, key=lambda v: (v.stat().st_mtime, v.name))


def titulo(video):
    """El nombre del archivo sin el [id] de YouTube, los #hashtags ni los emojis (la fuente de Minecraft no los tiene)."""
    texto = re.sub(r"\[[^\]]*\]", "", video.stem)
    texto = re.sub(r"#\S+", "", texto)
    texto = "".join(c for c in texto if ord(c) < 0x2000 or c in "’¿¡")
    texto = re.sub(r"\s+", " ", texto.replace("＂", "").replace("？", "?")).strip(" -|")
    texto = texto.replace("’", "'").replace('"', "'")
    return (texto[:37].rstrip() + "...") if len(texto) > 40 else texto


def cuadros(video):
    """Todos los cuadros del video, ya achicados a 72x128, como un array (n, alto, ancho, 3)."""
    # si no es 9:16 (por ejemplo 3:4) entra entero con barras negras en vez de recortarse
    filtro = (f"fps={FPS},scale={ANCHO}:{ALTO}:force_original_aspect_ratio=decrease:flags=area,"
              f"pad={ANCHO}:{ALTO}:(ow-iw)/2:(oh-ih)/2:black")
    proceso = subprocess.run(
        ["ffmpeg", "-v", "error", "-t", str(MAX_SEGUNDOS), "-i", str(video), "-vf", filtro,
         "-f", "rawvideo", "-pix_fmt", "rgb24", "-"],
        capture_output=True)
    crudo = proceso.stdout
    tamano = ALTO * ANCHO * 3
    if proceso.returncode != 0 or len(crudo) < tamano or len(crudo) % tamano != 0:
        error = proceso.stderr.decode("utf-8", "replace").strip().splitlines()
        motivo = error[-1] if error else "ffmpeg no devolvio cuadros"
        raise SystemExit(f"No se pudo convertir {video.name}: {motivo}. No se toco nada del pack.")
    frames = np.frombuffer(crudo, np.uint8).reshape(-1, ALTO, ANCHO, 3)
    paso = 256 // NIVELES
    return (frames // paso * paso + paso // 2).astype(np.uint8)


def modelo_video(n):
    modelo = generar_celular.modelo()
    modelo["textures"]["1"] = f"celular:item/video_{n}"
    modelo["elements"] += [
        {"name": "pantalla_negra", "from": [PANTALLA_X[0], PANTALLA_Y[0], 8.8], "to": [PANTALLA_X[1], PANTALLA_Y[1], 8.8],
         "shade": False, "light_emission": 15,
         "faces": {"south": {"uv": NEGRO, "texture": "#0"}}},
        {"name": "video", "from": [PANTALLA_X[0], round(VIDEO_Y[0], 3), 8.85],
         "to": [PANTALLA_X[1], round(VIDEO_Y[1], 3), 8.85],
         "shade": False, "light_emission": 15,
         "faces": {"south": {"uv": [0, 0, 16, 16], "texture": "#1"}}},
    ]
    return modelo


def definicion_item(n):
    # hand_animation_on_swap: al cambiar de video el celu no baja y sube en la mano
    return {"hand_animation_on_swap": False, "model": {
        "type": "minecraft:select",
        "property": "minecraft:display_context",
        "cases": [{"when": MANOS, "model": {"type": "minecraft:model", "model": f"celular:item/celular_video_{n}"}}],
        "fallback": {"type": "minecraft:model", "model": "celular:item/celular"},
    }}


def generados(carpeta):
    """Los archivos de videos que hay en una carpeta con la estructura del pack (texturas, modelos e items)."""
    return (sorted((carpeta / "textures" / "item").glob("video_*"))
            + sorted((carpeta / "items").glob("video_*"))
            + sorted((carpeta / "models" / "item").glob("celular_video_*")))


def escribir_json(path, contenido):
    path.write_text(json.dumps(contenido, indent=2), encoding="utf-8", newline="\n")


def convertir(todos, destino):
    """Genera todo en destino, con la misma estructura que el pack. Devuelve el texto de videos.yml y las muestras."""
    for sub in (("textures", "item"), ("models", "item"), ("items",)):
        destino.joinpath(*sub).mkdir(parents=True, exist_ok=True)
    lista = ["# Generado por tools/celular/convertir_videos.py. Orden en el que se ven en el celu.", "videos:"]
    muestras = []
    for n, video in enumerate(todos, start=1):
        nombre = titulo(video)
        frames = cuadros(video)
        textura = destino / "textures" / "item" / f"video_{n}.png"
        Image.fromarray(frames.reshape(-1, ANCHO, 3), "RGB").save(textura, optimize=True)
        escribir_json(textura.with_name(textura.name + ".mcmeta"),
                      {"animation": {"frametime": 20 // FPS, "width": ANCHO, "height": ALTO, "interpolate": False}})
        escribir_json(destino / "models" / "item" / f"celular_video_{n}.json", modelo_video(n))
        escribir_json(destino / "items" / f"video_{n}.json", definicion_item(n))
        lista += [f"  - id: video_{n}", f"    titulo: \"{nombre}\"", f"    cuadros: {len(frames)}"]
        muestras.append(Image.fromarray(frames[len(frames) // 3]))
        peso = textura.stat().st_size / 1e6
        print(f"video_{n}: {nombre:<32} {len(frames):4d} cuadros ({len(frames) / FPS:.1f}s)  {peso:.1f} MB")
    return "\n".join(lista) + "\n", muestras


def validar(destino, cantidad, lista):
    """Que esten los 4 archivos de cada video, que el JSON se lea y que cada tira tenga los cuadros de videos.yml."""
    cuadros_por_video = [int(x) for x in re.findall(r"cuadros: (\d+)", lista)]
    if len(cuadros_por_video) != cantidad:
        raise SystemExit("videos.yml no tiene un video por cada archivo convertido. No se toco nada del pack.")
    for n in range(1, cantidad + 1):
        textura = destino / "textures" / "item" / f"video_{n}.png"
        with Image.open(textura) as img:
            if img.size != (ANCHO, ALTO * cuadros_por_video[n - 1]):
                raise SystemExit(f"{textura.name} salio con un tamano raro {img.size}. No se toco nada del pack.")
        for archivo in (textura.with_name(textura.name + ".mcmeta"),
                        destino / "models" / "item" / f"celular_video_{n}.json",
                        destino / "items" / f"video_{n}.json"):
            json.loads(archivo.read_text(encoding="utf-8"))
    if len(generados(destino)) != 4 * cantidad:
        raise SystemExit("Sobran o faltan archivos generados. No se toco nada del pack.")


def reemplazar(destino, lista):
    """Cambia los videos viejos del pack por los nuevos. Si algo falla en el medio, vuelve a poner los viejos."""
    lista_vieja = LISTA.read_bytes() if LISTA.exists() else None
    with tempfile.TemporaryDirectory(dir=HERE, prefix=".respaldo_") as carpeta:
        respaldo = Path(carpeta)
        movidos = []
        try:
            for viejo in generados(ASSETS):
                guardado = respaldo / viejo.relative_to(ASSETS)
                guardado.parent.mkdir(parents=True, exist_ok=True)
                shutil.move(viejo, guardado)
                movidos.append((guardado, viejo))
            for nuevo in generados(destino):
                final = ASSETS / nuevo.relative_to(destino)
                final.parent.mkdir(parents=True, exist_ok=True)
                shutil.move(nuevo, final)
            LISTA.write_text(lista, encoding="utf-8", newline="\n")
        except BaseException:
            for nuevo in generados(ASSETS):
                nuevo.unlink()
            for guardado, original in movidos:
                shutil.move(guardado, original)
            if lista_vieja is not None:
                LISTA.write_bytes(lista_vieja)
            raise


def vista_previa(muestras):
    """Un cuadro de cada video en la pantalla del celu."""
    atlas = Image.open(ASSETS / "textures" / "item" / "celular.png").convert("RGBA")
    filas = (len(muestras) + 4) // 5
    hoja = Image.new("RGBA", (5 * 240, 380 * filas), (54, 57, 63, 255))
    for i, muestra in enumerate(muestras):
        tex = atlas.copy()
        # pinto el cuadro sobre la zona de la pantalla del atlas solo para la vista previa
        pantalla = Image.new("RGBA", (24, 56), (10, 10, 14, 255))
        alto = round(24 * ALTO / ANCHO)
        pantalla.paste(muestra.resize((24, alto), Image.NEAREST).convert("RGBA"), (0, (56 - alto) // 2))
        tex.paste(pantalla, (2, 2))
        vista = generar_celular.render(generar_celular.modelo(), tex, -15, 8, lado=360, zoom=20)
        vista = vista.crop((60, 0, 300, 380 - 10))
        hoja.paste(vista, ((i % 5) * 240, 5 + (i // 5) * 380), vista)
    hoja.save(HERE / "vista_previa_videos.png")


def main():
    parser = argparse.ArgumentParser(description="Convierte los videos del celular en texturas animadas.")
    parser.add_argument("--videos", type=Path, default=VIDEOS,
                        help="carpeta con los videos (por defecto tools/celular/videos)")
    args = parser.parse_args()
    if shutil.which("ffmpeg") is None:
        raise SystemExit("Se necesita ffmpeg en el PATH para convertir los videos. No se toco nada del pack.")
    todos = videos(args.videos)
    with tempfile.TemporaryDirectory(dir=HERE, prefix=".conversion_") as carpeta:
        temporal = Path(carpeta)
        lista, muestras = convertir(todos, temporal)
        validar(temporal, len(todos), lista)
        reemplazar(temporal, lista)
    vista_previa(muestras)
    print(f"Listo: {len(todos)} videos. Despues corre tools/celular/generar_bedrock.py para los items de Bedrock.")


if __name__ == "__main__":
    main()

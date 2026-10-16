"""Genera el cubo de las ruinas del Mutango: un bloque (16x16x16) con la textura de
tools/mutango/fuentes/TEXTURA.png en las seis caras. Tapa el agujero que despierta a las estatuas.

Uso: python tools/mutango/generar_bloque_ruinas.py   (deja bigcasares_ruinas_bloque.bbmodel en src/main/resources/bettermodel/models/)
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from modelo_base import Model  # noqa: E402

NAME = "bigcasares_ruinas_bloque"
OUT_DIR = Path(__file__).resolve().parents[2] / "src" / "main" / "resources" / "bettermodel" / "models"
TEXTURE = Path(__file__).resolve().parent / "fuentes" / "TEXTURA.png"


def build():
    texture = Image.open(TEXTURE).convert("RGBA")
    m = Model(NAME, {"root": ([0, 0, 0], None), "bloque": ([0, 8, 0], "root")}, density=1, tex=64)
    m.out_dir = OUT_DIR
    m.cube("bloque", "bloque", [-8, 0, -8], [8, 16, 8], "bloque")

    def paint(el, face, w, h, seed):
        return texture.resize((w, h), Image.NEAREST)

    m.build(paint, {"idle": (1.0, "linear", {
        "bloque": [("rotation", 0, (0, 0, 0)), ("rotation", 1.0, (0, 0, 0))]})})


if __name__ == "__main__":
    build()

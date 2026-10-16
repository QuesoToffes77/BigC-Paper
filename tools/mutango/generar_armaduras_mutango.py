"""Versiones con armadura de Lef, Lihuen, Lican y Neguechen (como generar_armaduras.py de Don Pollos en BigC).

Cada vez que le ganas a la pelea del Mutango, la siguiente sale con mejor armadura: cobre, hierro, diamante y
netherite. BetterModel esconde la armadura de la vaca de abajo, asi que la armadura tiene que estar en el modelo. Este
script toma cada modelo y arma una version por material:
  Mutangos:  casco (la parte de arriba del cuerpo, sin tapar el ojo), brazales, grebas y botas.
  Neguechen: hombreras y pechera (la parte de arriba de la tunica).
Cada pieza es una caja un poco mas grande que lo que cubre y va en el mismo hueso, asi se mueve con las animaciones.
Son modelos aparte (y no partes que se prenden y apagan) para que GeyserModelEngine los muestre igual en Bedrock.

La textura sigue la grilla de pixeles del modelo: cada pixel de la armadura es un cuadrado de DENSIDAD x DENSIDAD
de la textura (3 en los Mutangos, 1 en Neguechen), igual que la piedra. La textura se agranda al doble de alto y las
placas van en la mitad nueva; las coordenadas de la textura vieja no cambian.

Uso: python tools/mutango/generar_armaduras_mutango.py   (despues de generar_mutango.py / generar_neguechen.py)
Salida: src/main/resources/bettermodel/models/<modelo>_<material>.bbmodel. Despues: python tools/mutango/generar_bedrock_mutango.py
"""
import base64
import copy
import io
import json
import math
import uuid
from pathlib import Path

from PIL import Image

MUTANGO = Path(__file__).resolve().parents[2] / "src" / "main" / "resources" / "bettermodel" / "models"

TIERS = {
    # material: (base, luz, sombra) -- los mismos colores que Don Pollos
    "cobre": ((191, 104, 64), (232, 146, 100), (128, 62, 36)),
    "hierro": ((198, 198, 198), (236, 236, 236), (128, 128, 128)),
    "diamante": ((74, 214, 216), (164, 250, 244), (32, 136, 150)),
    "netherite": ((70, 60, 64), (104, 94, 98), (36, 31, 34)),
}

MUTANGO_PIECES = [("cuerpo", "casco"), ("brazo_der", "brazal"), ("brazo_izq", "brazal"),
                  ("pierna_der", "greba"), ("pierna_izq", "greba"), ("pie_der", "bota"), ("pie_izq", "bota")]
PIECES = {
    "bigcasares_boss_mutango": MUTANGO_PIECES,
    "bigcasares_boss_mutango_2ojos": MUTANGO_PIECES,
    "bigcasares_boss_mutango_3ojos": MUTANGO_PIECES,
    "bigcasares_neguechen": [("hombros", "hombrera"), ("tunica_7", "pechera"), ("tunica_6", "pechera")],
}
# cuanto mas grande que lo que cubre (en pixeles del modelo, para que quede en la grilla)
GROW = 0.5
# el casco tapa solo los 4 pixeles de arriba del cuerpo (el ojo queda a la vista); el brazal, los 8 de abajo del brazo
CASCO_ALTO = 4
BRAZAL_ALTO = 8
NAMESPACE = uuid.UUID("8a3f2c71-4d6e-4b1a-9c0d-6e5f4a3b2c1d")

# Neguechen: la armadura y el cuerpo no pueden tener caras en el mismo plano (se pisan las texturas y se ve raro).
# Se corre en Z lo que sobresale del cuerpo para que quede siempre atras de la armadura: elemento -> (Z de adelante,
# Z de atras); None = no cambia. El UV no se toca (el tamano de la placa en la textura es el de antes).
AJUSTES_NEGUECHEN = {
    "collar": (-6.3, None),                     # el cuello del vestido sobresalia por delante de la pechera
    "cuello": (-7.3, None),                     # estaba justo en el plano de adelante de la hombrera
    "patron": (-6.3, None),                     # el dibujo del vestido sobresalia por delante de la pechera
    "armadura_pechera_tunica_6": (-6.4, 6.4),   # tenia las caras en el mismo plano que la otra pechera
}


def shell(element, piece):
    """La caja de la pieza: un poco mas grande que el elemento que cubre."""
    (x1, y1, z1), (x2, y2, z2) = element["from"], element["to"]
    if piece == "casco":
        y1 = y2 - CASCO_ALTO
    elif piece == "brazal":
        y2 = y1 + BRAZAL_ALTO
    return [x1 - GROW, y1 - GROW, z1 - GROW], [x2 + GROW, y2 + GROW, z2 + GROW]


def face_size(frm, to, face):
    """Ancho y alto (en pixeles del modelo) de una cara de la caja."""
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    if face in ("north", "south"):
        return dx, dy
    if face in ("east", "west"):
        return dz, dy
    return dx, dz


def plate(cells_w, cells_h, density, colors, seed):
    """Una placa de armadura en la grilla: borde oscuro, brillo arriba y a la izquierda, remaches en las esquinas."""
    base, light, dark = colors
    img = Image.new("RGBA", (cells_w * density, cells_h * density), base + (255,))
    for cx in range(cells_w):
        for cy in range(cells_h):
            edge = cx in (0, cells_w - 1) or cy in (0, cells_h - 1)
            if edge:
                color = dark
            elif cy == 1 or cx == 1:
                color = light
            else:
                # un poco de variacion, siempre en celdas enteras
                h = (cx * 73 + cy * 151 + seed * 31) % 7
                color = tuple(max(0, c - 14) for c in base) if h == 0 else base
            if (cx, cy) in ((1, 1), (cells_w - 2, 1), (1, cells_h - 2), (cells_w - 2, cells_h - 2)) \
                    and cells_w >= 5 and cells_h >= 5:
                color = light
            for px in range(density):
                for py in range(density):
                    img.putpixel((cx * density + px, cy * density + py), color + (255,))
    return img


class Packer:
    """Acomoda rectangulos en filas, de izquierda a derecha, en la mitad nueva de la textura."""

    def __init__(self, width, top):
        self.width = width
        self.x = 0
        self.y = top
        self.row = 0

    def place(self, w, h):
        if self.x + w > self.width:
            self.x = 0
            self.y += self.row
            self.row = 0
        at = (self.x, self.y)
        self.x += w
        self.row = max(self.row, h)
        return at


def density_of(model):
    """Cuantos texeles de la textura por pixel del modelo (mirando la cara de adelante de la primer pieza)."""
    for element in model["elements"]:
        face = element["faces"].get("north")
        width = element["to"][0] - element["from"][0]
        if face and width >= 4:
            return round((face["uv"][2] - face["uv"][0]) / width)
    return 1


def build(base_key, tier):
    data = json.loads((MUTANGO / f"{base_key}.bbmodel").read_text(encoding="utf-8"))
    model = copy.deepcopy(data)
    key = f"{base_key}_{tier}"
    model["name"] = key
    if "model_identifier" in model:
        model["model_identifier"] = key
    density = density_of(data)

    texture = model["textures"][0]
    old = Image.open(io.BytesIO(base64.b64decode(texture["source"].split(",", 1)[1]))).convert("RGBA")
    width, height = old.size
    canvas = Image.new("RGBA", (width, height * 2), (0, 0, 0, 0))
    canvas.paste(old, (0, 0))
    packer = Packer(width, height)

    by_name = {e["name"]: e for e in model["elements"]}
    added = {}
    for index, (name, piece) in enumerate(PIECES[base_key]):
        element = by_name[name]
        armor = copy.deepcopy(element)
        armor["name"] = f"armadura_{piece}_{name}"
        armor["uuid"] = str(uuid.uuid5(NAMESPACE, key + "/" + armor["name"]))
        armor["from"], armor["to"] = shell(element, piece)
        armor["inflate"] = 0
        faces = {}
        for face in element["faces"]:
            fw, fh = face_size(armor["from"], armor["to"], face)
            cells_w, cells_h = max(1, math.ceil(fw - 1e-6)), max(1, math.ceil(fh - 1e-6))
            img = plate(cells_w, cells_h, density, TIERS[tier], index * 6 + len(faces))
            x, y = packer.place(img.width, img.height)
            canvas.paste(img, (x, y))
            faces[face] = {"uv": [x, y, x + fw * density, y + fh * density], "texture": 0}
        armor["faces"] = faces
        model["elements"].append(armor)
        added[element["uuid"]] = armor["uuid"]
    if base_key == "bigcasares_neguechen":
        for element in model["elements"]:
            ajuste = AJUSTES_NEGUECHEN.get(element["name"])
            if ajuste:
                if ajuste[0] is not None:
                    element["from"][2] = ajuste[0]
                if ajuste[1] is not None:
                    element["to"][2] = ajuste[1]
    if packer.y + packer.row > height * 2:
        raise SystemExit(f"{key}: las placas no entran en la textura")

    out = io.BytesIO()
    canvas.save(out, format="PNG", optimize=False)
    texture["source"] = "data:image/png;base64," + base64.b64encode(out.getvalue()).decode("ascii")
    texture["height"] = height * 2
    texture["uv_height"] = texture.get("uv_height", height) * 2
    texture["name"] = key + ".png"
    texture["relative_path"] = key + ".png"
    texture["uuid"] = str(uuid.uuid5(NAMESPACE, key + "/texture"))
    model["resolution"]["height"] = model["resolution"]["height"] * 2

    def place(children):
        for child in list(children):
            if isinstance(child, dict):
                place(child["children"])
            elif child in added:
                children.insert(children.index(child) + 1, added[child])

    place(model["outliner"])
    out_path = MUTANGO / f"{key}.bbmodel"
    out_path.write_text(json.dumps(model, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    return out_path, len(added)


def main():
    for base_key in PIECES:
        for tier in TIERS:
            path, pieces = build(base_key, tier)
            print("ok", path.name, pieces, "piezas")


if __name__ == "__main__":
    main()

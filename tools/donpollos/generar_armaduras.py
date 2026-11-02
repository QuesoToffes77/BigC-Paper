"""Versiones con armadura de los bichos del Don Pollo Boss (gallina WhatsApp y Labubus).

Cada vez que le ganas al boss, sus bichos salen con mejor armadura: cobre, hierro, diamante y netherite. En Java
BetterModel esconde la armadura del zombie (se ve el modelo), asi que la armadura tiene que estar en el modelo. Este
script toma cada modelo y arma una version por material, con un casco que tapa la parte de arriba de la cabeza (sin
tapar la cara), una pechera sobre el cuerpo y, en los Labubus, botas. Cada pieza va en el mismo hueso que lo que
cubre, asi se mueve con las animaciones. Son modelos aparte (y no partes que se prenden y apagan) para que
GeyserModelEngine los muestre igual en Bedrock.

La textura se agranda al doble de alto y la mitad nueva lleva el dibujo de cada material; las coordenadas de la
textura vieja no cambian.

Uso: python tools/generar_armaduras.py
Salida: <modelo>_<material>.bbmodel junto a los modelos (ver OUT_MODELS de generar_modelos.py).
"""
import base64
import copy
import io
import json
import sys
import uuid
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import generar_modelos as gm  # noqa: E402

TIERS = {
    # material: (base, luz, sombra)
    "cobre": ((191, 104, 64), (232, 146, 100), (128, 62, 36)),
    "hierro": ((198, 198, 198), (236, 236, 236), (128, 128, 128)),
    "diamante": ((74, 214, 216), (164, 250, 244), (32, 136, 150)),
    "netherite": ((70, 60, 64), (104, 94, 98), (36, 31, 34)),
}

# Que piezas lleva cada modelo: (elemento que cubre, pieza). "casco" tapa solo la parte de arriba de la cabeza.
PIECES = {
    "bigcasares_gallina_wsp": [("cabeza", "casco"), ("cuerpo", "pechera")],
    "bigcasares_labubu_verde": [("cabeza", "casco"), ("bata_arriba", "pechera"), ("bata_abajo", "pechera"),
                                ("pierna_der", "bota"), ("pierna_izq", "bota")],
}
for color in ("rojo", "rosa", "azul"):
    PIECES["bigcasares_labubu_" + color] = PIECES["bigcasares_labubu_verde"]

TILE = 32  # lado del dibujo de cada material en la textura (pixeles)
NAMESPACE = uuid.UUID("5f0d1c3a-6b2e-4c8e-9a51-0d0a11a2b3c4")


def tile(base, light, dark):
    """Placa de armadura: borde oscuro, brillo arriba a la izquierda y remaches."""
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    px = img.load()
    for i in range(TILE):
        for j in range(TILE):
            if i in (0, TILE - 1) or j in (0, TILE - 1):
                px[i, j] = dark + (255,)
            elif i < 4 or j < 4:
                px[i, j] = light + (255,)
            elif (i // 8 + j // 8) % 2 == 0 and (i % 8 == 0 or j % 8 == 0):
                px[i, j] = dark + (255,)
    for i, j in ((6, 6), (TILE - 7, 6), (6, TILE - 7), (TILE - 7, TILE - 7)):
        px[i, j] = light + (255,)
    return img


# Cuanto de la cabeza tapa el casco (desde arriba): a la gallina los ojos le quedan muy arriba, asi que es una placa.
HELMET_DEPTH = {"gallina": 0.1, "labubu": 0.35}


def shell(element, piece, grow, helmet_depth):
    """La pieza de armadura: una caja un poco mas grande que el elemento que cubre."""
    (x1, y1, z1), (x2, y2, z2) = element["from"], element["to"]
    if piece == "casco":
        # solo la parte de arriba de la cabeza: la cara (ojos, boca) queda a la vista
        y1 = y2 - (y2 - y1) * helmet_depth
    elif piece == "bota":
        y2 = y1 + (y2 - y1) * 0.55
    return [x1 - grow, y1 - grow, z1 - grow], [x2 + grow, y2 + grow, z2 + grow]


def build(base_key, tier):
    path = gm.OUT_MODELS / f"{base_key}.bbmodel"
    data = json.loads(path.read_text(encoding="utf-8"))
    model = copy.deepcopy(data)
    key = f"{base_key}_{tier}"
    model["name"] = key
    if "model_identifier" in model:
        model["model_identifier"] = key

    texture = model["textures"][0]
    raw = base64.b64decode(texture["source"].split(",", 1)[1])
    old = Image.open(io.BytesIO(raw)).convert("RGBA")
    width, height = old.size
    canvas = Image.new("RGBA", (width, height * 2), (0, 0, 0, 0))
    canvas.paste(old, (0, 0))
    canvas.paste(tile(*TIERS[tier]), (0, height))
    out = io.BytesIO()
    canvas.save(out, format="PNG", optimize=False)
    texture["source"] = "data:image/png;base64," + base64.b64encode(out.getvalue()).decode("ascii")
    texture["height"] = height * 2
    texture["uv_height"] = texture.get("uv_height", height) * 2
    texture["name"] = key + ".png"
    texture["relative_path"] = key + ".png"
    texture["uuid"] = str(uuid.uuid5(NAMESPACE, key + "/texture"))
    model["resolution"]["height"] = model["resolution"]["height"] * 2

    # grow: 0.25 unidades en los Labubus (chicos), 0.5 en la gallina
    grow = 0.5 if "gallina" in base_key else 0.25
    by_name = {e["name"]: e for e in model["elements"]}
    uv = [0, height, TILE, height + TILE]
    added = {}
    for name, piece in PIECES[base_key]:
        element = by_name[name]
        armor = copy.deepcopy(element)
        armor["name"] = f"armadura_{piece}_{name}"
        armor["uuid"] = str(uuid.uuid5(NAMESPACE, key + "/" + armor["name"]))
        depth = HELMET_DEPTH["gallina" if "gallina" in base_key else "labubu"]
        armor["from"], armor["to"] = shell(element, piece, grow, depth)
        armor["inflate"] = 0
        armor["faces"] = {face: {"uv": list(uv), "texture": 0} for face in element["faces"]}
        model["elements"].append(armor)
        added[element["uuid"]] = armor["uuid"]

    def place(children):
        for child in list(children):
            if isinstance(child, dict):
                place(child["children"])
            elif child in added:
                children.insert(children.index(child) + 1, added[child])

    place(model["outliner"])
    out_path = gm.OUT_MODELS / f"{key}.bbmodel"
    out_path.write_text(json.dumps(model, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    return out_path


def main():
    for base_key in PIECES:
        for tier in TIERS:
            print("ok", build(base_key, tier).name)


if __name__ == "__main__":
    main()

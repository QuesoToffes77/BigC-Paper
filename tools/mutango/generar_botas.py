"""Genera el modelo de las Botas de Neguechen: botas de vaquero de piedra con un poco de musgo, con una espuela roja atras
cuya rueda es el Sol (amarillo) en una bota y la Luna (blanca) en la otra. Escribe el modelo 3D del item (JSON de
Minecraft con elementos), su textura y el modelo puesto (equipment asset + textura de 64x32 que se dibuja sobre las
piernas).

Uso: python tools/mutango/generar_botas.py [vista_previa.png]
   Escribe en resourcepack/java/assets/mutango/ y, si se pide, una vista previa del item y de las botas puestas.
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

REPO = Path(__file__).resolve().parents[2]
ASSETS = REPO / "resourcepack" / "java" / "assets" / "mutango"

STONES = [(122, 122, 122, 255), (133, 133, 133, 255), (110, 110, 110, 255), (145, 145, 145, 255),
          (128, 128, 128, 255)]
MOSS = [(84, 120, 48, 255), (106, 150, 60, 255), (72, 104, 40, 255)]
DARK = (78, 78, 82, 255)
BLACK = (40, 40, 44, 255)
RED = (172, 50, 50, 255)
RED_DARK = (128, 36, 36, 255)
SUN = [(251, 229, 54, 255), (251, 242, 54, 255)]
MOON = [(255, 255, 255, 255), (225, 225, 235, 255)]

# ---------------------------------------------------------------- textura del modelo del item (16x16)
# regiones (x1, y1, x2, y2) en pixeles; en el JSON el uv de una cara apunta a una de ellas
REGIONS = {
    "piedra": (0, 0, 6, 6),
    "musgo": (6, 0, 12, 6),
    "oscura": (12, 0, 14, 2),
    "negro": (14, 0, 16, 2),
    "rojo": (0, 6, 2, 8),
    "rojo_oscuro": (2, 6, 4, 8),
    "sol": (4, 6, 8, 10),
    "luna": (8, 6, 12, 10),
    "amarillo": (12, 6, 14, 8),
    "blanco": (14, 6, 16, 8),
}


def stone(x, y):
    return STONES[(x * 7 + y * 13 + (x * y) % 3) % len(STONES)]


def mossy(x, y):
    h = (x * 31 + y * 17 + (x ^ y) * 5) % 10
    return MOSS[h % len(MOSS)] if h < 4 else stone(x, y)


def item_texture():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()

    def fill(key, color):
        x1, y1, x2, y2 = REGIONS[key]
        for y in range(y1, y2):
            for x in range(x1, x2):
                px[x, y] = color(x, y) if callable(color) else color

    fill("piedra", stone)
    fill("musgo", mossy)
    fill("oscura", DARK)
    fill("negro", BLACK)
    fill("rojo", RED)
    fill("rojo_oscuro", RED_DARK)
    fill("amarillo", SUN[0])
    fill("blanco", MOON[0])
    for key, colors in (("sol", SUN), ("luna", MOON)):
        x1, y1, x2, y2 = REGIONS[key]
        for y in range(y1, y2):
            for x in range(x1, x2):
                px[x, y] = colors[(x + y) % 2]
    return im


def face(region):
    x1, y1, x2, y2 = REGIONS[region]
    return {"uv": [x1, y1, x2, y2], "texture": "#0"}


def box(a, b, faces, name, rotation=None):
    element = {"name": name, "from": list(a), "to": list(b), "faces": {k: face(v) for k, v in faces.items()}}
    if rotation:
        element["rotation"] = rotation
    return element


DIRS = ("north", "south", "east", "west", "up", "down")


def uniform(region):
    return {d: region for d in DIRS}


def boot(x1, x2, wheel, outer, label):
    """Una bota de vaquero de piedra (la punta mira al norte, el talon y la espuela al sur): suela y taco, pie, una cana
    alta con el borde oscuro y una pestana de piedra, musgo de un costado y la espuela roja atras con su rueda (un
    Sol o una Luna con forma de estrella de ocho puntas)."""
    cx = (x1 + x2) / 2
    stone_faces = uniform("piedra")
    moss_faces = dict(stone_faces)
    moss_faces[outer] = "musgo"
    moss_faces["up"] = "musgo"
    elements = [
        box((x1, 0, 2), (x2, 1, 9), uniform("negro"), label + " suela"),
        box((x1, 0, 10), (x2, 2, 14), uniform("negro"), label + " taco"),
        box((x1, 1, 2), (x2, 3, 5.5), stone_faces, label + " punta"),
        box((x1, 1, 5.5), (x2, 5, 10), moss_faces, label + " pie"),
        box((x1, 2, 9), (x2, 12.5, 14), moss_faces, label + " cana"),
        box((x1 - 0.25, 11.5, 8.75), (x2 + 0.25, 12.75, 14.25), uniform("oscura"), label + " borde"),
        box((cx - 1.25, 12.75, 10), (cx + 1.25, 14, 11), uniform("oscura"), label + " pestana"),
        # la espuela: un tallo rojo que sale hacia atras del talon y, al final, la rueda
        box((cx - 0.75, 3.1, 14), (cx + 0.75, 4.1, 17.5), {**uniform("rojo"), "up": "rojo_oscuro"},
            label + " espuela roja"),
        box((cx - 1.9, 3.1, 14), (cx + 1.9, 4.1, 14.8), uniform("rojo_oscuro"), label + " correa roja"),
    ]
    flat = "sol" if wheel == "sol" else "luna"
    plain = "amarillo" if wheel == "sol" else "blanco"
    # la rueda: dos placas cuadradas (una girada 45 grados) vistas de costado = una estrella de ocho puntas
    for angle in (0, 45):
        faces = uniform(plain)
        faces["east"] = flat
        faces["west"] = flat
        rotation = {"origin": [cx, 3.6, 19], "axis": "x", "angle": angle} if angle else None
        elements.append(box((cx - 0.4, 2.1, 17.5), (cx + 0.4, 5.1, 20.5), faces,
                            label + " rueda " + wheel + (" girada" if angle else ""), rotation))
    return elements


def item_model():
    elements = boot(1, 6, "sol", "west", "izquierda") + boot(10, 15, "luna", "east", "derecha")
    return {
        "parent": "minecraft:block/block",
        "textures": {"0": "mutango:item/botas_neguechen_modelo", "particle": "mutango:item/botas_neguechen_modelo"},
        "elements": elements,
    }


# ---------------------------------------------------------------- botas puestas (equipment, 64x32)
# la pierna es una caja de 4 x 12 x 4 con la textura en (0, 16): arriba (4,16), abajo (8,16), lado de afuera (0,20),
# frente (4,20), lado de adentro (8,20) y atras (12,20). Las botas pintan la parte de abajo (una cana alta de vaquero).
LEG_Y = 16
BOOT_ROWS = 10  # alto de las botas en pixeles


def equipment_texture():
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()
    top = LEG_Y + 16 - BOOT_ROWS  # primera fila de las botas (el piso es la ultima, la 31)
    for x0 in (4, 12, 0, 8):  # frente, atras, afuera y adentro
        for y in range(top, LEG_Y + 16):
            for x in range(x0, x0 + 4):
                px[x, y] = mossy(x, y) if x0 == 0 else stone(x, y)
        for x in range(x0, x0 + 4):
            px[x, top] = DARK  # borde de arriba
            px[x, LEG_Y + 15] = BLACK  # suela
    # atras: el taco oscuro y la espuela roja con su rueda (el Sol y la Luna, mitad y mitad)
    for x in range(12, 16):
        px[x, LEG_Y + 14] = BLACK
    for dx in range(4):
        px[12 + dx, LEG_Y + 12] = RED if dx in (1, 2) else DARK
    for dy in range(2):
        px[12, LEG_Y + 12 - dy - 1 + 0] = px[12, LEG_Y + 12 - dy - 1]
    px[13, LEG_Y + 11] = SUN[0]
    px[13, LEG_Y + 13] = SUN[1]
    px[14, LEG_Y + 11] = MOON[0]
    px[14, LEG_Y + 13] = MOON[1]
    # costados: musgo por afuera, la pestana oscura asoma arriba
    for dy in range(3):
        for dx in range(2):
            px[1 + dx, top + 2 + dy] = MOSS[(dx + dy) % len(MOSS)]
    # arriba y abajo de la caja
    for x in range(4, 8):
        for y in range(LEG_Y, LEG_Y + 4):
            px[x, y] = DARK
    for x in range(8, 12):
        for y in range(LEG_Y, LEG_Y + 4):
            px[x, y] = BLACK
    return im


def equipment_asset():
    return {"layers": {"humanoid": [{"texture": "mutango:neguechen_boots"}]}}


# ---------------------------------------------------------------- vista previa (proyeccion ortogonal)
CORNERS = {
    "north": lambda x1, y1, z1, x2, y2, z2: [(x2, y2, z1), (x1, y2, z1), (x1, y1, z1), (x2, y1, z1)],
    "south": lambda x1, y1, z1, x2, y2, z2: [(x1, y2, z2), (x2, y2, z2), (x2, y1, z2), (x1, y1, z2)],
    "east": lambda x1, y1, z1, x2, y2, z2: [(x2, y2, z2), (x2, y2, z1), (x2, y1, z1), (x2, y1, z2)],
    "west": lambda x1, y1, z1, x2, y2, z2: [(x1, y2, z1), (x1, y2, z2), (x1, y1, z2), (x1, y1, z1)],
    "up": lambda x1, y1, z1, x2, y2, z2: [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)],
    "down": lambda x1, y1, z1, x2, y2, z2: [(x1, y1, z2), (x2, y1, z2), (x2, y1, z1), (x1, y1, z1)],
}
NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0),
           "down": (0, -1, 0)}
SHADING = {"up": 1.0, "north": 0.86, "south": 0.86, "east": 0.72, "west": 0.72, "down": 0.55}


def project(p, yaw, pitch):
    """Camara a 'yaw' grados (45 = noroeste mirando al sureste, como el icono de un bloque en el inventario)."""
    x, y, z = p
    fx, fz = math.sin(math.radians(yaw)), math.cos(math.radians(yaw))  # hacia donde mira (en x, z)
    rx, rz = -fz, fx  # a su derecha
    along = x * fx + z * fz
    sx = x * rx + z * rz
    sy = y * math.cos(math.radians(pitch)) + along * math.sin(math.radians(pitch))
    depth = along * math.cos(math.radians(pitch)) - y * math.sin(math.radians(pitch))
    return sx, sy, depth


def draw_faces(faces, size, scale, yaw=45, pitch=30, background=(40, 40, 46, 255)):
    """faces: (normal, esquinas 3D, uv en pixeles (u1, v1, u2, v2), textura, sombra). De atras para adelante."""
    projected = []
    view = (math.sin(math.radians(yaw)) * math.cos(math.radians(pitch)), -math.sin(math.radians(pitch)),
            math.cos(math.radians(yaw)) * math.cos(math.radians(pitch)))
    for n, corners, uv, texture, shade in faces:
        if n[0] * view[0] + n[1] * view[1] + n[2] * view[2] >= -1e-6:
            continue  # de espaldas
        pts = [project(c, yaw, pitch) for c in corners]
        projected.append((sum(p[2] for p in pts) / 4, pts, uv, texture, shade))
    xs = [p[0] for item in projected for p in item[1]]
    ys = [p[1] for item in projected for p in item[1]]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    canvas = Image.new("RGBA", (size, size), background)
    for _, pts, uv, texture, shade in sorted(projected, key=lambda item: -item[0]):
        dest = [((p[0] - cx) * scale + size / 2, size / 2 - (p[1] - cy) * scale) for p in pts]
        (x0, y0), (x1, y1), _, (x3, y3) = dest
        u1, v1, u2, v2 = uv
        det = (x1 - x0) * (y3 - y0) - (x3 - x0) * (y1 - y0)
        if abs(det) < 1e-6:
            continue
        # (x, y) = p0 + s * (p1 - p0) + t * (p3 - p0)  ->  (u, v) = (u1, v1) + s * (u2 - u1, 0) + t * (0, v2 - v1)
        inv = [[(y3 - y0) / det, -(x3 - x0) / det], [-(y1 - y0) / det, (x1 - x0) / det]]
        coeffs = (inv[0][0] * (u2 - u1), inv[0][1] * (u2 - u1), u1 - (inv[0][0] * x0 + inv[0][1] * y0) * (u2 - u1),
                  inv[1][0] * (v2 - v1), inv[1][1] * (v2 - v1), v1 - (inv[1][0] * x0 + inv[1][1] * y0) * (v2 - v1))
        layer = texture.transform((size, size), Image.AFFINE, coeffs, resample=Image.NEAREST)
        channels = layer.split()
        layer = Image.merge("RGBA", (*[c.point(lambda v: int(v * shade)) for c in channels[:3]], channels[3]))
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).polygon(dest, fill=255)
        layer.putalpha(Image.composite(channels[3], Image.new("L", (size, size), 0), mask))
        canvas.alpha_composite(layer)
    return canvas


def rotate_x(p, origin, angle):
    c, s = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    y, z = p[1] - origin[1], p[2] - origin[2]
    return (p[0], origin[1] + y * c - z * s, origin[2] + y * s + z * c)


def item_faces(model, texture):
    faces = []
    for element in model["elements"]:
        (x1, y1, z1), (x2, y2, z2) = element["from"], element["to"]
        rotation = element.get("rotation")
        for name, data in element["faces"].items():
            corners = CORNERS[name](x1, y1, z1, x2, y2, z2)
            normal = NORMALS[name]
            shade = SHADING[name]
            if rotation:
                corners = [rotate_x(c, rotation["origin"], rotation["angle"]) for c in corners]
                normal = rotate_x(normal, (0, 0, 0), rotation["angle"])
                shade = 0.85
            faces.append((normal, corners, data["uv"], texture, shade))
    return faces


def legs_faces(texture):
    """Dos piernas con las botas puestas (la de la derecha es la de la izquierda espejada)."""
    faces = []
    boxes = {  # cara -> uv de la pierna (u1, v1, u2, v2)
        "up": (4, 16, 8, 20), "down": (8, 16, 12, 20), "west": (0, 20, 4, 32), "north": (4, 20, 8, 32),
        "east": (8, 20, 12, 32), "south": (12, 20, 16, 32)}
    for x0, mirrored in ((0, False), (6, True)):
        for name in boxes:
            uv_name = name
            if mirrored and name in ("east", "west"):
                uv_name = "east" if name == "west" else "west"  # espejada: el lado de afuera queda del otro costado
            u1, v1, u2, v2 = boxes[uv_name]
            uv = (u2, v1, u1, v2) if mirrored else (u1, v1, u2, v2)
            corners = CORNERS[name](x0 - 0.5, -0.5, -0.5, x0 + 4.5, 12.5, 4.5)
            faces.append((NORMALS[name], corners, uv, texture, SHADING[name]))
    return faces


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(data, Image.Image):
        data.save(path)
    else:
        path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main():
    model = item_model()
    atlas = item_texture()
    worn = equipment_texture()
    write(ASSETS / "models" / "item" / "botas_neguechen.json", model)
    write(ASSETS / "textures" / "item" / "botas_neguechen_modelo.png", atlas)
    write(ASSETS / "equipment" / "neguechen_boots.json", equipment_asset())
    write(ASSETS / "textures" / "entity" / "equipment" / "humanoid" / "neguechen_boots.png", worn)
    print("ok botas: modelo con", len(model["elements"]), "elementos")
    if len(sys.argv) > 1:
        faces = item_faces(model, atlas)
        panels = [draw_faces(faces, 420, 17, yaw=45, pitch=28),  # de frente y de costado: las puntas con musgo
                  draw_faces(faces, 420, 17, yaw=215, pitch=28),  # por atras: las espuelas rojas
                  draw_faces(legs_faces(worn), 420, 22, yaw=200, pitch=18)]  # puestas, de espaldas
        out = Image.new("RGBA", (420 * len(panels), 420))
        for i, panel in enumerate(panels):
            out.paste(panel, (420 * i, 0))
        out.save(sys.argv[1])
        print("vista previa en", sys.argv[1])


if __name__ == "__main__":
    main()

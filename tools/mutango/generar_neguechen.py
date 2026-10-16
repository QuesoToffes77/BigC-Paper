"""Genera a Neguechen (tools/mutango/fuentes/neguechen*.{png,jpg}) como modelo de cuboides.

Forma (neguechen.png): un busto flotante que se ensancha hacia los hombros, sin piernas, cabeza octogonal y dos manos
sueltas flotando a los costados, abiertas con los dedos separados.
Aspecto femenino (neguechen3): cara de mujer, gorro redondo de gajos de colores con una flor azul adelante, vincha de
monedas de plata sobre la frente y trenzas largas de colores que caen por encima de los hombros.
Aspecto imponente (neguechen1 y 2): tunica roja profunda con guardas doradas, collar de plata en el pecho, un panel en
el medio con motivos geometricos mapuche (rombos escalonados) que brillan, ojos ambar que brillan y manos grandes que
brillan.

Todo con cuboides y textura de baja resolucion (1 texel por pixel de modelo). Lo que brilla va en huesos glow_
(etiqueta de BetterModel: brillo propio, sin sombra).

Uso: python tools/mutango/generar_neguechen.py   (deja bigcasares_neguechen.bbmodel en src/main/resources/bettermodel/models/)
"""
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import modelo_base as gm  # noqa: E402
from modelo_base import Model  # noqa: E402

NAME = "bigcasares_neguechen"
OUT_DIR = Path(__file__).resolve().parents[2] / "src" / "main" / "resources" / "bettermodel" / "models"

# colores
ROBE = (128, 26, 34)
ROBE_DARK = (98, 18, 26)
ROBE_LIGHT = (156, 38, 44)
GOLD = (222, 176, 62)
GOLD_DARK = (170, 120, 36)
NAVY = (26, 30, 70)
NAVY_DARK = (16, 18, 48)
GLOW = (255, 170, 48)
GLOW_HOT = (255, 226, 120)
SILVER = (200, 204, 214)
SILVER_DARK = (132, 136, 150)
SILVER_LIGHT = (240, 242, 248)
SKIN = (206, 148, 110)
SKIN_SHADE = (176, 118, 86)
SKIN_LIGHT = (226, 172, 132)
HAIR = (34, 26, 30)
HAIR_LIGHT = (58, 44, 48)
LIP = (160, 66, 66)
LIP_LIGHT = (188, 98, 92)
BROW = (70, 46, 40)
AMBER = (255, 160, 40)
AMBER_HOT = (255, 230, 140)
HAND = (226, 186, 150)
HAND_SHADE = (196, 152, 118)
RAINBOW = [(206, 40, 44), (236, 120, 36), (244, 206, 52), (64, 160, 72), (40, 160, 176), (44, 84, 176),
           (120, 60, 160)]
BRAIDS = [(206, 40, 44), (64, 160, 72), (44, 84, 176), (244, 206, 52), (236, 120, 36), (120, 60, 160)]

# manos como el boceto: palma poligonal y dedos cortos y anchos abiertos en abanico (x desde el centro de la palma)
HAND_FINGER_X = (-4.5, -1.5, 1.5, 4.5)
HAND_FINGER_LENGTH = (5.5, 6.5, 6.5, 5.5)
HAND_SPREAD = (-30, -10, 10, 30)

# cara: 16 columnas (x 8 a -8; la columna 0 es +X, la izquierda mirando de frente) y 18 filas (y 61 a 43)
FACE_X, FACE_TOP = 8, 61
FACE = [
    "HHHHHHHHHHHHHHHH",
    "HHHHHHHHHHHHHHHH",
    "HHHHHHHHHHHHHHHH",
    "HHHHHHHHHHHHHHHH",
    "HHhSSSSSSSSSShHH",
    "HhSSSSSSSSSSSShH",
    "HhSSSSLLLLSSSShH",
    "HhSSSSSLLSSSSShH",
    "HhSSSSSSSSSSSShH",
    "HhSBBBsSSsBBBShH",
    "HhSsddsSSsddsShH",
    "HhSSssSLsSssSShH",
    "HhScSSSLsSSScShH",
    "HhSSSSSssSSSSShH",
    "HhsSSSSSSSSSSsHH",
    "HHsSSSppppSSSsHH",
    "HHHsSSSqqSSSsHHH",
    "HHHHssSSSSssHHHH",
]
FACE_COLORS = {"H": HAIR, "h": HAIR_LIGHT, "S": SKIN, "s": SKIN_SHADE, "L": SKIN_LIGHT, "B": BROW,
               "d": (60, 36, 34), "c": (216, 136, 112), "p": LIP, "q": LIP_LIGHT}


def face_image():
    img = Image.new("RGB", (16, len(FACE)))
    for y, row in enumerate(FACE):
        for x, ch in enumerate(row):
            img.putpixel((x, y), FACE_COLORS[ch])
    return img


def north_xy(el, w, h, j, i):
    """Coordenadas de mundo (x, y) del texel (j, i) de la cara norte (de frente, la izquierda es +X)."""
    (x1, y1, _), (x2, y2, _) = el["from"], el["to"]
    return x2 - (j + 0.5) * (x2 - x1) / w, y2 - (i + 0.5) * (y2 - y1) / h


def build():
    groups = {
        "root": ([0, 0, 0], None),
        "cuerpo": ([0, 20, 0], "root"),
        "glow_patron": ([0, 20, 0], "cuerpo"),
        "cabeza": ([0, 43, 0], "cuerpo"),
        "glow_ojos": ([0, 50, 0], "cabeza"),
        # hitbox de BetterModel (prefijo b_) del tamano del cuerpo y la cabeza, para pegarle donde se lo ve
        "b_cuerpo": ([0, 20, 0], "cuerpo"),
    }
    braid_z = [-4.5, -1.0, 2.5]
    for s, side in ((1, "izq"), (-1, "der")):
        for b in range(3):
            groups[f"trenza_{side}_{b}"] = ([s * 20, 44, braid_z[b] + 1], "cuerpo")
        hand = f"glow_mano_{side}"
        groups[hand] = ([s * 32, 26, -10], "root")
        # dedos en abanico desde el borde de arriba de la palma (como el boceto): cuanto mas afuera, mas abiertos
        for k, spread in enumerate(HAND_SPREAD):
            fx = s * (32 + HAND_FINGER_X[k])
            groups[f"glow_dedo_{side}_{k}"] = ([fx, 31, -10], hand, (0, 0, -s * spread))
        # el pulgar sale del costado de la palma que da al cuerpo, para arriba y hacia adentro
        groups[f"glow_pulgar_{side}"] = ([s * (32 - 6), 26, -10], hand, (0, 0, s * 62))

    m = Model(NAME, groups, density=1, tex=256)
    m.out_dir = OUT_DIR

    # tunica: franjas que se ensanchan hacia arriba y la capa de los hombros
    for k in range(8):
        half = 10 + k
        m.cube(f"tunica_{k}", "cuerpo", [-half, 5 * k, -6], [half, 5 * k + 5, 6], "robe")
    m.cube("hombros", "cuerpo", [-19, 40, -7], [19, 43, 7], "shoulder")
    # collar de plata (trapelakucha) y el cuello
    m.cube("collar", "cuerpo", [-9, 33, -7], [9, 40, -6], "necklace")
    m.cube("cuello", "cuerpo", [-7, 40, -7.5], [7, 43, -6.5], "neckband")
    # panel del medio con los motivos que brillan
    m.cube("patron", "glow_patron", [-5, 2, -6.6], [5, 33, -6], "panel")
    m.cube("hitbox_cuerpo", "b_cuerpo", [-12, 0, -6], [12, 66, 6], "hitbox")
    # cabeza octogonal: el medio, los costados mas cortos y el menton
    m.cube("cabeza", "cabeza", [-6, 45, -6], [6, 61, 6], "head")
    m.cube("cabeza_izq", "cabeza", [6, 46, -5], [8, 59, 5], "head")
    m.cube("cabeza_der", "cabeza", [-8, 46, -5], [-6, 59, 5], "head")
    m.cube("menton", "cabeza", [-4, 43, -5], [4, 45, 5], "head")
    m.cube("ojo_izq", "glow_ojos", [2, 50, -6.25], [4, 51, -6], "eye")
    m.cube("ojo_der", "glow_ojos", [-4, 50, -6.25], [-2, 51, -6], "eye")
    # vincha de monedas (trarilonko), las monedas que cuelgan sobre la frente, el gorro de gajos y la flor
    m.cube("vincha", "cabeza", [-8.5, 57, -6.5], [8.5, 59, 6.5], "band")
    m.cube("monedas", "cabeza", [-7, 55, -6.75], [7, 57, -6.5], "coins")
    m.cube("gorro_1", "cabeza", [-8, 59, -6], [8, 63, 6], "cap")
    m.cube("gorro_2", "cabeza", [-6, 63, -4.5], [6, 66, 4.5], "cap")
    m.cube("gorro_3", "cabeza", [-3, 66, -2], [3, 68, 2], "cap")
    m.cube("flor", "cabeza", [-2, 59, -6.5], [2, 63, -6], "flower")
    # trenzas: al costado de la cabeza, por arriba del hombro y colgando por afuera del cuerpo
    for s, side in ((1, "izq"), (-1, "der")):
        for b in range(3):
            z1 = braid_z[b]
            color = BRAIDS[b if s > 0 else b + 3]
            g = f"trenza_{side}_{b}"
            xa = sorted((s * 8.5, s * 10.5))
            xb = sorted((s * 8.5, s * 21.5))
            xc = sorted((s * 19.5, s * 21.5))
            m.cube(f"{g}_a", g, [xa[0], 43, z1], [xa[1], 58, z1 + 2], ("braid", color))
            m.cube(f"{g}_b", g, [xb[0], 43, z1], [xb[1], 45, z1 + 2], ("braid", color))
            m.cube(f"{g}_c", g, [xc[0], 6 + 5 * b, z1], [xc[1], 43, z1 + 2], ("braid", color))
    # manos: palma, cuatro dedos separados y el pulgar
    for s, side in ((1, "izq"), (-1, "der")):
        hx = s * 32
        # palma gruesa de dos escalones: ancha arriba y mas angosta abajo
        for r, (y1, y2, half) in enumerate(((20, 24, 4), (24, 32, 6.5))):
            m.cube(f"palma_{side}_{r}", f"glow_mano_{side}", [hx - half, y1, -12], [hx + half, y2, -8], "palm")
        # dedos cortos y anchos, tan gruesos como la palma
        for k, length in enumerate(HAND_FINGER_LENGTH):
            fx = s * (32 + HAND_FINGER_X[k])
            m.cube(f"dedo_{side}_{k}", f"glow_dedo_{side}_{k}", [fx - 1.4, 31, -11.75],
                   [fx + 1.4, 31 + length, -8.25], "finger")
        tx = s * (32 - 6)
        m.cube(f"pulgar_{side}", f"glow_pulgar_{side}", [tx - 1.5, 26, -11.75], [tx + 1.5, 32.5, -8.25], "finger")

    face = face_image()

    def paint(el, side, w, h, seed):
        part = el["part"]
        if part == "hitbox":
            return Image.new("RGBA", (w, h), (0, 0, 0, 0))  # invisible: solo da el tamano de la hitbox
        kind = part[0] if isinstance(part, tuple) else part
        rng = random.Random(seed)
        img = Image.new("RGBA", (w, h))
        put = lambda j, i, c: img.putpixel((j, i), (*c, 255))  # noqa: E731
        for i in range(h):
            for j in range(w):
                x, y = north_xy(el, w, h, j, i) if side in ("north", "south") else (0.0, 0.0)
                if kind == "robe":
                    c = ROBE
                    if side in ("north", "south"):
                        if j in (0, w - 1):
                            c = GOLD
                        elif round(x) % 5 == 0:
                            c = ROBE_DARK
                        elif y < 1.5:
                            c = GOLD
                    elif side in ("east", "west"):
                        c = ROBE_DARK if j in (0, w - 1) else ROBE
                    else:
                        c = ROBE_DARK
                    put(j, i, gm.shade(c, rng.randint(-4, 4)))
                elif kind == "shoulder":
                    c = GOLD if (i == h - 1 or j in (0, w - 1)) and side != "up" else ROBE_LIGHT
                    put(j, i, gm.shade(c, rng.randint(-4, 4)))
                elif kind == "necklace":
                    # trapelakucha: una fila de monedas arriba, una cadena en el medio y una placa en cruz abajo;
                    # el resto transparente (se ve la tunica)
                    if side != "north":
                        continue
                    c = None
                    if i <= 1 and j % 2 == 0:
                        c = SILVER_LIGHT if i == 0 else SILVER          # monedas alrededor del cuello
                    elif abs(x) < 1.1 and i < h - 3:
                        c = SILVER if i % 2 else SILVER_DARK            # cadena
                    elif i >= h - 3 and (abs(x) < 3.1 if i == h - 2 else abs(x) < 1.1):
                        c = SILVER_LIGHT if abs(x) < 1.1 else SILVER    # placa en cruz
                    elif i == 2 and abs(x) < 5 and j % 2 == 1:
                        c = SILVER_DARK                                 # segunda vuelta de monedas, mas chica
                    if c:
                        put(j, i, c)
                elif kind == "neckband":
                    # el cuello de la tunica, con una guarda dorada
                    put(j, i, GOLD if i == h - 1 else ROBE_LIGHT)
                elif kind == "panel":
                    # rombos escalonados mapuche que brillan, unidos por una linea, sobre azul oscuro
                    c = NAVY
                    if side == "north":
                        cy = 2 + 6.0 * round((y - 2) / 6.0)
                        d = abs(x) + abs(y - cy)
                        if abs(x) < 0.6:
                            c = GLOW
                        if 2.5 <= d < 3.5:
                            c = GLOW
                        elif d < 1.2:
                            c = GLOW_HOT
                        if j in (0, w - 1):
                            c = GOLD
                    put(j, i, c)
                elif kind == "head":
                    if side == "north":
                        u, v = int(FACE_X - x), int(FACE_TOP - y)
                        c = face.getpixel((min(15, max(0, u)), min(len(FACE) - 1, max(0, v))))
                    elif side == "down":
                        c = SKIN_SHADE
                    else:
                        c = HAIR if rng.random() < 0.8 else HAIR_LIGHT
                    put(j, i, c)
                elif kind == "eye":
                    put(j, i, AMBER_HOT if j % 2 == 0 else AMBER)
                elif kind == "band":
                    c = SILVER if (j + i) % 2 else SILVER_LIGHT
                    if side in ("north", "east", "west", "south") and j % 3 == 1:
                        c = SILVER_DARK
                    put(j, i, c)
                elif kind == "coins":
                    # moneditas que cuelgan, con huecos transparentes entre ellas
                    if j % 2 == 0:
                        put(j, i, SILVER_LIGHT if i == 0 else SILVER)
                elif kind == "cap":
                    # gajos de colores que salen de la flor (abajo en el medio de adelante)
                    if side in ("north", "south"):
                        import math
                        angle = math.atan2(x, y - 58)
                        k = int((angle + math.pi / 2) / math.pi * len(RAINBOW))
                        c = RAINBOW[max(0, min(len(RAINBOW) - 1, k))]
                    elif side in ("east", "west"):
                        c = RAINBOW[0] if side == "west" else RAINBOW[-1]
                        c = RAINBOW[(j * len(RAINBOW)) // max(1, w)]
                    else:
                        c = RAINBOW[(j + i) % len(RAINBOW)]
                    put(j, i, gm.shade(c, -14 if (j + i) % 4 == 0 else 0))
                elif kind == "flower":
                    edge = j in (0, w - 1) or i in (0, h - 1)
                    center = j in (1, 2) and i in (1, 2)
                    if side == "north" and center:
                        put(j, i, (240, 240, 250))
                    elif side == "north" and edge and (j + i) % 2 == 0:
                        continue  # petalos: esquinas recortadas
                    else:
                        put(j, i, (52, 96, 210) if (j + i) % 2 else (80, 130, 230))
                elif kind == "braid":
                    # trenza: diagonales que se alternan, un tono mas claro y otro mas oscuro
                    color = part[1]
                    c = gm.shade(color, 22 if (i + (j if side != "up" else 0)) % 3 == 0 else -10)
                    put(j, i, c)
                elif kind == "palm":
                    # palma lisa (esta hecha de dos pedazos: sin lineas para que no quede rayada)
                    c = HAND_SHADE if side not in ("north", "south") else gm.shade(HAND, rng.randint(-3, 3))
                    put(j, i, c)
                elif kind == "finger":
                    # nudillos: dos lineas un poco mas oscuras
                    knuckle = side == "north" and i in (h // 3, (2 * h) // 3)
                    put(j, i, HAND_SHADE if side not in ("north", "south") or knuckle else HAND)
        return img

    t = 4.0
    q = t / 4

    def circle(cx, cy, r, phase):
        """Posiciones que dan una vuelta (en X e Y) en t segundos, empezando en phase (0..3)."""
        pts = [(cx, cy + r), (cx + r, cy), (cx, cy - r), (cx - r, cy)]
        return [("position", k * q, pts[(k + phase) % 4] + (0,)) for k in range(5)]

    anims = {
        # flota con vida: el cuerpo sube y baja y se balancea, la cabeza mira para los costados, las manos dan vueltas
        # a contratiempo girando y las trenzas se mecen
        "idle": (t, "catmullrom", {
            "cuerpo": [("position", 0, (0, 0, 0)), ("position", t / 2, (0, 2.5, 0)), ("position", t, (0, 0, 0)),
                       ("rotation", 0, (0, 0, -2.5)), ("rotation", t / 2, (0, 0, 2.5)), ("rotation", t, (0, 0, -2.5))],
            "cabeza": [("rotation", 0, (0, 0, 0)), ("rotation", q, (-3, 6, 0)), ("rotation", 2 * q, (0, 0, 0)),
                       ("rotation", 3 * q, (-3, -6, 0)), ("rotation", t, (0, 0, 0))],
            "glow_mano_izq": circle(0, 0, 2.5, 0) + [("rotation", 0, (0, 0, -6)), ("rotation", t / 2, (0, 0, 6)),
                                                    ("rotation", t, (0, 0, -6))],
            "glow_mano_der": circle(0, 0, 2.5, 2) + [("rotation", 0, (0, 0, 6)), ("rotation", t / 2, (0, 0, -6)),
                                                    ("rotation", t, (0, 0, 6))],
            **{f"trenza_{side}_{b}": [("rotation", 0, (3 + b, 0, 0)), ("rotation", t / 2, (-3 - b, 0, 0)),
                                      ("rotation", t, (3 + b, 0, 0))]
               for side in ("izq", "der") for b in range(3)},
        }),
        # saltito (0.7 s, el plugin lo mueve en arco): se agacha, se estira con las manos arriba y las trenzas al
        # aire, y cae aplastandose un poco
        "salto": (0.7, "linear", {
            "cuerpo": [("position", 0, (0, 0, 0)), ("position", 0.1, (0, -2.5, 0)), ("position", 0.35, (0, 3, 0)),
                       ("position", 0.6, (0, -2, 0)), ("position", 0.7, (0, 0, 0))],
            "glow_mano_izq": [("position", 0, (0, 0, 0)), ("position", 0.35, (-2, 8, 0)), ("position", 0.7, (0, 0, 0))],
            "glow_mano_der": [("position", 0, (0, 0, 0)), ("position", 0.35, (2, 8, 0)), ("position", 0.7, (0, 0, 0))],
            **{f"trenza_{side}_{b}": [("rotation", 0, (0, 0, 0)), ("rotation", 0.35, (-18 - 4 * b, 0, 0)),
                                      ("rotation", 0.7, (0, 0, 0))]
               for side in ("izq", "der") for b in range(3)},
        }),
    }
    # fase 3: empuja las manos para adelante (hacia el jugador) cuando manda al Sol y a la Luna
    anims["lanzar"] = (0.9, "linear", {
        "glow_mano_izq": [("position", 0, (0, 0, 0)), ("position", 0.3, (-4, 4, -10)), ("position", 0.9, (0, 0, 0))],
        "glow_mano_der": [("position", 0, (0, 0, 0)), ("position", 0.3, (4, 4, -10)), ("position", 0.9, (0, 0, 0))],
        "cuerpo": [("rotation", 0, (0, 0, 0)), ("rotation", 0.3, (6, 0, 0)), ("rotation", 0.9, (0, 0, 0))],
    })
    # fase 3, segundo ataque: levanta las manos arriba de la cabeza (0.6 s); quedan con el centro de la palma en
    # (+-20, 58, -10). En Blockbench la posicion X va invertida: la izquierda (+X) se corre hacia adentro con X positivo.
    anims["levantar_manos"] = (0.6, "linear", {
        "glow_mano_izq": [("position", 0, (0, 0, 0)), ("position", 0.6, (12, 32, 0)),
                          ("rotation", 0, (0, 0, 0)), ("rotation", 0.6, (90, 0, 0))],
        "glow_mano_der": [("position", 0, (0, 0, 0)), ("position", 0.6, (-12, 32, 0)),
                          ("rotation", 0, (0, 0, 0)), ("rotation", 0.6, (90, 0, 0))],
        "cuerpo": [("rotation", 0, (0, 0, 0)), ("rotation", 0.6, (-6, 0, 0))],
    })
    # mientras las manos andan sueltas (son otros modelos), las del cuerpo se esconden
    anims["sin_manos"] = (1.0, "linear", {
        bone: [("scale", 0, (0.01, 0.01, 0.01)), ("scale", 1.0, (0.01, 0.01, 0.01))]
        for bone in ("glow_mano_izq", "glow_mano_der")
    })
    m.build(paint, anims)

    # las manos sueltas del segundo ataque: las mismas manos pero ya acostadas, con la palma para abajo y los dedos
    # para adelante (-Z), y el centro de la palma en el origen. Asi golpean el piso de plano sin depender de ningun
    # giro. Se arman paradas como en el cuerpo y se acuestan: (x, y, z) -> (x, z, -y); los dedos, que se abrian
    # girando en Z, ahora se abren girando en Y.
    def lay(v):
        return [v[0], v[2], -v[1]]

    def lay_box(frm, to):
        a, b = lay(frm), lay(to)
        return [min(a[i], b[i]) for i in range(3)], [max(a[i], b[i]) for i in range(3)]

    for s, side in ((1, "izq"), (-1, "der")):
        hg = {"root": ([0, 0, 0], None), "glow_mano": ([0, 0, 0], "root")}
        for k, spread in enumerate(HAND_SPREAD):
            hg[f"glow_dedo_{k}"] = (lay([s * HAND_FINGER_X[k], 5, 0]), "glow_mano", (0, -s * spread, 0))
        hg["glow_pulgar"] = (lay([s * -6, 0, 0]), "glow_mano", (0, s * 62, 0))
        hm = Model(f"{NAME}_mano_{side}", hg, density=1, tex=128)
        hm.out_dir = OUT_DIR
        for r, (y1, y2, half) in enumerate(((-6, -2, 4), (-2, 6, 6.5))):
            hm.cube(f"palma_{r}", "glow_mano", *lay_box([-half, y1, -2], [half, y2, 2]), "palm")
        for k, length in enumerate(HAND_FINGER_LENGTH):
            fx = s * HAND_FINGER_X[k]
            hm.cube(f"dedo_{k}", f"glow_dedo_{k}", *lay_box([fx - 1.4, 5, -1.75], [fx + 1.4, 5 + length, 1.75]),
                    "finger")
        hm.cube("pulgar", "glow_pulgar", *lay_box([s * -6 - 1.5, 0, -1.75], [s * -6 + 1.5, 6.5, 1.75]), "finger")
        hm.build(paint, {"idle": (1.0, "linear", {
            "glow_mano": [("rotation", 0, (0, 0, 0)), ("rotation", 1.0, (0, 0, 0))]})})


if __name__ == "__main__":
    build()

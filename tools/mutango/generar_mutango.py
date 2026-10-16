"""Genera al Boss Mutango: un bloque de piedra con ojos, brazos-losa a los costados y patitas con pies.

Referencia: tools/mutango/fuentes/Gemini_Generated_Image_oh3uuwoh3uuwoh3u.jpg
Tres versiones: 1 ojo (como la referencia), 2 ojos juntos (comparten el contorno) y 3 ojos en piramide.
Cuerpo 16x17x14, brazos 2x12x8, piernas 4x5x4 en los bordes del cuerpo y pies 6x3x8 que miran hacia los costados
(hueso propio girado FOOT_ANGLE grados para afuera). Densidad 3: la piedra se pinta en celdas de 3x3 (una por unidad, como una textura de 16) con
el borde de cada cara mas oscuro, como la piedra lisa, grietas oscuras de una celda de ancho y algunas matas de musgo (mas arriba y en los pies); el ojo va en celdas de 2x2, mas fino, igual que en la
referencia. En la referencia el ojo tiene una franja gris arriba: aca se completa con el mismo color que abajo.

Uso: python tools/mutango/generar_mutango.py   (deja los tres .bbmodel en src/main/resources/bettermodel/models/)
"""
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import modelo_base as gm  # noqa: E402
from modelo_base import Model  # noqa: E402

NAME = "bigcasares_boss_mutango"
OUT_DIR = Path(__file__).resolve().parents[2] / "src" / "main" / "resources" / "bettermodel" / "models"
DENSITY = 3
FOOT_ANGLE = 80

# esquina superior izquierda (en texeles de la cara frontal 48x51) de cada ojo; cada ojo mide 20x18 y los que van
# juntos se pisan una celda para compartir el contorno
EYE_LAYOUTS = {
    NAME: [(14, 13)],
    f"{NAME}_2ojos": [(5, 13), (23, 13)],
    f"{NAME}_3ojos": [(14, 8), (5, 24), (23, 24)],
}
# las estatuas de las ruinas: los mismos tres, con los ojos cerrados (despiertan cuando les llega la luz del agujero)
SLEEPING = {f"{name}_dormido": layout for name, layout in EYE_LAYOUTS.items()}

# ojo cerrado: el parpado de piedra tapa todo el ojo y abajo queda la linea de las pestanas, curvada
# ojo cerrado, en la grilla de la piedra: cada letra es una celda de DENSITY x DENSITY texeles (como los pixeles de la
# piedra), y el ojo se pone alineado a esa grilla, centrado donde iba el ojo abierto
EYE_CLOSED = [".gggg.",
              "KggggK",
              ".KKKK."]

# tonos sacados de la referencia (cara frontal: 112-136, manchas claras 144, bordes oscuros 88-96)
STONE = (130, 130, 131)
STONE_LIGHT = (148, 148, 149)
STONE_LIGHTER = (162, 162, 163)
STONE_DARK = (114, 114, 116)
STONE_EDGE = (96, 96, 98)
STONE_CORNER = (82, 82, 84)
CRACK = (58, 58, 60)
# musgo, como el de la piedra musgosa de Minecraft
MOSS = [(78, 112, 38), (96, 132, 46), (64, 94, 32), (110, 146, 54)]

# ojo 10x9 en celdas de 2x2 texeles, filas de arriba a abajo; "." deja la piedra
EYE = ["...KKKK...",
       "..KPPPPK..",
       ".KpLBBLpK.",
       "KPWBCRBWPK",
       "KWWbROkWPK",
       "KPWbRkbWPK",
       ".KPLBBLPK.",
       "..KPPPPK..",
       "...KKKK..."]
EYE_COLORS = {
    "g": (150, 150, 154),   # parpado de piedra (ojo cerrado de las estatuas)
    "K": (6, 6, 8),         # contorno
    "P": (236, 168, 136),   # rosado del ojo
    "p": (204, 168, 160),   # rosado apagado de las esquinas
    "W": (236, 236, 232),   # blanco
    "L": (74, 132, 194),    # iris claro
    "B": (54, 72, 162),     # iris
    "b": (38, 52, 96),      # iris oscuro
    "C": (255, 216, 180),   # brillo
    "R": (214, 80, 30),     # pupila roja
    "O": (246, 118, 52),    # pupila naranja
    "k": (12, 4, 4),        # centro de la pupila
}


def build(name, eyes, closed=False):
    groups = {
        "root": ([0, 0, 0], None),
        "body": ([0, 8, 0], "root"),
        "rightarm": ([9, 23, 0], "body"),
        "leftarm": ([-9, 23, 0], "body"),
        "rightleg": ([5, 8, 0], "root"),
        "leftleg": ([-5, 8, 0], "root"),
        # los pies giran sobre el centro de la pierna para que la punta mire hacia afuera
        # hitbox de BetterModel (prefijo b_): hijo del cuerpo con el pivote en su centro, asi cuando el cuerpo se
        # inclina o se tira de cabeza la hitbox se mueve con el y le podes pegar donde lo ves
        "b_cuerpo": ([0, 16.5, 0], "body"),
        "rightfoot": ([5, 0, 0], "rightleg", (0, -FOOT_ANGLE, 0)),
        "leftfoot": ([-5, 0, 0], "leftleg", (0, FOOT_ANGLE, 0)),
    }
    m = Model(name, groups, density=DENSITY, tex=256)
    m.out_dir = OUT_DIR
    m.cube("cuerpo", "body", [-8, 8, -7], [8, 25, 7], "body")
    m.cube("hitbox_cuerpo", "b_cuerpo", [-8, 8, -7], [8, 25, 7], "hitbox")
    m.cube("brazo_der", "rightarm", [8, 11, -4], [10, 23, 4], "arm")
    m.cube("brazo_izq", "leftarm", [-10, 11, -4], [-8, 23, 4], "arm")
    for side, sx in (("der", 1), ("izq", -1)):
        lx = sorted((3 * sx, 7 * sx))
        fx = sorted((2 * sx, 8 * sx))
        m.cube(f"pierna_{side}", "rightleg" if sx > 0 else "leftleg", [lx[0], 3, -2], [lx[1], 8, 2], "leg")
        m.cube(f"pie_{side}", "rightfoot" if sx > 0 else "leftfoot", [fx[0], 0, -5], [fx[1], 3, 3], "foot")

    def stone(w, h, rng):
        """Piedra lisa: manchas claras y oscuras agrupadas, con el borde de la cara mas oscuro."""
        cw, ch = -(-w // DENSITY), -(-h // DENSITY)
        grid = [[rng.choices((0, 1, 2, -1), (52, 22, 6, 20))[0] for _ in range(cw)] for _ in range(ch)]
        # una pasada de suavizado para que las manchas se junten en grupos, como en la referencia
        smooth = [row[:] for row in grid]
        for y in range(ch):
            for x in range(cw):
                near = [grid[y + dy][x + dx] for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0))
                        if 0 <= y + dy < ch and 0 <= x + dx < cw]
                if near.count(grid[y][x]) == 0 and rng.random() < 0.5:
                    smooth[y][x] = rng.choice(near)
        tones = {0: STONE, 1: STONE_LIGHT, 2: STONE_LIGHTER, -1: STONE_DARK}
        img = Image.new("RGBA", (w, h))
        for y in range(ch):
            for x in range(cw):
                edge_x, edge_y = x in (0, cw - 1), y in (0, ch - 1)
                if edge_x and edge_y:
                    base = STONE_CORNER
                elif edge_x or edge_y:
                    base = STONE_EDGE if smooth[y][x] < 1 else STONE_DARK
                else:
                    base = tones[smooth[y][x]]
                color = (*gm.shade(base, rng.randint(-2, 2)), 255)
                for dy in range(DENSITY):
                    for dx in range(DENSITY):
                        px, py = x * DENSITY + dx, y * DENSITY + dy
                        if px < w and py < h:
                            img.putpixel((px, py), color)
        return img

    def cracks(img, rng):
        """Grietas zigzagueantes de una celda de ancho que entran desde el borde, con alguna rama; cada celda de
        grieta deja la de abajo un poco mas clara para que parezca hundida."""
        w, h = img.size
        cw, ch = -(-w // DENSITY), -(-h // DENSITY)
        if cw < 4 or ch < 4:
            return

        def cell(x, y, color):
            for dy in range(DENSITY):
                for dx in range(DENSITY):
                    px, py = x * DENSITY + dx, y * DENSITY + dy
                    if px < w and py < h:
                        img.putpixel((px, py), color)

        def walk(x, y, dx, dy, steps, can_branch):
            for i in range(steps):
                if not (1 <= x < cw - 1 and 1 <= y < ch - 1):
                    return
                cell(x, y, (*gm.shade(CRACK, rng.randint(-3, 3)), 255))
                if y + 1 < ch - 1:
                    under = img.getpixel((x * DENSITY, (y + 1) * DENSITY))
                    if under[0] > CRACK[0] + 10:
                        cell(x, y + 1, (*gm.shade(under[:3], 12), 255))
                if can_branch and i == steps // 2 and rng.random() < 0.45:
                    walk(x, y, dy or rng.choice((-1, 1)), dx or rng.choice((-1, 1)), steps // 2, False)
                # avanza en la direccion principal o se corre de costado, asi queda en zigzag
                if rng.random() < 0.6:
                    x, y = x + dx, y + dy
                else:
                    side = rng.choice((-1, 1))
                    x, y = x + dy * side, y + dx * side

        for _ in range(int(cw * ch / 100 + rng.random())):
            edge = rng.randrange(4)
            if edge == 0:
                x, y, dx, dy = rng.randrange(1, cw - 1), 1, 0, 1
            elif edge == 1:
                x, y, dx, dy = rng.randrange(1, cw - 1), ch - 2, 0, -1
            elif edge == 2:
                x, y, dx, dy = 1, rng.randrange(1, ch - 1), 1, 0
            else:
                x, y, dx, dy = cw - 2, rng.randrange(1, ch - 1), -1, 0
            walk(x, y, dx, dy, rng.randint(4, 9), True)

    def moss(img, rng, face):
        """Matas de musgo chicas: sobre todo en las caras de arriba; en los costados nacen arriba y chorrean un poco."""
        if face == "down":
            return
        w, h = img.size
        cw, ch = -(-w // DENSITY), -(-h // DENSITY)
        area = cw * ch
        count = int(area / 55 + rng.random()) if face == "up" else int(area / 140 + rng.random() * 0.8)
        for _ in range(count):
            x = rng.randrange(cw)
            y = rng.randrange(ch) if face == "up" else rng.randrange(min(3, ch))
            patch = {(x, y)}
            for _ in range(rng.randint(2, 8)):
                px, py = rng.choice(sorted(patch))
                # en los costados crece mas para abajo que para arriba
                dx, dy = rng.choice(((1, 0), (-1, 0), (0, 1), (0, 1), (0, -1)) if face != "up" else
                                    ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if 0 <= px + dx < cw and 0 <= py + dy < ch:
                    patch.add((px + dx, py + dy))
            for cx, cy in patch:
                color = (*gm.shade(rng.choice(MOSS), rng.randint(-4, 4)), 255)
                for dy in range(DENSITY):
                    for dx in range(DENSITY):
                        tx, ty = cx * DENSITY + dx, cy * DENSITY + dy
                        if tx < w and ty < h:
                            img.putpixel((tx, ty), color)

    def paint(el, face, w, h, seed):
        # la estatua dormida tiene la misma piedra (grietas y musgo) que el despierto: solo cambian los ojos
        seed = seed.replace("_dormido", "")
        if el["part"] == "hitbox":
            return Image.new("RGBA", (w, h), (0, 0, 0, 0))  # invisible: solo da el tamano de la hitbox
        rng = random.Random(seed)
        img = stone(w, h, rng)
        cracks(img, rng)
        moss(img, rng, face)
        if el["part"] == "body" and face == "north":
            for ox, oy in eyes:
                if closed:
                    # centro del ojo abierto (20 x 18 texeles) -> esquina del cerrado, redondeada a la grilla
                    cells_w, cells_h = len(EYE_CLOSED[0]), len(EYE_CLOSED)
                    cx = round((ox + 10 - cells_w * DENSITY / 2) / DENSITY)
                    cy = round((oy + 9 - cells_h * DENSITY / 2) / DENSITY)
                    for y, row in enumerate(EYE_CLOSED):
                        for x, ch in enumerate(row):
                            if ch == ".":
                                continue
                            for dy in range(DENSITY):
                                for dx in range(DENSITY):
                                    img.putpixel(((cx + x) * DENSITY + dx, (cy + y) * DENSITY + dy),
                                                 (*EYE_COLORS[ch], 255))
                    continue
                for y, row in enumerate(EYE):
                    for x, ch in enumerate(row):
                        if ch == ".":
                            continue
                        for dy in range(2):
                            for dx in range(2):
                                img.putpixel((ox + x * 2 + dx, oy + y * 2 + dy), (*EYE_COLORS[ch], 255))
        return img

    anims = {
        "idle": (2.0, "catmullrom", {
            "body": [("position", 0, (0, 0, 0)), ("position", 1.0, (0, -0.5, 0)), ("position", 2.0, (0, 0, 0))],
            "rightarm": [("rotation", 0, (0, 0, 0)), ("rotation", 1.0, (0, 0, -4)), ("rotation", 2.0, (0, 0, 0))],
            "leftarm": [("rotation", 0, (0, 0, 0)), ("rotation", 1.0, (0, 0, 4)), ("rotation", 2.0, (0, 0, 0))],
        }),
        # caminata dura, de muneco de piedra: se inclina hacia un costado levantando un poco la cabeza, levanta esa
        # pierna casi sin doblarla, la apoya de golpe cabeceando para adelante y repite del otro lado (signos de
        # Blockbench: X positivo inclina para adelante, X negativo en la pierna la lleva para adelante). Ademas gira
        # hacia el lado del paso (Y negativo adelanta el costado derecho) y la pierna levantada gira con el cuerpo. El cuerpo sube lo justo
        # para que la pierna de apoyo no se despegue: 8 * sin(10) = 0.87.
        "walk": (1.2, "linear", {
            "body": [("rotation", 0, (0, 0, 0)), ("rotation", 0.3, (-3, -21, 10)), ("rotation", 0.5, (4, -17, 0)),
                     ("rotation", 0.6, (0, 0, 0)), ("rotation", 0.9, (-3, 21, -10)), ("rotation", 1.1, (4, 17, 0)),
                     ("rotation", 1.2, (0, 0, 0)),
                     ("position", 0, (0, 0, 0)), ("position", 0.3, (0, 0.87, 0)), ("position", 0.5, (0, 0, 0)),
                     ("position", 0.9, (0, 0.87, 0)), ("position", 1.1, (0, 0, 0)), ("position", 1.2, (0, 0, 0))],
            "rightleg": [("rotation", 0, (0, 0, 0)), ("rotation", 0.3, (-12, -21, 0)), ("rotation", 0.5, (0, 0, 0)),
                         ("rotation", 1.2, (0, 0, 0)),
                         ("position", 0, (0, 0, 0)), ("position", 0.3, (0, 1.75, 0)), ("position", 0.5, (0, 0, 0)),
                         ("position", 1.2, (0, 0, 0))],
            "leftleg": [("rotation", 0, (0, 0, 0)), ("rotation", 0.6, (0, 0, 0)), ("rotation", 0.9, (-12, 21, 0)),
                        ("rotation", 1.1, (0, 0, 0)), ("rotation", 1.2, (0, 0, 0)),
                        ("position", 0, (0, 0, 0)), ("position", 0.6, (0, 0, 0)), ("position", 0.9, (0, 1.75, 0)),
                        ("position", 1.1, (0, 0, 0)), ("position", 1.2, (0, 0, 0))],
        }),
        # golpe: levanta los dos brazos y los baja contra el piso
        "attack": (1.0, "linear", {
            "rightarm": [("rotation", 0, (0, 0, 0)), ("rotation", 0.4, (-150, 0, 0)), ("rotation", 0.6, (-20, 0, 0)),
                         ("rotation", 1.0, (0, 0, 0))],
            "leftarm": [("rotation", 0, (0, 0, 0)), ("rotation", 0.4, (-150, 0, 0)), ("rotation", 0.6, (-20, 0, 0)),
                        ("rotation", 1.0, (0, 0, 0))],
            "body": [("rotation", 0, (0, 0, 0)), ("rotation", 0.4, (8, 0, 0)), ("rotation", 0.6, (-10, 0, 0)),
                     ("rotation", 1.0, (0, 0, 0))],
        }),
        # fase 1, embestida de cabra: se va inclinando para atras despacio (cada vez mas lento), subiendo un poco el cuerpo
        # para que la esquina de abajo no se meta en las piernas, con los brazos para atras, y a los 1.2 s se tira de golpe de cabeza para adelante (el plugin lanza la
        # entidad en ese momento). Se queda clavado un rato y vuelve. X negativo = para atras (signos de Blockbench).
        "embestida": (2.2, "linear", {
            "body": [("rotation", 0, (0, 0, 0)), ("rotation", 0.4, (-11, 0, 0)), ("rotation", 0.8, (-19, 0, 0)),
                     ("rotation", 1.2, (-24, 0, 0)), ("rotation", 1.32, (40, 0, 0)), ("rotation", 1.7, (36, 0, 0)),
                     ("rotation", 2.2, (0, 0, 0)),
                     ("position", 0, (0, 0, 0)), ("position", 1.2, (0, 0.6, 0)), ("position", 1.32, (0, 1.8, 0)),
                     ("position", 1.7, (0, 1.2, 0)), ("position", 2.2, (0, 0, 0))],
            "rightarm": [("rotation", 0, (0, 0, 0)), ("rotation", 1.2, (35, 0, 6)), ("rotation", 1.32, (-50, 0, 0)),
                         ("rotation", 1.7, (-45, 0, 0)), ("rotation", 2.2, (0, 0, 0))],
            "leftarm": [("rotation", 0, (0, 0, 0)), ("rotation", 1.2, (35, 0, -6)), ("rotation", 1.32, (-50, 0, 0)),
                        ("rotation", 1.7, (-45, 0, 0)), ("rotation", 2.2, (0, 0, 0))],
        }),
        # cabezazo de cerca: la misma idea que la embestida pero rapido (se carga 0.3 s y a los 0.35 s pega)
        "cabezazo": (0.9, "linear", {
            "body": [("rotation", 0, (0, 0, 0)), ("rotation", 0.3, (-18, 0, 0)), ("rotation", 0.35, (40, 0, 0)),
                     ("rotation", 0.6, (36, 0, 0)), ("rotation", 0.9, (0, 0, 0)),
                     ("position", 0, (0, 0, 0)), ("position", 0.3, (0, 0.4, 0)), ("position", 0.35, (0, 1.8, 0)),
                     ("position", 0.6, (0, 1.2, 0)), ("position", 0.9, (0, 0, 0))],
            "rightarm": [("rotation", 0, (0, 0, 0)), ("rotation", 0.3, (30, 0, 5)), ("rotation", 0.35, (-50, 0, 0)),
                         ("rotation", 0.6, (-45, 0, 0)), ("rotation", 0.9, (0, 0, 0))],
            "leftarm": [("rotation", 0, (0, 0, 0)), ("rotation", 0.3, (30, 0, -5)), ("rotation", 0.35, (-50, 0, 0)),
                        ("rotation", 0.6, (-45, 0, 0)), ("rotation", 0.9, (0, 0, 0))],
        }),
    }
    anims.update(fase_2_anims())
    m.build(paint, anims)


def _arms(frames):
    """Mismos cuadros para los dos brazos; el giro en Z se espeja (positivo = la mano hacia el centro)."""
    right = [(ch, t, (x, y, z)) for ch, t, (x, y, z) in frames]
    left = [(ch, t, (x, -y, -z)) for ch, t, (x, y, z) in frames]
    return {"rightarm": right, "leftarm": left}


def fase_2_anims():
    """Animaciones de la fase 2 (los tiempos coinciden con MutangoPhaseTwo.java del plugin)."""
    # brazos para arriba agarrando los pies del de arriba
    hold = (-165, 0, 8)
    return {
        # pina de bebe zombie: los dos brazos para adelante, rapido
        "golpe": (0.45, "linear", {
            **_arms([("rotation", 0, (0, 0, 0)), ("rotation", 0.12, (-100, 0, 0)), ("rotation", 0.25, (-70, 0, 0)),
                     ("rotation", 0.45, (0, 0, 0))]),
            "body": [("rotation", 0, (0, 0, 0)), ("rotation", 0.12, (6, 0, 0)), ("rotation", 0.45, (0, 0, 0))],
        }),
        # salto para subirse arriba de otro (0.8 s): se agacha, salta estirado, encoge las piernas en el aire y cae
        # aplastandose. El plugin lo mueve en arco entre 0.2 y 0.7 s.
        "saltar": (0.8, "linear", {
            "body": [("position", 0, (0, 0, 0)), ("position", 0.2, (0, -1.2, 0)), ("position", 0.35, (0, 0.8, 0)),
                     ("position", 0.6, (0, 0.4, 0)), ("position", 0.7, (0, -1.0, 0)), ("position", 0.8, (0, 0, 0)),
                     ("rotation", 0, (0, 0, 0)), ("rotation", 0.2, (8, 0, 0)), ("rotation", 0.35, (-6, 0, 0)),
                     ("rotation", 0.6, (0, 0, 0)), ("rotation", 0.8, (0, 0, 0))],
            **_arms([("rotation", 0, (0, 0, 0)), ("rotation", 0.2, (30, 0, 0)), ("rotation", 0.35, (-160, 0, 10)),
                     ("rotation", 0.6, (-60, 0, 0)), ("rotation", 0.7, (20, 0, -10)), ("rotation", 0.8, (0, 0, 0))]),
            "rightleg": [("rotation", 0, (0, 0, 0)), ("rotation", 0.35, (20, 0, 0)), ("rotation", 0.5, (-30, 0, 0)),
                         ("rotation", 0.7, (0, 0, 0))],
            "leftleg": [("rotation", 0, (0, 0, 0)), ("rotation", 0.35, (20, 0, 0)), ("rotation", 0.5, (-30, 0, 0)),
                        ("rotation", 0.7, (0, 0, 0))],
        }),
        # el de abajo agarra al de arriba de los pies (en loop mientras esta apilado), haciendo un poco de fuerza
        "sostener": (1.0, "linear", {
            **_arms([("rotation", 0, hold), ("rotation", 0.5, (-162, 0, 9)), ("rotation", 1.0, hold)]),
        }),
        # el del medio tira para atras al de arriba (0.5 s) y lo lanza de golpe para adelante (a los 0.55 s se suelta)
        "lanzar": (1.0, "linear", {
            **_arms([("rotation", 0, hold), ("rotation", 0.5, (-205, 0, 6)), ("rotation", 0.6, (-70, 0, 0)),
                     ("rotation", 1.0, (0, 0, 0))]),
            "body": [("rotation", 0, (0, 0, 0)), ("rotation", 0.5, (-12, 0, 0)), ("rotation", 0.6, (15, 0, 0)),
                     ("rotation", 1.0, (0, 0, 0))],
        }),
        # el de arriba mientras lo tiran para atras: se echa para atras con los brazos atras
        "tomar_impulso": (0.55, "linear", {
            "body": [("rotation", 0, (0, 0, 0)), ("rotation", 0.5, (-25, 0, 0)), ("rotation", 0.55, (-25, 0, 0))],
            **_arms([("rotation", 0, (0, 0, 0)), ("rotation", 0.5, (35, 0, 0)), ("rotation", 0.55, (35, 0, 0))]),
        }),
        # caido en la fase 1 (en loop hasta que caen los tres): desplomado, con el cuerpo hundido sobre las piernas,
        # inclinado para adelante y los brazos colgando; respira apenas
        "derrotado": (2.0, "linear", {
            "body": [("rotation", 0, (18, 0, 4)), ("rotation", 1.0, (15, 0, 3)), ("rotation", 2.0, (18, 0, 4)),
                     ("position", 0, (0, -2.5, 0)), ("position", 1.0, (0, -2.2, 0)), ("position", 2.0, (0, -2.5, 0))],
            **_arms([("rotation", 0, (-20, 0, -12)), ("rotation", 1.0, (-16, 0, -10)), ("rotation", 2.0, (-20, 0, -12))]),
        }),
        # volando de cabeza hacia el jugador (en loop): cuerpo acostado para adelante, brazos y piernas para atras
        "lanzado": (0.4, "linear", {
            "body": [("rotation", 0, (80, 0, 3)), ("rotation", 0.2, (80, 0, -3)), ("rotation", 0.4, (80, 0, 3))],
            **_arms([("rotation", 0, (60, 0, 0)), ("rotation", 0.2, (70, 0, 0)), ("rotation", 0.4, (60, 0, 0))]),
            "rightleg": [("rotation", 0, (55, 0, 0)), ("rotation", 0.4, (55, 0, 0))],
            "leftleg": [("rotation", 0, (55, 0, 0)), ("rotation", 0.4, (55, 0, 0))],
        }),
    }


if __name__ == "__main__":
    for model_name, eye_layout in EYE_LAYOUTS.items():
        build(model_name, eye_layout)
    for model_name, eye_layout in SLEEPING.items():
        build(model_name, eye_layout, closed=True)

"""Caras del Sol y la Luna dibujadas a mano en pixel art (1 texel por pixel de modelo), mirando de frente.

Se dibuja la mitad izquierda de la cara y se espeja, asi queda simetrica. Las coordenadas de los rasgos estan en una
grilla de 48 con el centro de la cara entre las columnas 23 y 24 (la del Sol); la Luna usa los mismos rasgos corridos
para que su cara quede justo en el medio de su disco.

Uso: python tools/caras_sol_luna.py <salida.png>   (guarda las dos texturas ampliadas para revisarlas)
"""
import math
import random
import sys

from PIL import Image

SUN_PALETTE = {
    "light": (255, 240, 140), "base": (255, 220, 76), "shade": (252, 192, 52), "deep": (240, 160, 40),
    "ring": (214, 146, 36), "ring_light": (246, 198, 72),
    "brow": (176, 96, 24), "lid": (122, 58, 18), "lid_low": (214, 142, 50),
    "white": (252, 246, 228), "iris": (54, 108, 206), "iris_dark": (28, 56, 128), "pupil": (16, 18, 34),
    "nose": (226, 150, 34), "nose_dark": (176, 100, 26), "nose_light": (255, 236, 140),
    "cheek": (246, 150, 48), "mouth": (128, 50, 22), "lip": (222, 128, 46), "lip_shadow": (208, 128, 40),
}
MOON_PALETTE = {
    "light": (214, 216, 228), "base": (184, 186, 200), "shade": (158, 160, 176), "deep": (128, 130, 148),
    "ring": (118, 120, 138), "ring_light": (150, 152, 168),
    "brow": (104, 106, 124), "lid": (62, 62, 78), "lid_low": (150, 152, 168),
    "white": (232, 234, 240), "iris": (106, 118, 150), "iris_dark": (70, 78, 104), "pupil": (28, 28, 38),
    "nose": (166, 168, 184), "nose_dark": (120, 122, 140), "nose_light": (226, 228, 238),
    "cheek": (176, 170, 186), "mouth": (70, 66, 82), "lip": (150, 140, 158), "lip_shadow": (162, 164, 180),
    "crater": (140, 142, 160), "crater_dark": (118, 120, 138), "crater_rim": (206, 208, 220),
}


def _left_half_features():
    """Rasgos de la mitad izquierda (x <= 23) en la grilla de 48: lista de (x, y, color). Cara seria y realista:
    boca neutra, cejas rectas que bajan hacia la nariz, parpado pesado y la estructura marcada con sombras."""
    f = []

    def row(y, x1, x2, color):
        f.extend((x, y, color) for x in range(x1, x2 + 1))

    # sienes y cuencas de los ojos
    for y in range(16, 21):
        f.append((10, y, "shade"))
    f += [(21, 17, "shade"), (21, 18, "shade"), (21, 19, "shade")]
    # cejas casi rectas, mas gruesas y un poco mas bajas hacia la nariz (seria)
    row(14, 13, 17, "brow")
    row(15, 15, 20, "brow")
    f += [(20, 16, "brow"), (21, 16, "brow")]
    # ojo: parpado de arriba pesado que tapa la parte de arriba del iris, blanco, iris y pupila, parpado de abajo
    row(17, 14, 20, "lid")
    f += [(13, 18, "lid"), (21, 18, "lid")]
    row(18, 15, 19, "white")
    row(19, 15, 19, "white")
    row(18, 16, 18, "iris_dark")
    row(19, 16, 18, "iris")
    f += [(17, 18, "pupil"), (17, 19, "pupil"), (16, 19, "nose_light")]
    row(20, 15, 19, "lid_low")
    row(21, 16, 18, "shade")
    # nariz larga y fina, solo sombra: puente claro, un costado sombreado, ala, fosas y la punta con luz
    for y in range(19, 27):
        f.append((23, y, "nose_light"))
    for y in range(21, 27):
        f.append((21, y, "shade"))
    f += [(20, 26, "nose"), (20, 27, "nose"), (21, 27, "nose")]
    f += [(23, 27, "nose_light"), (22, 27, "base")]
    f += [(21, 28, "nose_dark"), (22, 28, "nose_dark"), (23, 28, "nose")]
    row(29, 21, 23, "shade")
    # surco entre la nariz y la boca
    f += [(23, 30, "shade"), (23, 31, "shade")]
    # pliegues al costado de la boca
    f += [(19, 29, "shade"), (18, 30, "shade"), (18, 31, "shade"), (17, 32, "shade"), (17, 33, "shade")]
    # pomulos: hueco de sombra debajo
    f += [(12, 29, "shade"), (13, 30, "shade"), (14, 31, "shade"), (11, 28, "shade")]
    # boca neutra: labio de arriba, linea de la boca recta, labio de abajo y su sombra
    row(32, 20, 23, "lip_shadow")
    row(33, 18, 23, "mouth")
    row(34, 20, 23, "lip")
    row(35, 20, 23, "shade")
    # menton y mandibula
    row(38, 21, 23, "shade")
    row(40, 18, 23, "deep")
    return f


def draw_face(img, cx, cy, palette):
    """Dibuja los rasgos con el centro de la cara entre las columnas cx-1 y cx, en la fila cy (grilla del Sol: 24, 24)."""
    dx, dy = cx - 24, cy - 24
    pix = img.load()
    for x, y, color in _left_half_features():
        c = palette[color]
        pix[x + dx, y + dy] = c
        pix[(47 - x) + dx, y + dy] = c


def _shaded_disc(size, palette, light_center=(0.0, -0.15)):
    """Disco con sombreado suave de esfera (claro arriba en el centro, mas oscuro hacia el borde) y un anillo."""
    img = Image.new("RGB", (size, size), palette["ring"])
    pix = img.load()
    for y in range(size):
        for x in range(size):
            px = (x + 0.5) / size * 2 - 1
            py = (y + 0.5) / size * 2 - 1
            r = math.hypot(px, py)
            lr = math.hypot(px - light_center[0], py - light_center[1])
            if r > 1.0:
                continue
            if r > 1.0 - 2.2 / (size / 2):
                angle = math.atan2(py, px)
                pix[x, y] = palette["ring_light"] if int((angle + math.pi) / (2 * math.pi) * 36) % 3 == 0 \
                    else palette["ring"]
            elif lr < 0.45:
                pix[x, y] = palette["light"]
            elif lr < 0.78:
                pix[x, y] = palette["base"]
            elif r < 0.86:
                pix[x, y] = palette["shade"]
            else:
                pix[x, y] = palette["deep"]
    return img


def sun_face(size=48):
    img = _shaded_disc(size, SUN_PALETTE)
    draw_face(img, size // 2, size // 2, SUN_PALETTE)
    return img


def moon_face(size=56, seed=7, with_face=True):
    """La Luna: grises con manchas, crateres alrededor (nunca encima de la cara) y la cara en el medio del disco.
    Sin cara (with_face=False) sirve para la parte de atras: el ovalo liso se llena de crateres."""
    pal = MOON_PALETTE
    img = _shaded_disc(size, pal, light_center=(0.0, 0.0))
    pix = img.load()
    rng = random.Random(seed)
    c = size / 2

    def in_disc(x, y, margin=2.5):
        return math.hypot(x + 0.5 - c, y + 0.5 - c) < c - margin

    # la zona de la cara (un ovalo un poco mas claro y liso)
    face_rx, face_ry, face_cy = 12.5, 14.5, c + 1.0

    def in_face(x, y, pad=0.0):
        if not with_face:
            return False
        return ((x + 0.5 - c) / (face_rx + pad)) ** 2 + ((y + 0.5 - face_cy) / (face_ry + pad)) ** 2 <= 1.0

    # manchas de mares (zonas mas oscuras) fuera de la cara
    for _ in range(9):
        mx, my = rng.uniform(4, size - 4), rng.uniform(4, size - 4)
        mr = rng.uniform(3, 6)
        for y in range(size):
            for x in range(size):
                if in_disc(x, y) and not in_face(x, y, 2) and math.hypot(x + 0.5 - mx, y + 0.5 - my) < mr:
                    pix[x, y] = pal["shade"] if pix[x, y] != pal["deep"] else pal["crater_dark"]
    # crateres: hueco, sombra adentro arriba a la izquierda y borde claro abajo a la derecha
    placed = []
    for _ in range(600):
        if len(placed) >= 9:
            break
        kx, ky = rng.uniform(3, size - 3), rng.uniform(3, size - 3)
        kr = rng.choice((2.5, 3.0, 3.5, 4.0))
        if not in_disc(int(kx), int(ky), kr + 2.5) or in_face(int(kx), int(ky), kr + 2.0):
            continue
        if any(math.hypot(kx - ox, ky - oy) < kr + orr + 1.5 for ox, oy, orr in placed):
            continue
        for y in range(size):
            for x in range(size):
                ddx, ddy = x + 0.5 - kx, y + 0.5 - ky
                d = math.hypot(ddx, ddy)
                if d < kr - 1 and ddx + ddy < -0.5:
                    pix[x, y] = pal["crater_dark"]
                elif d < kr:
                    pix[x, y] = pal["crater"]
                elif d < kr + 1 and ddx + ddy > 0.5:
                    pix[x, y] = pal["crater_rim"]
        placed.append((kx, ky, kr))
    # puntitos sueltos
    for _ in range(40):
        x, y = rng.randrange(size), rng.randrange(size)
        if in_disc(x, y) and not in_face(x, y, 1):
            pix[x, y] = rng.choice((pal["deep"], pal["light"], pal["shade"]))
    # la cara: ovalo claro y los rasgos, centrados en el disco
    if not with_face:
        return img
    for y in range(size):
        for x in range(size):
            if in_face(x, y):
                lr = math.hypot((x + 0.5 - c) / face_rx, (y + 0.5 - face_cy + 3) / face_ry)
                pix[x, y] = pal["light"] if lr < 0.55 else pal["base"]
    draw_face(img, size // 2, int(face_cy) - 1, pal)
    return img


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "caras_sol_luna.png"
    sun, moon = sun_face(), moon_face()
    scale = 10
    sheet = Image.new("RGB", ((sun.width + moon.width) * scale + 20, moon.height * scale), (150, 190, 232))
    sheet.paste(sun.resize((sun.width * scale, sun.height * scale), Image.NEAREST), (0, 0))
    sheet.paste(moon.resize((moon.width * scale, moon.height * scale), Image.NEAREST), (sun.width * scale + 20, 0))
    sheet.save(out)
    print(out)

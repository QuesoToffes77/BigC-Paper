"""Genera el Sol y la Luna con cara (tools/mutango/fuentes/sol.jpg y luna.jpg) como modelos de cuboides.

Como los modelos buenos de las referencias (bien1-3): pocos cuboides grandes en capas, con una textura pixelada de
baja resolucion (1 texel por pixel de modelo, 16 por bloque) pintada encima; nada de un cubo por voxel.

- Los discos son circulos escalonados en pasos de 4 pixeles (se ve "minecraftero"): cada franja horizontal es un
  cuboide, y las franjas iguales seguidas se juntan en uno solo.
- Las caras estan dibujadas a mano en pixel art (caras_sol_luna.py), mirando de frente y simetricas; la nariz del
  Sol es solo textura (sin relieve) y la cara de la Luna esta justo en el medio de su disco. Atras de la Luna, solo
  crateres.
- Sol: disco de la cara, una corona naranja un poco mas grande atras y 16 rayos escalonados (largos y cortos
  alternados, cada uno en su hueso girado), unos mas atras que otros para que tenga profundidad.
- Luna: disco grueso con dos placas mas chicas adelante, asi la cara queda un poco abombada como una esfera.

Uso: python tools/mutango/generar_sol_luna.py   (deja los .bbmodel en boss_mutango/ y una comparacion con las referencias)
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import caras_sol_luna as caras  # noqa: E402
import modelo_base as gm  # noqa: E402
from modelo_base import Model  # noqa: E402

OUT_DIR = Path(__file__).resolve().parents[2] / "src" / "main" / "resources" / "bettermodel" / "models"
REFS = Path(__file__).resolve().parent / "fuentes"
STEP = 4            # tamano del escalon de los circulos (pixeles de modelo)
COLORS = 36         # colores por textura (paleta reducida, pixel art)

# ------------------------------------------------------------------ texturas desde las referencias

# sol.jpg: elipse de la cara con su anillo (se ve un poco de costado) y luna.jpg: elipse de la luna y centro de la cara
SUN_FACE = (448, 505, 203, 270)          # eje de la cara (entre los ojos), centro y, medio ancho izquierdo, radio y
MOON_DISC = (545, 482, 385, 422)
MOON_FACE_CENTER = (447, 540)            # entre las pupilas
# las caras miran de frente: se usa la mitad izquierda (la mejor iluminada) espejada sobre el eje de la nariz.
# En la Luna solo dentro de esta elipse (en coordenadas del disco, -1..1) para que los crateres de alrededor no se
# vean repetidos.
MOON_FACE_HALF = (0.56, 0.64)


def _quantize(img, brightness=1.0, contrast=1.0):
    from PIL import ImageEnhance
    img = ImageEnhance.Contrast(ImageEnhance.Brightness(img.convert("RGB")).enhance(brightness)).enhance(contrast)
    out = img.quantize(colors=COLORS, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert("RGB")
    # los azules (los ojos del Sol) son tan pocos pixeles que la paleta los pierde: se dejan como estaban
    src, dst = img.load(), out.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b = src[x, y]
            if b > r + 25 and b > g + 10:
                dst[x, y] = (r, g, b)
    return out


def _remap(src, size, sample):
    """Arma una imagen size x size llamando sample(px, py) -> punto de src (en coordenadas -1..1 del disco),
    primero a 8x de resolucion y despues achicada (promedia bien los pixeles de la referencia)."""
    big = size * 8
    out = Image.new("RGB", (big, big))
    pix = out.load()
    for y in range(big):
        for x in range(big):
            px = (x + 0.5) / big * 2 - 1
            py = (y + 0.5) / big * 2 - 1
            sx, sy = sample(px, py)
            sx = min(max(sx, 0), src.width - 1)
            sy = min(max(sy, 0), src.height - 1)
            pix[x, y] = src.getpixel((int(sx), int(sy)))
    return out.resize((size, size), Image.BOX)


def sun_face_texture(size):
    """La cara del Sol mirando de frente: la mitad izquierda de la referencia espejada sobre el eje de la nariz, de la
    elipse a un circulo. Las esquinas fuera del circulo, color del anillo."""
    src = Image.open(REFS / "sol.jpg").convert("RGB")
    cx, cy, rx, ry = SUN_FACE
    img = _remap(src, size, lambda px, py: (cx - abs(px) * rx, cy + py * ry))
    img = _quantize(img, brightness=1.12, contrast=1.1)
    ring = (176, 120, 30)
    pix = img.load()
    for y in range(size):
        for x in range(size):
            if math.hypot((x + 0.5) / size * 2 - 1, (y + 0.5) / size * 2 - 1) > 1.0:
                pix[x, y] = ring
    return img


def moon_face_texture(size):
    """La Luna con la cara corrida al centro y mirando de frente (simetrica). Lo que cae afuera de la luna de la referencia se espeja contra el borde,
    asi se completa con sus propios crateres."""
    src = Image.open(REFS / "luna.jpg").convert("RGB")
    cx, cy, rx, ry = MOON_DISC
    fx, fy = MOON_FACE_CENTER
    radius = (rx + ry) / 2

    def sample(px, py):
        hx, hy = MOON_FACE_HALF
        if (px / hx) ** 2 + (py / hy) ** 2 <= 1.0:
            px = -abs(px)  # la cara, simetrica: mira de frente
        sx, sy = fx + px * radius, fy + py * radius
        qx, qy = (sx - cx) / rx, (sy - cy) / ry
        q = math.hypot(qx, qy)
        if q > 0.97:
            scale = max(0.0, (1.94 - q)) / q
            qx, qy = qx * scale, qy * scale
        return cx + qx * rx, cy + qy * ry
    img = _remap(src, size, sample)
    return _quantize(img, brightness=1.18, contrast=1.25)


# ------------------------------------------------------------------ geometria

def stepped_disc(radius, cy):
    """Franjas horizontales de un circulo escalonado de a STEP pixeles: [(x1, y1, x2, y2)], juntando las iguales."""
    rows = []
    for y0 in range(-radius, radius, STEP):
        mid = y0 + STEP / 2
        half = math.sqrt(max(0.0, radius * radius - mid * mid))
        half = max(STEP, int(round(half / STEP)) * STEP)
        if rows and rows[-1][0] == -half and rows[-1][2] == half and rows[-1][3] == cy + y0:
            rows[-1] = (-half, rows[-1][1], half, cy + y0 + STEP)
        else:
            rows.append((-half, cy + y0, half, cy + y0 + STEP))
    return rows


def front_crop(image, radius, cy, x1, y1, x2, y2):
    """El pedazo de la textura del disco (radio radius, centro (0, cy)) que le toca a la cara de adelante (norte) de
    un cuboide. Mirando la cara norte, la izquierda de la imagen es +X."""
    u0 = int(radius - x2)
    v0 = int(cy + radius - y2)
    return image.crop((u0, v0, u0 + int(x2 - x1), v0 + int(y2 - y1)))


def edge(front, side, w, h):
    """Costado de un cuboide pintado con el borde de su textura de adelante estirado: asi los escalones y las capas no
    dejan lineas de otro color atravesando la cara. La textura de adelante (norte) tiene +X a la izquierda."""
    fw, fh = front.size
    if side in ("east", "west"):
        column = front.crop((0, 0, 1, fh) if side == "east" else (fw - 1, 0, fw, fh))
        strip = column.resize((w, h), Image.NEAREST)
    else:
        row = front.crop((0, 0, fw, 1) if side == "up" else (0, fh - 1, fw, fh))
        strip = row.resize((w, h), Image.NEAREST)
    return Image.eval(strip.convert("RGB"), lambda c: int(c * 0.85)).convert("RGBA")


def mottled(w, h, base, spread, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            img.putpixel((x, y), (*gm.shade(base, rng.randint(-spread, spread)), 255))
    return img


# ------------------------------------------------------------------ Sol

SUN_NAME = "bigcasares_sol"
SUN_RADIUS = 24
SUN_CY = 48
CORONA_RADIUS = 28
RAYS = 16
# segmentos de cada rayo: (desde, hasta, medio ancho) en pixeles desde el centro; los cortos se achican
RAY_SEGMENTS = [(20, 28, 7), (28, 33, 6), (33, 37, 5), (37, 40, 4), (40, 43, 3), (43, 45, 2), (45, 46, 1)]
RAY_LENGTH = 46
RAY_SHORT = 0.9
RAY_PALETTE = [(184, 34, 20), (214, 64, 22), (236, 110, 26), (246, 156, 32), (250, 204, 44), (252, 234, 104)]


def ray_color(t, s):
    """t: 0 en la base, 1 en la punta. s: -1..1 de un costado al otro. Como en la referencia, cada rayo tiene un
    costado amarillo (al sol) y el otro rojo (a la sombra), y la punta se vuelve toda amarilla."""
    if s >= 0:
        value = 0.62 + 0.38 * t      # costado amarillo: de amarillo fuerte a amarillo claro
    else:
        value = 0.02 + 0.5 * t + 0.35 * max(0.0, t - 0.6) / 0.4  # costado rojo: rojo, naranja y amarillo en la punta
    return RAY_PALETTE[max(0, min(len(RAY_PALETTE) - 1, int(value * len(RAY_PALETTE))))]


def build_sun():
    # prefijo glow_ (etiqueta de BetterModel): el Sol brilla solo, sin la sombra de la luz del mundo
    groups = {
        "root": ([0, 0, 0], None),
        "glow_cuerpo": ([0, SUN_CY, 0], "root"),
        "rayos": ([0, SUN_CY, 0], "glow_cuerpo"),
    }
    for k in range(RAYS):
        groups[f"glow_rayo_{k:02d}"] = ([0, SUN_CY, 0], "rayos", (0, 0, k * 360 / RAYS))
    m = Model(SUN_NAME, groups, density=1, tex=256)
    m.out_dir = OUT_DIR
    face = caras.sun_face(2 * SUN_RADIUS)

    for i, (x1, y1, x2, y2) in enumerate(stepped_disc(SUN_RADIUS, SUN_CY)):
        m.cube(f"cara_{i}", "glow_cuerpo", [x1, y1, -2], [x2, y2, 2], ("cara", (x1, y1, x2, y2)))
    for i, (x1, y1, x2, y2) in enumerate(stepped_disc(CORONA_RADIUS, SUN_CY)):
        m.cube(f"corona_{i}", "glow_cuerpo", [x1, y1, 0.5], [x2, y2, 2.5], ("corona", None))
    for k in range(RAYS):
        scale = 1.0 if k % 2 == 0 else RAY_SHORT
        z1, z2 = (1.0, 4.0) if k % 2 == 0 else (2.0, 5.0)  # los cortos un poco mas atras
        for j, (r1, r2, half) in enumerate(RAY_SEGMENTS):
            a = 20 + (r1 - 20) * scale
            b = 20 + (r2 - 20) * scale
            m.cube(f"rayo_{k:02d}_{j}", f"glow_rayo_{k:02d}", [-half, SUN_CY + a, z1], [half, SUN_CY + b, z2],
                   ("rayo", (a, b, half, 20 + (RAY_LENGTH - 20) * scale)))

    def paint(el, side, w, h, seed):
        kind, info = el["part"]
        if kind == "cara":
            front = front_crop(face, SUN_RADIUS, SUN_CY, *info)
            if side == "north":
                return front.convert("RGBA")
            if side == "south":
                return mottled(w, h, (214, 150, 34), 8, seed)
            return edge(front, side, w, h)
        if kind == "corona":
            return mottled(w, h, (204, 92, 22), 4, seed)
        # rayo: del rojo de la base al amarillo de la punta, con un costado a la sombra
        a, b, half, length = info
        img = Image.new("RGBA", (w, h))
        for y in range(h):
            for x in range(w):
                if side in ("north", "south"):
                    r = b - (y + 0.5) * (b - a) / h
                    s = ((x + 0.5) / w * 2 - 1) * (1 if side == "north" else -1)
                elif side in ("east", "west"):
                    r = b - (y + 0.5) * (b - a) / h
                    s = 0.9 if side == "west" else -0.9
                else:
                    r = b if side == "up" else a
                    s = (x + 0.5) / w * 2 - 1
                t = (r - 20) / (length - 20)
                img.putpixel((x, y), (*ray_color(t, s), 255))
        return img

    anims = {
        # los rayos giran despacio (45 grados es un largo + un corto: vuelve igual) y el sol flota apenas
        "idle": (8.0, "linear", {
            "rayos": [("rotation", 0, (0, 0, 0)), ("rotation", 8.0, (0, 0, 45))],
            "glow_cuerpo": [("position", 0, (0, 0, 0)), ("position", 4.0, (0, 1.5, 0)), ("position", 8.0, (0, 0, 0))],
        }),
    }
    m.build(paint, anims)
    return face


# ------------------------------------------------------------------ Luna

MOON_NAME = "bigcasares_luna"
MOON_RADIUS = 28
MOON_CY = 32
# (radio, z de adelante): el disco grueso y dos placas mas chicas adelante que la abomban
MOON_LAYERS = [(28, -2), (20, -3), (12, -4)]


def build_moon():
    groups = {"root": ([0, 0, 0], None), "cuerpo": ([0, MOON_CY, 0], "root")}
    m = Model(MOON_NAME, groups, density=1, tex=256)
    m.out_dir = OUT_DIR
    face = caras.moon_face(2 * MOON_RADIUS)
    back_texture = caras.moon_face(2 * MOON_RADIUS, seed=11, with_face=False)
    for layer, (radius, z) in enumerate(MOON_LAYERS):
        back = 4 if layer == 0 else z + 1
        for i, (x1, y1, x2, y2) in enumerate(stepped_disc(radius, MOON_CY)):
            m.cube(f"luna_{layer}_{i}", "cuerpo", [x1, y1, z], [x2, y2, back], ("luna", (x1, y1, x2, y2)))

    def paint(el, side, w, h, seed):
        info = el["part"][1]
        if side == "north":
            return front_crop(face, MOON_RADIUS, MOON_CY, *info).convert("RGBA")
        front = front_crop(face, MOON_RADIUS, MOON_CY, *info)
        if side == "south":
            # atras: solo crateres (otra textura sin cara)
            return front_crop(back_texture, MOON_RADIUS, MOON_CY, *info).transpose(Image.FLIP_LEFT_RIGHT).convert("RGBA")
        return edge(front, side, w, h)

    anims = {
        "idle": (6.0, "linear", {
            "cuerpo": [("position", 0, (0, 0, 0)), ("position", 3.0, (0, 1.5, 0)), ("position", 6.0, (0, 0, 0)),
                       ("rotation", 0, (0, 0, -2)), ("rotation", 3.0, (0, 0, 2)), ("rotation", 6.0, (0, 0, -2))],
        }),
    }
    m.build(paint, anims)
    return face


if __name__ == "__main__":
    build_sun()
    build_moon()

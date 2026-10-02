#!/usr/bin/env python3
"""Generate 16x16 BigCasares item textures (valid RGB PNGs readable by Java ImageIO)."""
import struct, zlib, math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "resourcepack"
SIZE = 16


def rgb(hexstr):
    hexstr = hexstr.lstrip('#')
    return tuple(int(hexstr[i:i + 2], 16) for i in (0, 2, 4))


IRON      = rgb("9AA0A8"); IRON_D    = rgb("565B63")
COPPER    = rgb("C2703D"); COPPER_D  = rgb("8F4E26")
GOLD      = rgb("F6D24A"); GOLD_D    = rgb("C99A1F")
DIAMOND   = rgb("63D8E2"); DIAMOND_D = rgb("2E9CB0")
EMERALD   = rgb("4CE05A"); EMERALD_D = rgb("1F9E3C")
NETHERITE = rgb("44405A"); NETHERITE_D = rgb("26233A")
PURPLE    = rgb("B24CD8"); PURPLE_D  = rgb("6E2A8C")
STAR      = rgb("FFF2A0")
PAPER     = rgb("C9A26B"); PAPER_D   = rgb("8F6B3E")
POWDER    = rgb("EFEFEA"); POWDER_D  = rgb("B9B9B0")
GLASS     = rgb("DCEFF2"); GLASS_D   = rgb("9FBFC6")
ACID_G    = rgb("A8E63B"); ACID_D    = rgb("5C9C1E")
HAZARD_Y  = rgb("F2C14E"); HAZARD_D  = rgb("A8791F")
HAZARD_B  = rgb("2B2B2B")
TRANSP    = None


def canvas():
    return [[TRANSP for _ in range(SIZE)] for _ in range(SIZE)]


def put(img, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE and color is not None:
        img[y][x] = color


def rect(img, x0, y0, x1, y1, color, outline=None):
    for y in range(max(0, y0), min(SIZE, y1 + 1)):
        for x in range(max(0, x0), min(SIZE, x1 + 1)):
            edge = x == x0 or x == x1 or y == y0 or y == y1
            put(img, x, y, outline if edge and outline else color)


def ring(img, cx, cy, r, color, dark=None, thickness=1.1, gap_angles=None):
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            if abs(d - r) > thickness:
                continue
            ang = math.degrees(math.atan2(y - cy, x - cx)) % 360
            if gap_angles:
                skip = False
                for (a0, a1) in gap_angles:
                    if a0 <= ang <= a1 or (a0 > a1 and (ang >= a0 or ang <= a1)):
                        skip = True
                        break
                if skip:
                    continue
            put(img, x, y, dark if dark and (abs(d - r) > thickness - 0.55) else color)


def line(img, x0, y0, x1, y1, color, width=1):
    steps = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(steps + 1):
        t = i / steps
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        for dx in range(-(width // 2), width - width // 2 + 1):
            for dy in range(-(width // 2), width - width // 2 + 1):
                put(img, round(x) + dx, round(y) + dy, color)


def hook(ring_color, dark, shaft_color, grip_color, tip_color=None, glow=None, star=None):
    img = canvas()
    if glow:
        ring(img, 8.0, 7.0, 5.0, glow, thickness=1.8)
    ring(img, 8.0, 7.0, 3.6, ring_color, dark, thickness=1.05, gap_angles=[(-35, 45)])
    line(img, 8, 3, 8, 12, shaft_color, width=2)
    line(img, 8, 3, 8, 12, dark, width=1)
    ring(img, 8.0, 1.4, 1.2, dark, thickness=0.8)
    rect(img, 5, 12, 11, 14, grip_color, outline=dark)
    if tip_color:
        line(img, 11, 11, 14, 14, tip_color, width=2)
        line(img, 11, 11, 14, 14, dark, width=1)
        line(img, 13, 15, 14, 15, tip_color, width=1)
    if star:
        rect(img, 6, 6, 10, 10, STAR, outline=None)
        rect(img, 7, 5, 9, 11, STAR, outline=None)
        rect(img, 5, 7, 11, 9, STAR, outline=None)
    return img


HOOKS = {
    "grappling_hook_1": hook(IRON, IRON_D, IRON, IRON_D),
    "grappling_hook_2": hook(IRON, IRON_D, IRON, COPPER, tip_color=COPPER),
    "grappling_hook_3": hook(GOLD, GOLD_D, IRON, IRON_D, tip_color=DIAMOND),
    "grappling_hook_4": hook(GOLD, GOLD_D, GOLD, EMERALD, tip_color=DIAMOND),
    "grappling_hook_5": hook(NETHERITE, NETHERITE_D, NETHERITE, NETHERITE_D,
                             tip_color=PURPLE, glow=PURPLE_D),
    "grappling_hook_6": hook(NETHERITE, NETHERITE_D, NETHERITE, NETHERITE_D,
                             tip_color=STAR, glow=PURPLE, star=STAR),
}


def nitrate():
    img = canvas()
    rect(img, 3, 9, 12, 14, PAPER, outline=PAPER_D)
    for y in range(6, 12):
        for x in range(6, 12):
            d = abs(x - 8.5) + abs(y - 8.5)
            if 1.0 <= d <= 4.0:
                img[y][x] = HAZARD_B
            elif d < 1.0:
                img[y][x] = HAZARD_Y
    rect(img, 6, 6, 11, 11, HAZARD_Y, outline=HAZARD_B)
    for y in range(3, 9):
        for x in range(4, 12):
            if (x - 4) <= (y - 3) * 1.1 <= (x - 8) * -1 and (x - 4) <= (12 - x) * 0.9:
                img[y][x] = POWDER if (x + y) % 2 == 0 else POWDER_D
    for (x, y) in [(5, 3), (9, 2), (7, 4), (11, 3), (6, 5)]:
        img[y][x] = POWDER
    return img


def acid_flask():
    img = canvas()
    ring(img, 7.0, 8.5, 3.4, GLASS, GLASS_D, thickness=1.0, gap_angles=[(40, 320)])
    rect(img, 4, 6, 10, 11, GLASS_D, outline=None)
    for y in range(7, 12):
        for x in range(3, 12):
            d = math.hypot(x - 7.0, y - 8.5)
            if d <= 3.0 and not (x == 3 and y > 9):
                img[y][x] = ACID_G if (x + y) % 2 == 0 else ACID_D
    rect(img, 6, 1, 8, 5, GLASS, outline=GLASS_D)
    rect(img, 5, 0, 9, 1, COPPER, outline=COPPER_D)
    for y in range(7, 11):
        for x in range(5, 10):
            d = abs(x - 7.5) + abs(y - 8.5)
            if 0.5 <= d <= 3.0:
                img[y][x] = HAZARD_B if (x + y) % 2 == 0 else HAZARD_D
    for (x, y) in [(6, 9), (8, 10), (9, 8)]:
        img[y][x] = GLASS
    return img


def write_png(path, img):
    raw = b"".join(b"\x00" + bytes(v for c in row for v in (c if c is not None else (0, 0, 0))) for row in img)

    def chunk(typ, data):
        c = struct.pack(">I", len(data)) + typ + data
        return c + struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def main():
    generated = {f"{name}.png": img for name, img in HOOKS.items()}
    generated["potassium_nitrate.png"] = nitrate()
    generated["nitric_acid.png"] = acid_flask()
    for filename, img in generated.items():
        # Canonical: the SharedResourcePackAssembler copies shared/textures into
        # both assembled roots at build time. Also write to the real source
        # trees so they stay self-consistent when inspected directly.
        dests = (
            ROOT / "shared" / "textures/item" / filename,
            ROOT / "java/assets/bigcasares/textures/item" / filename,
            ROOT / "bedrock" / "textures/item" / filename,
        )
        for dest in dests:
            write_png(dest, img)
            print("wrote", dest)


if __name__ == "__main__":
    main()

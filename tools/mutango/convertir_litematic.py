"""Convierte un .litematic (Litematica) al formato de estructura que usan los plugins (como la ciudad de Don Pollos).

Salida: {"size": [ancho, alto, largo], "palette": ["minecraft:...[props]", ...], "runs": [[indice, cantidad], ...]}
en un .json.gz. Orden de los bloques: y, luego z, luego x (el mismo que el litematic). Incluye el aire, asi al pegarla
se limpia lo que haya adentro. Usa solo la primera region del litematic.

Uso: python tools/convertir_litematic.py [entrada.litematic] [salida.json.gz]
     (por defecto: tools/mutango/fuentes -> src/main/resources/mutango/structures/ruinas.json.gz)
"""
import gzip
import json
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import nbt  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).resolve().parent / "fuentes" / "Ruined World Spawn.litematic"
TARGET = ROOT / "src" / "main" / "resources" / "mutango" / "structures" / "ruinas.json.gz"


def state_string(entry):
    """Paleta del litematic -> "minecraft:bloque[prop=valor,...]" (propiedades ordenadas)."""
    props = entry.get("Properties")
    if not props:
        return entry["Name"]
    return entry["Name"] + "[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]"


def convert(source, target):
    _, root = nbt.read(source)
    name, region = next(iter(root["Regions"].items()))
    size = region["Size"]
    w, h, l = abs(size["x"]), abs(size["y"]), abs(size["z"])
    palette_in = [state_string(p) for p in region["BlockStatePalette"]]
    states = region["BlockStates"]
    # cada bloque ocupa `bits` bits, empaquetados seguidos en el long array (pueden cruzar de un long al otro)
    bits = max(2, math.ceil(math.log2(len(palette_in))))
    mask = (1 << bits) - 1

    def value(i):
        start = i * bits
        li, off = divmod(start, 64)
        v = (states[li] & 0xFFFFFFFFFFFFFFFF) >> off
        if off + bits > 64:
            v |= (states[li + 1] & 0xFFFFFFFFFFFFFFFF) << (64 - off)
        return v & mask

    palette, index, runs = [], {}, []
    for i in range(w * h * l):
        state = palette_in[value(i)]
        if state not in index:
            index[state] = len(palette)
            palette.append(state)
        k = index[state]
        if runs and runs[-1][0] == k:
            runs[-1][1] += 1
        else:
            runs.append([k, 1])
    target.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps({"size": [w, h, l], "palette": palette, "runs": runs}, separators=(",", ":"))
    target.write_bytes(gzip.compress(payload.encode("utf-8")))
    print(f"{name} -> {target.name}: {w}x{h}x{l}, {len(palette)} estados, {len(runs)} tramos, "
          f"{target.stat().st_size} bytes")


if __name__ == "__main__":
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else SOURCE
    dst = Path(sys.argv[2]) if len(sys.argv) > 2 else TARGET
    convert(src, dst)

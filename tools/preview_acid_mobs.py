#!/usr/bin/env python3
"""Render a textured orthographic asset-review sheet, not a Minecraft screenshot."""
import argparse
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / "src/main/resources/bettermodel/models"
FACES = {
    "north": (0, 1, 2, 3), "south": (5, 4, 7, 6),
    "east": (1, 5, 6, 2), "west": (4, 0, 3, 7),
    "up": (4, 5, 1, 0), "down": (3, 2, 6, 7),
}


def rotate(point, angles, origin):
    x, y, z = [point[i] - origin[i] for i in range(3)]
    rx, ry, rz = map(math.radians, angles)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    return [x + origin[0], y + origin[1], z + origin[2]]


def pose(model, animation_name, time):
    result = {}
    for animation in model["animations"]:
        if animation["name"] != animation_name:
            continue
        time = time % animation["length"] if animation["loop"] == "loop" else min(time, animation["length"])
        for bone, animator in animation["animators"].items():
            result[bone] = {}
            for channel in ("rotation", "position"):
                frames = sorted((f for f in animator["keyframes"] if f["channel"] == channel), key=lambda f: f["time"])
                if not frames:
                    continue
                low = max((f for f in frames if f["time"] <= time), key=lambda f: f["time"], default=frames[0])
                high = min((f for f in frames if f["time"] >= time), key=lambda f: f["time"], default=frames[-1])
                fraction = 0 if high["time"] == low["time"] else (time - low["time"]) / (high["time"] - low["time"])
                a, b = low["data_points"][0], high["data_points"][0]
                result[bone][channel] = [float(a[k]) * (1-fraction) + float(b[k]) * fraction for k in "xyz"]
    return result


def render(name, animation, time, view="threequarter"):
    model = json.loads((MODELS / (name + ".bbmodel")).read_text(encoding="utf-8"))
    texture = Image.open(MODELS / (name + ".png")).convert("RGBA")
    groups = {g["uuid"]: g for g in model["groups"]}
    parents = {child: g["uuid"] for g in groups.values() for child in g["children"]}
    poses = pose(model, animation, time)
    yaw, pitch = map(math.radians, {
        "threequarter": (-22,16), "front": (0,0), "back": (180,0),
        "left": (-90,0), "right": (90,0), "top": (0,70), "bottom": (0,-60)
    }[view])
    camera = [-math.sin(yaw)*math.cos(pitch), math.sin(pitch), -math.cos(yaw)*math.cos(pitch)]

    def project(p):
        x, y, z = p
        depth = -math.sin(yaw)*x + math.cos(yaw)*z
        return [math.cos(yaw)*x + math.sin(yaw)*z,
                math.cos(pitch)*y + math.sin(pitch)*depth,
                math.cos(pitch)*depth - math.sin(pitch)*y]

    quads = []
    for cube in model["elements"]:
        x0,y0,z0 = cube["from"]
        x1,y1,z1 = cube["to"]
        points = [[x0,y1,z0], [x1,y1,z0], [x1,y0,z0], [x0,y0,z0],
                  [x0,y1,z1], [x1,y1,z1], [x1,y0,z1], [x0,y0,z1]]
        points = [rotate(p, cube.get("rotation", [0,0,0]), cube["origin"]) for p in points]
        parent = parents.get(cube["uuid"])
        while parent:
            group = groups[parent]
            current = poses.get(parent, {})
            angles = [group["rotation"][i] + current.get("rotation", [0,0,0])[i] for i in range(3)]
            points = [rotate(p, angles, group["origin"]) for p in points]
            delta = current.get("position", [0,0,0])
            points = [[p[i] + delta[i] for i in range(3)] for p in points]
            parent = parents.get(parent)
        for face, indices in FACES.items():
            vertices = [points[i] for i in indices]
            a, b = [[vertices[n][i]-vertices[0][i] for i in range(3)] for n in (1,3)]
            normal = [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]
            length = math.sqrt(sum(v*v for v in normal))
            normal = [v/length for v in normal]
            if sum(normal[i]*camera[i] for i in range(3)) <= 0:
                continue
            projected = [project(p) for p in vertices]
            shade = 0.72 + 0.28*max(0, sum(normal[i]*(-0.3,0.7,-0.65)[i] for i in range(3)))
            quads.append((sum(p[2] for p in projected)/4, projected, cube["faces"][face]["uv"], shade))
    panel = Image.new("RGBA", (420,500), (30,34,38,255))
    scale, cx, ground = 10.5, 210, 445
    ImageDraw.Draw(panel).ellipse((cx-145,ground-14,cx+145,ground+14), fill=(21,24,27))
    for _, points, uv, shade in sorted(quads, reverse=True, key=lambda q: q[0]):
        quad = [(cx+p[0]*scale, ground-p[1]*scale) for p in points]
        p, u, v = quad[0], (quad[1][0]-quad[0][0], quad[1][1]-quad[0][1]), (quad[3][0]-quad[0][0], quad[3][1]-quad[0][1])
        determinant = u[0]*v[1]-u[1]*v[0]
        if abs(determinant) < 1e-7:
            continue
        width, height = uv[2]-uv[0], uv[3]-uv[1]
        a,b = width*v[1]/determinant, -width*v[0]/determinant
        d,e = -height*u[1]/determinant, height*u[0]/determinant
        transform = (a,b,uv[0]-a*p[0]-b*p[1],d,e,uv[1]-d*p[0]-e*p[1])
        layer = texture.transform(panel.size, Image.Transform.AFFINE, transform, resample=Image.Resampling.NEAREST)
        layer = ImageEnhance.Brightness(layer).enhance(shade)
        mask = Image.new("L", panel.size)
        ImageDraw.Draw(mask).polygon(quad, fill=255)
        panel.paste(layer, (0,0), mask)
    label = {"toxic_mob":"TOXIC CRAWLER", "toxic_brute":"ACID BRUTE", "toxic_spitter":"CHEMICAL SPITTER"}[name]
    draw = ImageDraw.Draw(panel)
    draw.text((25,25), label, fill=(225,230,220))
    draw.text((25,45), "MODEL ASSET PREVIEW / " + animation.upper(), fill=(142,157,162))
    return panel


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--animation", default="idle")
    parser.add_argument("--time", type=float, default=0)
    parser.add_argument("--view", choices=("threequarter","front","back","left","right","top","bottom"), default="threequarter")
    args = parser.parse_args()
    output = ROOT / "build/visual-previews"
    output.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGBA", (1260,500))
    for index, name in enumerate(("toxic_mob", "toxic_brute", "toxic_spitter")):
        sheet.paste(render(name,args.animation,args.time,args.view),(index*420,0))
    suffix = "" if args.view == "threequarter" else "-" + args.view
    path = output / ("acid-mobs-" + args.animation + suffix + ".png")
    sheet.save(path)
    print(path)

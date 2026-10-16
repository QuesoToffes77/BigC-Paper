"""Lo comun que necesitan los generadores de modelos de Mutango (generar_mutango.py, generar_neguechen.py,
generar_sol_luna.py, generar_bloque_ruinas.py): ayudas de color, nombres estables, tamanos de cara, claves de
animacion y la clase Model, que arma el atlas de la textura y escribe el .bbmodel.

Sale de los modelos de Don Pollos (generar_modelos.py y generar_wsp.py), de donde se saco solo lo que usa Mutango.
Ojo: stable_uuid usa el prefijo "donpollos/" a proposito: de ahi salen los uuid que ya tienen los .bbmodel de esta
carpeta; si se cambia, todos los uuid cambian.
"""
import base64
import io
import json
import uuid as uuidlib
from pathlib import Path

from PIL import Image

REPO = Path(__file__).resolve().parents[2]  # la raiz de BigC
ROOT = REPO
MODELS_DIR = REPO / "src" / "main" / "resources" / "bettermodel" / "models"
SOURCES = Path(__file__).resolve().parent / "fuentes"
OUT_PREVIEWS = ROOT / "build" / "model-previews"
CLEAR = (0, 0, 0, 0)
FACES = ("north", "east", "south", "west", "up", "down")


def stable_uuid(*parts):
    return str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, "donpollos/" + "/".join(parts)))


def shade(color, amount):
    return tuple(max(0, min(255, c + amount)) for c in color[:3])


def face_size(el, face):
    (x1, y1, z1), (x2, y2, z2) = el["from"], el["to"]
    w, h, d = abs(x2 - x1), abs(y2 - y1), abs(z2 - z1)
    return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h),
            "up": (w, d), "down": (w, d)}[face]


def keyframe(channel, t, xyz, model, anim, bone, idx, interpolation):
    return {"channel": channel, "data_points": [{"x": str(xyz[0]), "y": str(xyz[1]), "z": str(xyz[2])}],
            "uuid": stable_uuid(model, anim, bone, channel, str(idx)), "time": t, "color": -1,
            "interpolation": interpolation}


class Model:
    def __init__(self, name, groups, density, tex):
        self.name, self.groups, self.density, self.tex = name, groups, density, tex
        self.els = []
        self.out_dir = None

    def cube(self, name, group, frm, to, part):
        self.els.append({"name": name, "group": group, "from": frm, "to": to, "part": part})

    def build(self, paint, anims):
        dens = self.density
        rects = []
        for i, el in enumerate(self.els):
            for f in FACES:
                w, h = face_size(el, f)
                rects.append(((i, f), max(1, round(w * dens)), max(1, round(h * dens))))
        layout, x, y, shelf = {}, 0, 0, 0
        for rid, w, h in sorted(rects, key=lambda r: (-r[2], -r[1])):
            if x + w > self.tex:
                x, y, shelf = 0, y + shelf, 0
            if y + h > self.tex:
                raise RuntimeError(f"la textura de {self.name} no entra en {self.tex}x{self.tex}")
            layout[rid] = (x, y)
            x += w
            shelf = max(shelf, h)
        atlas = Image.new("RGBA", (self.tex, self.tex), CLEAR)
        sizes = {rid: (w, h) for rid, w, h in rects}
        for (i, f), (w, h) in sizes.items():
            atlas.paste(paint(self.els[i], f, w, h, f"{self.name}/{self.els[i]['name']}/{f}"), layout[(i, f)])
        buf = io.BytesIO()
        atlas.save(buf, "PNG")

        guid = {g: stable_uuid(self.name, "group", g) for g in self.groups}
        elements, children = [], {g: [] for g in self.groups}
        for i, el in enumerate(self.els):
            euid = stable_uuid(self.name, "el", el["name"])
            faces = {}
            for f in FACES:
                fx, fy = layout[(i, f)]
                w, h = sizes[(i, f)]
                faces[f] = {"uv": [fx, fy, fx + w, fy + h], "texture": 0}
            elements.append({"name": el["name"], "type": "cube", "uuid": euid, "box_uv": False, "rescale": False,
                             "locked": False, "render_order": "default", "allow_mirror_modeling": True,
                             "from": el["from"], "to": el["to"], "autouv": 0, "color": 0, "inflate": 0,
                             "origin": self.groups[el["group"]][0], "rotation": [0, 0, 0], "faces": faces,
                             "export": True, "visibility": True, "shade": True, "mirror_uv": False,
                             "light_emission": 0})
            children[el["group"]].append(euid)
        # cada grupo es (pivote, padre) o (pivote, padre, rotacion fija de reposo)
        groups = [{"name": g, "uuid": guid[g], "export": True, "locked": False, "scope": 0, "origin": v[0],
                   "rotation": list(v[2]) if len(v) > 2 else [0, 0, 0], "color": 0, "reset": False, "shade": True,
                   "mirror_uv": False, "visibility": True, "autouv": 0, "isOpen": True}
                  for g, v in self.groups.items()]

        def tree(g):
            kids = list(children[g])
            for child, v in self.groups.items():
                if v[1] == g:
                    kids.append(tree(child))
            return {"uuid": guid[g], "isOpen": True, "children": kids}

        out_anims = []
        for name, (length, interp, bones) in anims.items():
            animators = {}
            for bone, frames in bones.items():
                kfs = [keyframe(channel, t, xyz, self.name, name, bone, i, interp)
                       for i, (channel, t, xyz) in enumerate(frames)]
                animators[guid[bone]] = {"name": bone, "type": "bone", "rotation_global": False,
                                         "quaternion_interpolation": False, "keyframes": kfs}
            out_anims.append({"uuid": stable_uuid(self.name, "anim", name), "name": name, "loop": "loop",
                              "override": False, "length": length, "snapping": 24, "selected": False,
                              "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "",
                              "animators": animators})

        data = {
            "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
            "name": self.name, "model_identifier": "", "visible_box": [2, 2, 0],
            "variable_placeholders": "", "variable_placeholder_buttons": [], "timeline_setups": [],
            "unhandled_root_fields": {}, "resolution": {"width": self.tex, "height": self.tex},
            "elements": elements, "groups": groups, "outliner": [tree("root")],
            "textures": [{"path": "", "name": f"{self.name}.png", "folder": "", "namespace": "", "id": "0",
                          "group": "", "width": self.tex, "height": self.tex, "uv_width": self.tex,
                          "uv_height": self.tex, "particle": False, "use_as_default": False,
                          "layers_enabled": False, "sync_to_project": "", "render_mode": "default",
                          "render_sides": "auto", "pbr_channel": "color", "frame_time": 1,
                          "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False,
                          "visible": True, "internal": True, "saved": True,
                          "uuid": stable_uuid(self.name, "texture"), "relative_path": f"{self.name}.png",
                          "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()}],
            "animations": out_anims,
        }
        out = (self.out_dir or MODELS_DIR) / f"{self.name}.bbmodel"
        out.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
        OUT_PREVIEWS.mkdir(parents=True, exist_ok=True)
        atlas.resize((self.tex * 2, self.tex * 2), Image.NEAREST).save(OUT_PREVIEWS / f"{self.name}_textura.png")
        print("ok", out.name, len(elements), "elementos")
        return data, atlas

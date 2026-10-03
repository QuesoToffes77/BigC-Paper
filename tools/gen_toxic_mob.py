#!/usr/bin/env python3
"""Deterministically assemble three UV-mapped, articulated Acid Rain models.

The material atlas is authored separately. This script generates model data,
copies the existing PNG unchanged, and never procedurally paints textures.
"""
import base64
import json
import math
from pathlib import Path
import shutil
import uuid

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/bettermodel/models"
ATLAS = ROOT / "resourcepack/sources/acid_mob_melted_materials.png"
NAMESPACE = uuid.UUID("5f2b1111-0000-4000-8000-000000000010")
MATERIALS = dict(zip(
    ("skin", "chitin", "bone", "shirt", "copper", "leather", "acid", "sinew",
     "eye", "tooth", "mouth", "iron", "scale", "pants", "crust", "rust"), range(16)))


class Model:
    def __init__(self, name, body_name, origin):
        self.name, self.body_name = name, body_name
        self.elements, self.groups, self.animations = [], {}, []
        self.bone("Root", None, [0, 0, 0])
        self.bone(body_name, "Root", origin)

    def uid(self, label):
        return str(uuid.uuid5(NAMESPACE, self.name + "/" + label))

    def bone(self, name, parent, origin):
        if name in self.groups:
            raise ValueError("Duplicate bone: " + name)
        self.groups[name] = {"name": name, "uuid": self.uid("bone/" + name),
            "export": True, "origin": origin, "rotation": [0, 0, 0],
            "children": [], "visibility": True, "shade": True, "mirror_uv": False}
        if parent:
            self.groups[parent]["children"].append(self.groups[name]["uuid"])

    def cube(self, name, start, end, material, bone, rotation=None, origin=None):
        dx, dy, dz = [end[i] - start[i] for i in range(3)]
        if min(dx, dy, dz) <= 0:
            raise ValueError("Degenerate cube: " + name)
        tile = MATERIALS[material]
        u, v = (tile % 4) * 32, (tile // 4) * 32
        faces = {}
        for face, (width, height) in {"north": (dx, dy), "south": (dx, dy),
            "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}.items():
            width, height = min(28, width * 2), min(28, height * 2)
            uv = [u + 1, v + 1, u + 31, v + 31] if material == "eye" else [
                u + 16 - width / 2, v + 16 - height / 2, u + 16 + width / 2, v + 16 + height / 2]
            faces[face] = {"uv": uv, "texture": 0}
        identifier = self.uid("cube/" + name)
        cube = {"name": name, "type": "cube", "uuid": identifier,
            "from": start, "to": end, "origin": origin or [(start[i] + end[i]) / 2 for i in range(3)],
            "faces": faces, "box_uv": False, "export": True,
            "autouv": 0, "render_order": "default", "color": 0}
        if rotation:
            cube["rotation"] = rotation
        self.elements.append(cube)
        self.groups[bone]["children"].append(identifier)

    def rod(self, name, start, end, width, material, bone):
        delta = [end[i] - start[i] for i in range(3)]
        length = math.sqrt(sum(value * value for value in delta))
        center = [(start[i] + end[i]) / 2 for i in range(3)]
        # BetterModel uses Rz * Ry * Rx; rotate local Y to the exact endpoint.
        x = math.degrees(math.atan2(delta[2], math.hypot(delta[0], delta[1])))
        z = math.degrees(math.atan2(-delta[0], delta[1]))
        self.cube(name, [center[0] - width / 2, center[1] - length / 2, center[2] - width / 2],
            [center[0] + width / 2, center[1] + length / 2, center[2] + width / 2],
            material, bone, [x, 0, z], center)

    def animation(self, name, length, loop, tracks):
        animators = {}
        for bone, channels in tracks.items():
            frames = []
            for channel, values in channels.items():
                for index, (time, xyz) in enumerate(values):
                    frames.append({"channel": channel, "time": time,
                        "data_points": [dict(zip("xyz", (str(value) for value in xyz)))],
                        "uuid": self.uid(f"frame/{name}/{bone}/{channel}/{index}"),
                        "color": -1, "interpolation": "linear"})
            animators[self.groups[bone]["uuid"]] = {"name": bone, "type": "bone",
                "rotation_global": False, "quaternion_interpolation": False, "keyframes": frames}
        self.animations.append({"name": name, "uuid": self.uid("animation/" + name),
            "length": length, "loop": loop, "override": False, "snapping": 20, "animators": animators})

    def save(self):
        texture = ATLAS.read_bytes()
        if int.from_bytes(texture[16:20], "big") != 128 or int.from_bytes(texture[20:24], "big") != 128:
            raise ValueError("Expected a normalized 128x128 PNG atlas")
        ids = {group["uuid"]: name for name, group in self.groups.items()}

        def tree(name):
            group = self.groups[name]
            return {"uuid": group["uuid"], "isOpen": True,
                "children": [tree(ids[child]) if child in ids else child for child in group["children"]]}

        model = {"meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
            "name": self.name, "model_identifier": self.name, "visible_box": [3, 3, 0],
            "resolution": {"width": 128, "height": 128}, "elements": self.elements,
            "groups": list(self.groups.values()), "outliner": [tree("Root")], "animations": self.animations,
            "textures": [{"name": self.name + ".png", "relative_path": self.name + ".png",
                "id": "0", "uuid": self.uid("texture"), "width": 128, "height": 128,
                "uv_width": 128, "uv_height": 128, "internal": True, "saved": True,
                "frame_time": 1, "frame_interpolate": False,
                "visible": True, "render_mode": "default", "render_sides": "front",
                "source": "data:image/png;base64," + base64.b64encode(texture).decode("ascii")} ]}
        OUTPUT.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ATLAS, OUTPUT / (self.name + ".png"))
        (OUTPUT / (self.name + ".bbmodel")).write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
        print(f"{self.name}: {len(self.elements)} cubes, {len(self.groups)} bones, 4 animations")


def loop_axis(axis, amount, duration, inverted=False):
    values = []
    for time, sign in ((0, 1), (duration / 2, -1), (duration, 1)):
        xyz = [0, 0, 0]
        xyz[axis] = amount * sign * (-1 if inverted else 1)
        values.append((time, xyz))
    return values


def humanoid_animations(m, skeleton=False):
    body = m.body_name
    m.animation("idle", 2.4, "loop", {
        body: {"position": [(0, [0, 0, 0]), (1.2, [0, 0.18, 0]), (2.4, [0, 0, 0])]},
        "Head": {"rotation": loop_axis(1, 2, 2.4)},
        "Jaw": {"rotation": [(0, [0, 0, 0]), (1.2, [-3, 0, 0]), (2.4, [0, 0, 0])]}})
    d = 0.9 if skeleton else 1.1
    m.animation("walk", d, "loop", {
        body: {"position": [(0, [0, 0, 0]), (d/4, [0, 0.28, 0]), (d/2, [0, 0, 0]),
                            (3*d/4, [0, 0.28, 0]), (d, [0, 0, 0])]},
        "LegLeft": {"rotation": loop_axis(0, 24, d)}, "LegRight": {"rotation": loop_axis(0, 24, d, True)},
        "ArmLeft": {"rotation": loop_axis(0, 12 if skeleton else 18, d, True)},
        "ArmRight": {"rotation": loop_axis(0, 8 if skeleton else 18, d)}})
    m.animation("attack", 0.65, "once", {
        body: {"rotation": [(0, [0, 0, 0]), (0.2, [3 if skeleton else 5, -8, 0]),
                            (0.4, [4 if skeleton else 12, 4, 0]), (0.65, [0, 0, 0])]},
        "ArmLeft": {"rotation": [(0, [0, 0, 0]), (0.2, [65, 0, 32] if skeleton else [68, 0, -8]),
                                  (0.4, [65, 0, 10] if skeleton else [80, 0, 4]), (0.65, [0, 0, 0])]},
        "ArmRight": {"rotation": [(0, [0, 0, 0]), (0.2, [85, 0, -5] if skeleton else [72, 0, 8]),
                                   (0.4, [85, 0, -5] if skeleton else [84, 0, -4]), (0.65, [0, 0, 0])]},
        "Jaw": {"rotation": [(0, [0, 0, 0]), (0.2, [-18, 0, 0]), (0.4, [-7, 0, 0]), (0.65, [0, 0, 0])]},
        **({"AcidBow": {"rotation": [(0, [0,0,0]), (0.2, [-85,0,0]),
                                      (0.4, [-85,0,0]), (0.65, [0,0,0])]}} if skeleton else {})})
    m.animation("hurt", 0.35, "once", {
        body: {"rotation": [(0, [0, 0, 0]), (0.1, [-8, 0, -4]), (0.22, [3, 0, 2]), (0.35, [0, 0, 0])]},
        "Head": {"rotation": [(0, [0, 0, 0]), (0.1, [-12, 0, 3]), (0.35, [0, 0, 0])]}})


def acid_drip(m, label, point, length, bone):
    x, y, z = point
    width = 0.7 if isinstance(label, str) else 0.85
    if not isinstance(label, str):
        m.cube(f"AcidClot{label}", [x-0.85,y-0.25,z-0.85],
            [x+0.85,y+0.8,z+0.85], "crust", bone)
    m.cube(f"AcidDrip{label}", [x-width/2,y-length,z-width/2],
        [x+width/2,y+0.4,z+width/2], "acid", bone)
    m.cube(f"AcidDropTip{label}", [x-width*0.7,y-length-0.35,z-width*0.7],
        [x+width*0.7,y-length+0.4,z+width*0.7], "acid", bone)


def brute():
    m = Model("toxic_brute", "ToxicBrute", [0, 12, 0])
    for name, parent, origin in (("Torso", "ToxicBrute", [0,13,0]), ("Head", "Torso", [0,24,0]),
        ("Jaw", "Head", [0,25,-0.5]), ("ArmLeft", "Torso", [-6,23,0]),
        ("ArmRight", "Torso", [6,23,0]), ("LegLeft", "ToxicBrute", [-2.1,12,0]),
        ("LegRight", "ToxicBrute", [2.1,12,0])):
        m.bone(name, parent, origin)
    for args in (
        ("MutatedTorso", [-4,13,-2.7], [4,24,2.7], "sinew", "Torso"),
        ("ShirtLeft", [-4.2,14,-3], [-1.4,24,3], "shirt", "Torso"),
        ("ShirtRight", [2.1,17,-3], [4.2,24,3], "shirt", "Torso"),
        ("ShirtBackLeft", [-3.9,14,2.7], [-1.2,24,3.1], "shirt", "Torso"),
        ("ShirtBackRight", [1.2,14,2.7], [4,24,3.1], "shirt", "Torso"),
        ("ShirtBackCollar", [-1.2,22,2.7], [1.2,24,3.1], "shirt", "Torso"),
        ("ExposedBackSpine", [-0.55,15,2.7], [0.55,22,3.05], "bone", "Torso"),
        ("TornHem", [-1.6,12.7,-3], [0.1,15,-2.3], "shirt", "Torso"),
        ("ChestCavity", [-1.4,16,-3.1], [2.2,23,-2.8], "mouth", "Torso"),
        ("ExposedNeck", [-1.7,23,-1.8], [1.7,26,1.8], "sinew", "Head"),
        ("MutantHead", [-4,25,-2.8], [4,32,4], "skin", "Head"),
        ("FaceSkinLeft", [-4,26,-4], [-0.6,32,-2.8], "skin", "Head"),
        ("ExposedSkull", [-0.5,26,-4.05], [2.1,32,-2.9], "bone", "Head"),
        ("SkullWound", [2.1,26.8,-3.5], [4.1,31,-2.6], "mouth", "Head"),
        ("SkullTopWound", [0.3,31.9,-1.8], [3.4,32.3,2.9], "sinew", "Head"),
        ("SkullSideBone", [3.9,27.5,-2.4], [4.2,30,1.2], "bone", "Head"),
        ("BrokenTempleRim", [3.55,26.5,-3.8], [4.1,31.3,-2.6], "bone", "Head"),
        ("EyeSocketLeft", [-3.3,28.2,-4.2], [-1.2,29.7,-3.9], "mouth", "Head"),
        ("EyeSocketRight", [0.3,28,-4.25], [2.2,30,-3.9], "mouth", "Head"),
        ("HeavyJaw", [-3.6,23.8,-3.9], [3.6,26,2.5], "skin", "Jaw"),
        ("BrokenMouth", [-2.2,24.8,-4.1], [2.7,26,-3.8], "mouth", "Jaw"),
        ("JawBone", [0.1,24,-4.25], [2.8,24.7,-3.8], "bone", "Jaw")):
        m.cube(*args)
    for rib in range(3):
        m.cube(f"ExposedChestRib{rib}", [-1.2,18+rib*1.8,-3.55], [2.2,18.8+rib*1.8,-3.05], "bone", "Torso")
    for tooth in range(4):
        x = -1.7 + tooth*1.1
        m.cube(f"JawTooth{tooth}", [x,24.6,-4.25], [x+0.65,25.25,-3.8], "tooth", "Jaw")
    for sign, side in ((-1,"Left"), (1,"Right")):
        x, arm = sign*6, "Arm"+side
        a,b = sorted((sign*3.5,sign*6))
        m.cube("ShoulderConnection"+side, [a,21.5,-1.7], [b,23.8,1.7], "sinew", "Torso")
        m.cube("UpperArm"+side, [x-1.8,17,-2], [x+1.8,24,2], "shirt", arm)
        m.cube("ElbowWound"+side, [x-1.65,14,-1.9], [x+1.65,18,1.9], "sinew", arm)
        m.cube("Forearm"+side, [x-1.65,9,-2], [x+1.65,15.5,2], "skin", arm)
        m.cube("ArmBone"+side, [x+0.2,11.5,-2.2], [x+1.2,17,-1.85], "bone", arm)
        m.cube("Hand"+side, [x-1.8,7.5,-2.3], [x+1.8,10.3,2], "skin", arm)
        m.cube("Thumb"+side, [x-sign*1.7-0.55,7.8,-2.8], [x-sign*1.7+0.55,10.1,-1.2], "skin", arm)
        for finger in range(3):
            fx = x-1.4+finger*1.05
            m.cube(f"Finger{side}{finger}", [fx,6.9,-2.4], [fx+0.7,8.5,-1], "skin", arm)
        x, leg = sign*2.1, "Leg"+side
        m.cube("Trousers"+side, [x-1.8,6.5,-2.2], [x+1.8,13,2.2], "pants", leg)
        m.cube("ShinWound"+side, [x-1.6,2,-1.9], [x+1.6,7.5,1.9], "sinew", leg)
        m.cube("ShinBone"+side, [x-0.6,2.5,-2.05], [x+0.5,7.6,-1.5], "bone", leg)
        m.cube("Foot"+side, [x-1.9,0,-3], [x+1.9,3,2.1], "skin", leg)
    for i, (bone, point, length) in enumerate((
        ("Head",[-3.5,26,-4],1.8), ("Head",[3.1,28,-3.7],3.5),
        ("Jaw",[1.6,24.2,-4.1],2.2), ("Head",[4,27.5,2],1.6),
        ("Torso",[1.2,18,-3.5],2.8), ("Torso",[-2.8,14,-3],2),
        ("ArmLeft",[-7.2,8,-2],2), ("ArmLeft",[-5.2,8,-2],1.4),
        ("ArmRight",[6.9,8,-2],2.2), ("ArmRight",[5.1,8,-1.5],1.6),
        ("LegLeft",[-2.6,5,-2],1.5), ("LegRight",[2.8,4.1,-2.2],1.5),
        ("Torso",[2.5,17,3],2))):
        acid_drip(m,i,point,length,bone)
    humanoid_animations(m)
    return m


def spitter():
    m = Model("toxic_spitter", "ToxicSpitter", [0,12,0])
    m.bone("Torso", "ToxicSpitter", [0,12,0])
    m.bone("Head", "Torso", [0,24,0])
    m.bone("Jaw", "Head", [0,24.5,0])
    for sign, side in ((-1,"Left"), (1,"Right")):
        m.bone("Arm"+side, "Torso", [sign*5.5,23,0])
        m.bone("Leg"+side, "ToxicSpitter", [sign*2.2,12,0])
    m.cube("Spine", [-0.8,11,0], [0.8,25,1.5], "bone", "Torso")
    m.cube("NeckVertebrae", [-0.9,23.5,-0.7], [0.9,26.2,1.3], "bone", "Head")
    for level in range(4):
        y, width = 14+level*2.5, 3.8+level*0.35
        for sign, side in ((-1,"Left"), (1,"Right")):
            x0,x1 = sorted((sign*0.7,sign*width))
            m.cube(f"Rib{side}{level}", [x0,y,-1.8], [x1,y+1,-0.8], "bone", "Torso")
            x = sign*width
            m.cube(f"RibSide{side}{level}", [x-0.5,y,-1.3], [x+0.5,y+1,1.5], "bone", "Torso")
            m.cube(f"RibBack{side}{level}", [min(x,0),y,0.7], [max(x,0),y+1,1.5], "bone", "Torso")
    for args in (
        ("Sternum", [-0.65,15,-2], [0.65,23.5,-1], "bone", "Torso"),
        ("AcidHeart", [-1.3,17,-0.9], [1.3,20,1], "acid", "Torso"),
        ("Pelvis", [-3.5,10,-2], [3.5,13,2], "bone", "Torso"),
        ("Skull", [-4,25,-1.8], [4,32,3.7], "bone", "Head"),
        ("SkullFaceLeft", [-4,26,-3.7], [-0.2,32,-1.8], "bone", "Head"),
        ("SkullCrown", [0,30.8,-3.7], [4,32,-1.8], "bone", "Head"),
        ("SkullCheek", [0,25,-3.7], [4,27,-1.8], "bone", "Head"),
        ("SkullFracture", [0.2,27.1,-2.5], [4.15,30.6,-1.7], "mouth", "Head"),
        ("SkullTempleRim", [3.5,26.5,-3.7], [4.15,31.3,-1.7], "bone", "Head"),
        ("SkullAcid", [1.7,27.5,-2.8], [4.25,30,-2.4], "acid", "Head"),
        ("Jaw", [-3.6,23.6,-3.8], [3.6,25.3,1.6], "bone", "Jaw"),
        ("Mouth", [-2.8,25,-4], [2.8,26.7,-3.65], "mouth", "Head"),
        ("Nose", [-0.6,27,-4.05], [0.6,28.5,-3.65], "mouth", "Head"),
        ("SkullCorrosion", [2.5,29,1.2], [4.1,31.5,3.8], "crust", "Head")):
        m.cube(*args)
    for sign, side in ((-1,"Left"), (1,"Right")):
        x = sign*2.1
        if side == "Left":
            m.cube("EyeSocket"+side, [x-1.25,28,-3.98], [x+1.25,30.6,-3.68], "mouth", "Head")
            m.cube("Eye"+side, [x-0.5,28.7,-4.08], [x+0.5,29.7,-3.97], "eye", "Head")
        x,arm = sign*5.5,"Arm"+side
        a,b = sorted((0,sign*5.5))
        m.cube("Clavicle"+side, [a,23,-0.8], [b,24,0.8], "bone", "Torso")
        m.cube("Shoulder"+side, [x-1.1,22,-1.2], [x+1.1,24.5,1.2], "bone", arm)
        m.cube("UpperArm"+side, [x-0.9,15.5,-0.9], [x+0.9,23,0.9], "bone", arm)
        m.cube("Forearm"+side, [x-0.9,9.5,-0.9], [x+0.9,16,0.9], "bone", arm)
        m.cube("Hand"+side, [x-1.2,8.4,-1.2], [x+1.2,11,1.2], "bone", arm)
        m.cube("Thumb"+side, [x-sign*1.2-0.35,8.4,-1.5], [x-sign*1.2+0.35,10.2,-0.2], "bone", arm)
        for finger in range(3):
            fx=x-0.95+finger*0.75
            m.cube(f"Finger{side}{finger}", [fx,7.8,-1.3], [fx+0.5,9.2,-0.3], "bone", arm)
        x,leg=sign*2.2,"Leg"+side
        m.cube(leg, [x-1,1.5,-1], [x+1,12,1], "bone", leg)
        m.cube("Knee"+side, [x-1.2,5.4,-1.3], [x+1.2,7.2,1.1], "bone", leg)
        m.cube("Foot"+side, [x-1.2,0,-2.8], [x+1.2,2,1.2], "bone", leg)
    for tooth in range(4):
        x=-2.2+tooth*1.2
        m.cube(f"JawTooth{tooth}", [x,25,-3.95], [x+0.7,25.65,-3.5], "tooth", "Jaw")
    m.bone("AcidBow", "ArmRight", [6,10,-1.5])
    m.rod("BowUpperLimb", [6.3,10.5,-2], [8,17,-2], 1, "iron", "AcidBow")
    m.rod("BowLowerLimb", [6.3,10.5,-2], [8,4,-2], 1, "iron", "AcidBow")
    m.rod("BowString", [8,4,-2], [8,17,-2], 0.25, "sinew", "AcidBow")
    m.cube("BowGrip", [5.5,9,-2.8], [6.9,12,-1.2], "leather", "AcidBow")
    for i,(bone,point,length) in enumerate((
        ("Head",[3.8,27.8,-3],3.5), ("Head",[2.2,28,-3],4.3),
        ("Jaw",[-2.6,24,-3.6],2.5), ("Head",[4,27,2.9],2.2),
        ("Torso",[2,19.5,-1.9],3), ("Torso",[-3.5,16.5,-1.8],2.6),
        ("Torso",[1.4,11,-2.1],1.7), ("ArmLeft",[-5.5,9,-1.2],2.2),
        ("ArmRight",[5.8,9.5,-1.2],1.9), ("LegLeft",[-2.8,6,-1.2],2.2),
        ("LegRight",[2.4,4,-1.2],2), ("AcidBow",[8,15.8,-2.2],2.8))):
        acid_drip(m,i,point,length,bone)
    humanoid_animations(m, True)
    return m


def crawler():
    m = Model("toxic_mob", "ToxicMutant", [0,7,0])
    for name,parent,origin in (("Thorax","ToxicMutant",[0,8,-2]), ("Abdomen","ToxicMutant",[0,8,3]),
        ("Head","Thorax",[0,8,-6]), ("Jaw","Head",[0,5,-8])):
        m.bone(name,parent,origin)
    for args in (
        ("MutatedBody", [-4.2,5.5,-5.8], [4.2,11,2.5], "sinew", "Thorax"),
        ("Underbelly", [-3.5,4,-4.7], [3.5,6.5,2], "bone", "Thorax"),
        ("MeltedAbdomen", [-6,4.5,2], [6,11,12], "chitin", "Abdomen"),
        ("AbdomenWound", [-4,10.6,3], [4,12.2,10.5], "sinew", "Abdomen"),
        ("Head", [-3.8,5,-8.5], [3.8,11,-5.5], "sinew", "Head"),
        ("SkullFace", [-3.8,8.8,-10.4], [3.8,11,-8.5], "bone", "Head"),
        ("SkullCheekLeft", [-3.8,5.2,-10.4], [-1.8,9,-8.5], "bone", "Head"),
        ("SkullCheekRight", [1.8,5.2,-10.4], [3.8,9,-8.5], "bone", "Head"),
        ("SkullNoseBridge", [-0.65,6,-10.4], [0.65,9.3,-8.5], "bone", "Head"),
        ("NasalCavity", [-0.6,6.4,-10.55], [0.6,7.8,-10.35], "mouth", "Head"),
        ("Jaw", [-2.9,3.4,-10.4], [2.9,5.5,-6.5], "bone", "Jaw"),
        ("AcidSacCenter", [-5.9,6,8.4], [-5.2,10.2,11.9], "acid", "Abdomen"),
        ("DorsalSpikeCenter", [3.4,10.8,7.7], [5.6,12.4,10.7], "bone", "Abdomen"),
        ("AbdomenBonePatch", [-6.2,7.9,2.2], [-4.6,11.3,5.7], "bone", "Abdomen")):
        m.cube(*args)
    for rib in range(3):
        m.cube(f"ExposedAbdomenRib{rib}", [-3.2,4.1,4+rib*2.3], [3.2,4.9,5+rib*2.3], "bone", "Abdomen")
    for tooth in range(4):
        x=-2.2+tooth*1.2
        m.cube(f"JawTooth{tooth}", [x,5,-10.5], [x+0.7,6,-9.3], "tooth", "Jaw")
    for sign,side in ((-1,"Left"),(1,"Right")):
        x = sign*1.25
        m.cube("Eye"+side,[x-0.8,7.8,-10.55],[x+0.8,9,-10.35],"mouth","Head")
        m.rod("FrontClaw"+side,[sign*2.8,5.2,-9.5],[sign*3.6,2.7,-12],1.2,"tooth","Jaw")
        m.rod("FangTip"+side,[sign*3.6,2.7,-12],[sign*2.7,2,-13],0.8,"tooth","Jaw")
        for leg in range(1,5):
            z=-4.4+(leg-1)*3
            spread=(-4.2,-1.8,2,4.4)[leg-1]
            name=f"Leg{leg}{side}"
            hip=[sign*3.8,8,z]
            knee=[sign*9.5,9.1,z+spread]
            foot=[sign*14,0.6,z+spread*1.35]
            m.bone(name,"ToxicMutant",hip)
            m.cube(f"Leg{leg}Socket{side}", [hip[0]-0.95,hip[1]-0.95,hip[2]-0.95],
                [hip[0]+0.95,hip[1]+0.95,hip[2]+0.95], "sinew", name)
            m.rod(f"Leg{leg}Upper{side}",hip,knee,1.7,"chitin",name)
            m.rod(f"Leg{leg}Lower{side}",knee,foot,1.3,"bone",name)
            if leg in (1,4):
                exposed_start = [knee[i]+(foot[i]-knee[i])*0.25 for i in range(3)]
                exposed_end = [knee[i]+(foot[i]-knee[i])*0.55 for i in range(3)]
                m.rod(f"Leg{leg}Flesh{side}",exposed_start,exposed_end,1.5,"sinew",name)
            m.cube(f"Leg{leg}Joint{side}",[knee[0]-0.9,knee[1]-0.9,knee[2]-0.9],
                [knee[0]+0.9,knee[1]+0.9,knee[2]+0.9],"bone",name)
            m.cube(f"Leg{leg}Foot{side}", [foot[0]-0.8,0,foot[2]-1],
                [foot[0]+0.8,1.2,foot[2]+1], "tooth", name)
            acid_drip(m,f"Leg{leg}{side}",[foot[0],2.5,foot[2]],1.8,name)
    for i,(bone,point,length) in enumerate((
        ("Head",[-3.1,6,-10.5],3), ("Head",[3,7,-10.5],4),
        ("Jaw",[-0.8,4.5,-10.4],2.6), ("Jaw",[1.4,4.8,-10.3],3.4),
        ("Abdomen",[-5.8,7.1,10.5],3.5), ("Abdomen",[5.8,6.8,6.5],4.2),
        ("Abdomen",[3.7,5,11.8],3.4), ("Abdomen",[-2.1,5,11.8],3),
        ("Thorax",[-4.1,5.5,-3],3), ("Thorax",[4.1,5.5,-1],3))):
        acid_drip(m,i,point,length,bone)
    m.animation("idle",2,"loop",{
        "Thorax":{"position":[(0,[0,0,0]),(1,[0,0.15,0]),(2,[0,0,0])]},
        "Abdomen":{"rotation":loop_axis(0,1.5,2)},
        "Jaw":{"rotation":[(0,[0,0,0]),(1,[-4,0,0]),(2,[0,0,0])]}})
    walk={"ToxicMutant":{"position":[(0,[0,0,0]),(0.2,[0,0.22,0]),(0.4,[0,0,0]),
                                      (0.6,[0,0.22,0]),(0.8,[0,0,0])]}}
    for leg in range(1,5):
        for side in ("Left","Right"):
            walk[f"Leg{leg}{side}"]={"rotation":loop_axis(1,13,0.8,(leg%2==0)!=(side=="Left"))}
    m.animation("walk",0.8,"loop",walk)
    m.animation("attack",0.55,"once",{
        "ToxicMutant":{"position":[(0,[0,0,0]),(0.12,[0,-0.3,0.6]),(0.3,[0,0.3,-1.6]),(0.55,[0,0,0])]},
        "Jaw":{"rotation":[(0,[0,0,0]),(0.12,[-22,0,0]),(0.3,[6,0,0]),(0.55,[0,0,0])]},
        "Leg1Left":{"rotation":[(0,[0,0,0]),(0.12,[0,-12,-12]),(0.55,[0,0,0])]},
        "Leg1Right":{"rotation":[(0,[0,0,0]),(0.12,[0,12,12]),(0.55,[0,0,0])]}})
    m.animation("hurt",0.35,"once",{
        "ToxicMutant":{"rotation":[(0,[0,0,0]),(0.1,[-6,0,5]),(0.35,[0,0,0])]},
        "Head":{"rotation":[(0,[0,0,0]),(0.1,[0,8,0]),(0.35,[0,0,0])]}})
    return m


if __name__ == "__main__":
    for asset in (crawler(), brute(), spitter()):
        asset.save()

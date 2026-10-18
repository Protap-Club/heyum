"""
One illustration per dish. Ids match shared/.../model/DishesPool.kt and docs/dish-images.csv.
"""
import math
import zlib

from PIL import ImageDraw

from dishart import (E, Ln, P, TAU, bowl, board, dollop, drizzle, epoly, fill, herb, herbs, jit, lighten, mixc,
                     noodles, pan, plate, prop_dip, props, rgba, rice, ring_pts, shade, shape_pts, shaded, spots,
                     sprinkle, steamer)
from templates import (CX, CY, bowl_dish, bun_top, burger, dumpling, fries, garnish, glass, latte_art, leaf, momo, mug,
                       paper_tray, pizza, place, ring_item, roll, slab, slice_, stack, toast, waffle)

REG = {}


def dish(id_, bg="slate"):
    def deco(fn):
        REG[id_] = (bg, fn)
        return fn
    return deco


def seed_for(id_):
    return zlib.crc32(id_.encode())


# ------------------------------------------------------------------ palettes: (base, blobs, flecks)
def pal(base, d1=0.85, l1=0.12, fleck=None):
    return dict(base=base, blobs=[shade(base, d1), lighten(base, l1), shade(base, 0.95)], flecks=fleck or [shade(base, 0.7), lighten(base, 0.3)])


PALAK = dict(base=(44, 86, 32), blobs=[(70, 120, 40), (58, 104, 34), (86, 138, 48), (36, 74, 28)], flecks=[(24, 56, 20), (112, 164, 64), (30, 64, 24)])
BUTTER = pal((208, 90, 36), fleck=[(170, 50, 20), (244, 160, 80)])
TOMATO = pal((192, 54, 34), fleck=[(130, 30, 20), (232, 112, 72)])
BROWN = pal((124, 66, 34), fleck=[(70, 34, 18), (190, 120, 64)])
YELLOW = pal((224, 170, 46), fleck=[(176, 120, 28), (252, 216, 112)])
WHITEG = pal((240, 222, 182), fleck=[(214, 180, 116), (252, 244, 220)])
GREENG = pal((138, 176, 76), fleck=[(70, 110, 40), (196, 220, 128)])
REDG = pal((164, 42, 26), fleck=[(100, 22, 14), (226, 96, 54)])
DARKRED = pal((104, 38, 28), fleck=[(70, 20, 16), (170, 70, 50)])
CHOLE = pal((158, 98, 42), fleck=[(110, 62, 24), (210, 150, 80)])
ORANGE = pal((226, 118, 44), fleck=[(180, 70, 24), (250, 170, 90)])
MUSTARD = pal((218, 166, 60), fleck=[(160, 110, 30), (250, 220, 130)])
BROTH = pal((220, 176, 92), 0.9, 0.1, fleck=[(180, 130, 56), (250, 220, 150)])
CLEARB = pal((232, 204, 138), 0.92, 0.08, fleck=[(200, 164, 90), (252, 236, 190)])
CREAMY = pal((238, 226, 196), fleck=[(214, 196, 150), (252, 248, 232)])
SOYG = pal((92, 44, 22), fleck=[(50, 24, 12), (150, 84, 44)])

CHICKEN = (214, 150, 74)
TANDOORI = (200, 88, 44)
PANEER = (248, 238, 208)
POTATO = (232, 196, 108)
GREENVEG = (84, 150, 60)
CARROT = (232, 124, 40)


def curry(s, vessel="copper", r=340, sauce=BUTTER, items=(), swirl=False, garn=(), pr=(), **kw):
    return bowl_dish(s, vessel=vessel, r=r, items=items, swirl=swirl, garn=garn, pr=pr, **sauce, **kw)


# ============================================================================ INDIAN
@dish("palak_paneer", "slate")
def _(s):
    curry(s, "copper", sauce=PALAK, items=[("cube", 34, PANEER, 8)], swirl=True, garn=["ginger", "chili"], pr=[("spice", {}), "leaf"])


@dish("butter_chicken", "slate")
def _(s):
    curry(s, "copper", sauce=BUTTER, items=[("blob", 34, TANDOORI, 8)], swirl=True, garn=["cilantro", "ginger"], pr=["spice", "chili"])


@dish("dish_022", "slate")  # Chicken Tikka
def _(s):
    plate(s, CX, CY, 360, "dark")
    pts = place(s, CX, CY, 290, 280, [("blob", 40, (200, 84, 40), 9), ("ring", 26, (176, 76, 124), 5, {"thick": 0.14}), ("slice", 22, (230, 170, 60), 3, {"edge": (40, 120, 50)})], base_mind=1.5)
    sprinkle(s, CX, CY, 280, 270, 90, "dot", (28, 20, 16), (2, 4))
    herbs(s, spots(s, CX, CY, 250, 240, 5, 50), 10)
    props(s, ["lime", ("dip", {"color": (90, 150, 60)}), "chili"])


@dish("dish_026", "slate")  # Tandoori chicken
def _(s):
    plate(s, CX, CY, 360, "white")
    for (x, y, rot) in [(480, 450, -0.5), (680, 430, 0.5), (570, 620, 0.1)]:
        shaded(s, "rect", x, y, 130, 42, rot, (196, 70, 36), wob=0.05, ex=2.6)
        shaded(s, "blob", x + math.cos(rot) * 120, y + math.sin(rot) * 120, 34, 30, 0, (230, 220, 200), wob=0.1)
        sprinkle(s, x, y, 100, 28, 40, "dot", (70, 24, 14), (2, 4))
    place(s, CX, CY + 40, 250, 200, [("ring", 24, (176, 76, 124), 7, {"thick": 0.12})])
    props(s, ["lemon", ("dip", {"color": (96, 150, 70)}), "chili"])


@dish("dish_027", "terracotta")  # Aloo Gobhi
def _(s):
    rx, ry = pan(s, CX, CY, 340, "dark")
    fill(s, CX, CY, rx, ry, (206, 148, 40), [(230, 176, 60), (186, 120, 30)], [(250, 220, 120)], nblob=100, nfleck=300)
    place(s, CX, CY, rx, ry, [("cube", 30, POTATO, 14), ("ball", 30, (246, 238, 214), 10, {"mind": 1.5}), ("ball", 8, GREENVEG, 16, {"mind": 1.5})])
    garnish(s, CX, CY, rx, ry, ["cumin", "chili", "cilantro"])
    props(s, ["spice", "tomato"])


@dish("dish_028", "wood")  # Paneer Bhurji
def _(s):
    rx, ry = pan(s, CX, CY, 340, "steel")
    fill(s, CX, CY, rx, ry, (228, 140, 40), [(240, 170, 60), (206, 110, 30)], [(250, 220, 130)], nblob=100, nfleck=200)
    place(s, CX, CY, rx, ry, [("blob", 15, (252, 240, 200), 70, {"mind": 1.1}), ("rect", 10, (90, 160, 60), 14), ("disc", 10, (200, 50, 36), 12)])
    garnish(s, CX, CY, rx, ry, ["cilantro", "chili"])
    props(s, ["lime", "spoon"])


@dish("dish_029", "linen")  # Veg Pulao
def _(s):
    bowl_dish(s, "cream", 340, base=None, rice_=dict(color=(244, 238, 218), accent=[(244, 190, 60), (110, 170, 70), (230, 130, 50)], accent_n=0.25, n=1500),
              items=[("cube", 14, CARROT, 12), ("ball", 11, (110, 180, 70), 18), ("rect", 10, (96, 160, 64), 8)], garn=["friedonion", "cilantro"], pr=["spice", "lemon"])


@dish("dish_051", "slate")  # Malai Kofta
def _(s):
    curry(s, "copper", sauce=WHITEG, items=[("ball", 38, (176, 108, 48), 7)], swirl=True, garn=["saffron", "cilantro"], pr=["spice", "leaf"])


@dish("dish_052", "wood")  # Dal Tadka
def _(s):
    curry(s, "steel", sauce=YELLOW, items=[("slice", 10, (200, 40, 28), 10, {"edge": (150, 24, 20)})], garn=["cumin", "chili", "cilantro"],
          after=lambda s, cx, cy, rx, ry: drizzle(s, cx, cy - 10, 100, (190, 60, 20), "swirl", 6, 180), pr=["lemon", "spice"])


@dish("dish_053", "slate")  # Baingan Bharta
def _(s):
    curry(s, "terra", sauce=pal((142, 100, 54), fleck=[(90, 60, 30), (196, 150, 80)]), items=[("blob", 14, (214, 80, 40), 12), ("disc", 12, (230, 200, 130), 10)], garn=["cilantro", "chili"], pr=["chili", "lime"])


@dish("dish_054", "slate")  # Classic chicken curry
def _(s):
    curry(s, "copper", sauce=ORANGE, items=[("blob", 34, CHICKEN, 7)], garn=["cilantro", "ginger"], pr=["spice", "lemon"])


@dish("dish_055", "slate")  # Rogan josh
def _(s):
    curry(s, "copper", sauce=pal((156, 52, 28), fleck=[(100, 28, 16), (214, 100, 56)]), items=[("blob", 38, (122, 62, 36), 7)], swirl=True, swirl_color=(240, 214, 190), garn=["cilantro"], pr=["chili", "cinnamon"])


@dish("dish_056", "navy")  # Goan fish curry
def _(s):
    curry(s, "terra", sauce=pal((224, 120, 40), fleck=[(190, 70, 24), (250, 180, 96)]), items=[("rect", 30, (246, 232, 206), 6), ("leaf", 8, (70, 130, 50), 8)], garn=["chili", "cilantro"], pr=["lime", "chili"])


@dish("dish_057", "slate")  # Prawn masala
def _(s):
    rx, ry = bowl(s, CX, CY, 340, "copper")
    fill(s, CX, CY, rx, ry, *[v for v in (ORANGE["base"], ORANGE["blobs"], ORANGE["flecks"])])
    for (x, y) in spots(s, CX, CY, rx * 0.8, ry * 0.8, 8, 95):
        shaded(s, "cres", x, y, 44, 44, s.r.uniform(0, TAU), (240, 140, 90), wob=0.05, hl=0.3)
    garnish(s, CX, CY, rx, ry, ["cilantro", "chili"])
    props(s, ["lemon", "spice"])


@dish("dish_058", "slate")  # Paneer tikka masala
def _(s):
    curry(s, "copper", sauce=pal((210, 80, 34), fleck=[(170, 50, 20), (244, 150, 70)]), items=[("cube", 32, (246, 214, 150), 6), ("rect", 14, (96, 160, 60), 5), ("disc", 18, (220, 60, 40), 4)], swirl=True, garn=["cilantro", "redonion"], pr=["spice", "chili"])


@dish("dish_059", "slate")  # Dum aloo
def _(s):
    curry(s, "dark", sauce=pal((158, 54, 28), fleck=[(100, 28, 16), (220, 100, 56)]), items=[("ball", 38, (220, 168, 90), 7)], garn=["cilantro", "cumin"], pr=["chili", "spice"])


@dish("dish_060", "wood")  # Kadhi pakora
def _(s):
    curry(s, "white", sauce=pal((234, 196, 84), fleck=[(190, 150, 50), (252, 232, 150)]), items=[("ball", 26, (186, 118, 48), 9)], garn=["cumin", "chili", "cilantro"],
          after=lambda s, cx, cy, rx, ry: drizzle(s, cx, cy, 90, (176, 40, 20), "swirl", 5, 200), pr=["chili", "spice"])


@dish("dish_061", "terracotta")  # Undhiyu
def _(s):
    curry(s, "dark", sauce=pal((136, 124, 40), fleck=[(80, 90, 30), (214, 190, 90)]), items=[("cube", 28, (122, 70, 120), 5), ("ball", 22, (232, 190, 80), 6), ("rect", 14, (96, 160, 60), 9), ("ball", 20, (226, 120, 60), 5)], garn=["cilantro", "sesame"], pr=["chili", "lime"])


@dish("dish_062", "wood")  # Pesarattu
def _(s):
    plate(s, CX, CY, 370, "white")
    for (x, y, rot) in [(520, 470, -0.2), (680, 560, 0.15)]:
        shaded(s, "rect", x, y, 200, 120, rot, (122, 154, 62), wob=0.04, ex=2.2)
        sprinkle(s, x, y, 160, 90, 120, "dot", (190, 214, 110), (2, 5))
    props(s, [("dip", {"color": (244, 240, 224)}), ("dip", {"color": (200, 60, 40)}), ("dip", {"color": (96, 150, 60)})])


@dish("dish_063", "linen")  # Appam with veg stew
def _(s):
    plate(s, 440, 540, 270, "wood")
    for (x, y, r) in [(380, 480, 100), (500, 600, 100), (470, 450, 70)]:
        lay = s.lay()
        d = ImageDraw.Draw(lay)
        E(d, x, y, r, r * 0.92, rgba((246, 240, 226)))
        E(d, x, y, r * 0.55, r * 0.5, rgba((254, 252, 244)))
        E(d, x, y, r * 0.96, r * 0.88, None)
        d.ellipse([(x - r) * 2, (y - r * 0.92) * 2, (x + r) * 2, (y + r * 0.92) * 2], outline=rgba((210, 170, 100)), width=8)
        s.put(lay, shadow=(5, 9, 8, 0.5))
    rx, ry = bowl(s, 800, 500, 210, "cream")
    fill(s, 800, 500, rx, ry, (244, 232, 200), [(248, 238, 214), (232, 218, 180)], [(180, 120, 60)], nblob=60, nfleck=60)
    place(s, 800, 500, rx, ry, [("cube", 14, CARROT, 8), ("cube", 12, (240, 224, 150), 6), ("ball", 8, GREENVEG, 10)], base_mind=1.2)
    herbs(s, spots(s, 800, 500, rx * 0.7, ry * 0.7, 3, 40), 9)
    props(s, [("spice", {"color": (200, 60, 30)})])


@dish("dish_064", "slate")  # Chicken Chettinad
def _(s):
    curry(s, "dark", sauce=pal((120, 52, 26), fleck=[(60, 26, 14), (190, 110, 56)]), items=[("blob", 34, (150, 80, 40), 7), ("leaf", 8, (60, 120, 46), 8)], garn=["pepper", "chili", "cumin"], pr=["chili", "cinnamon"])


@dish("dish_065", "slate")  # Laal maas
def _(s):
    curry(s, "copper", sauce=pal((178, 36, 22), fleck=[(110, 18, 12), (240, 100, 56)]), items=[("blob", 38, (130, 52, 30), 7)], garn=["chili", "cilantro"], pr=["chili", "spice"])


@dish("dish_101", "wood")  # Chole Bhature
def _(s):
    bowl_dish(s, "steel", 230, base=CHOLE["base"], blobs=CHOLE["blobs"], flecks=CHOLE["flecks"], items=[("ball", 14, (226, 176, 100), 22, {"mind": 1.05})], garn=["cilantro", "redonion", "chili"], cx=380, cy=480)
    for (x, y, rot) in [(790, 400, 0.3), (780, 660, -0.2)]:
        shaded(s, "blob", x, y, 175, 140, rot, (226, 170, 80), wob=0.08, hl=0.3, edge=(168, 110, 40))
        sprinkle(s, x, y, 140, 110, 30, "dot", (150, 90, 36), (3, 6))
    props(s, [("lemon", {}), "chili"])


@dish("dish_104", "linen")  # Rajma Chawal
def _(s):
    bowl_dish(s, "white", 220, base=DARKRED["base"], blobs=DARKRED["blobs"], flecks=DARKRED["flecks"], items=[("ball", 14, (150, 44, 36), 26, {"mind": 1.0})], garn=["cilantro"], cx=380, cy=500)
    rice(s, 810, 500, 190, 170, n=1000)
    herbs(s, spots(s, 810, 500, 120, 100, 3, 40), 9)
    props(s, ["lemon", "chili"])


@dish("dish_105", "slate")  # Pav bhaji
def _(s):
    rx, ry = bowl(s, 520, 480, 290, "steel")
    fill(s, 520, 480, rx, ry, (196, 70, 30), [(220, 90, 40), (170, 50, 24)], [(240, 160, 70), (120, 150, 60)], nblob=200, nfleck=500)
    shaded(s, "rect", 520, 470, 52, 38, 0.2, (250, 232, 130), wob=0.0, ex=4)
    garnish(s, 520, 480, rx, ry, ["cilantro", "redonion"])
    for (x, y) in [(870, 330), (910, 520), (860, 710)]:
        shaded(s, "blob", x, y, 92, 80, 0, (226, 176, 98), wob=0.05, hl=0.35, edge=(176, 120, 56))
    props(s, ["lemon"])


@dish("dish_106", "wood")  # Aloo Paratha
def _(s):
    plate(s, CX, CY, 360, "white")
    stack(s, 560, 470, 200, 3, (222, 168, 86), edge=(176, 120, 56), offset=22)
    sprinkle(s, 560, 460, 170, 150, 90, "dot", (150, 90, 36), (3, 7))
    shaded(s, "rect", 560, 440, 44, 34, 0.2, (250, 232, 130), wob=0.0, ex=4)
    props(s, [("dip", {"color": (248, 246, 236)}), "chili", "lemon"])


@dish("dish_107", "slate")  # Hyderabadi veg biryani
def _(s):
    bowl_dish(s, "copper", 345, base=None, rice_=dict(color=(244, 236, 214), accent=[(240, 170, 40), (214, 120, 40), (120, 160, 70)], accent_n=0.35, n=1700),
              items=[("cube", 20, CARROT, 8), ("ball", 12, (110, 180, 70), 10), ("cube", 22, (246, 220, 150), 5)], garn=["friedonion", "mint", "saffron"], pr=["lemon", ("dip", {"color": (250, 248, 240)})])


@dish("chicken_biryani", "slate")
def _(s):
    bowl_dish(s, "copper", 345, base=None, rice_=dict(color=(244, 236, 214), accent=[(240, 170, 40), (214, 120, 40)], accent_n=0.35, n=1700),
              items=[("blob", 34, (176, 100, 44), 6), ("disc", 18, (250, 246, 234), 2)], garn=["friedonion", "mint", "saffron"], pr=["lemon", ("dip", {"color": (250, 248, 240)})])


@dish("dish_108", "slate")  # Kadhai paneer
def _(s):
    rx, ry = pan(s, CX, CY, 330, "dark")
    fill(s, CX, CY, rx, ry, (196, 66, 30), [(220, 90, 40), (170, 50, 24)], [(240, 150, 70)], nblob=180, nfleck=300)
    place(s, CX, CY, rx, ry, [("cube", 30, (248, 232, 190), 7), ("rect", 14, (76, 150, 56), 8), ("rect", 14, (232, 190, 50), 6), ("ring", 16, (176, 76, 124), 4, {"thick": 0.14})])
    garnish(s, CX, CY, rx, ry, ["cilantro", "ginger"])
    props(s, ["chili", "spice"])


@dish("masala_dosa", "slate")
def _(s):
    plate(s, CX, CY, 390, "steel")
    shaded(s, "rect", 590, 420, 340, 92, -0.12, (214, 148, 64), wob=0.02, ex=2.2, hl=0.25)
    sprinkle(s, 590, 420, 300, 70, 240, "dot", (150, 84, 30), (2, 6))
    shaded(s, "blob", 330, 470, 56, 60, 0, (236, 196, 80), wob=0.1, hl=0.25)
    for (x, y, c) in [(450, 740, (186, 92, 36)), (610, 765, (244, 240, 226)), (770, 735, (196, 60, 40))]:
        prop_dip(s, x, y, c, 64, "steel")
    herbs(s, [(620, 760), (600, 770)], 8)


@dish("dish_152", "terracotta")  # Litti Chokha
def _(s):
    plate(s, CX, CY, 370, "white")
    for (x, y) in [(430, 430), (560, 360), (690, 430), (500, 540)]:
        shaded(s, "blob", x, y, 70, 66, 0, (190, 128, 64), wob=0.07, edge=(110, 70, 30))
        sprinkle(s, x, y, 52, 48, 12, "dot", (110, 60, 24), (2, 4))
    fill(s, 740, 650, 150, 110, (150, 106, 62), [(180, 130, 70)], [(90, 60, 30), (210, 90, 50)], nblob=60, nfleck=160)
    place(s, 740, 650, 150, 110, [("ball", 8, (200, 50, 36), 8, {"mind": 1.4})])
    herbs(s, [(720, 640), (760, 660)], 9)
    props(s, ["lemon", "chili"])


@dish("dish_153", "linen")  # Poha
def _(s):
    bowl_dish(s, "white", 320, base=(236, 200, 80), blobs=[(246, 214, 100), (222, 184, 64)], flecks=[(250, 226, 120), (200, 160, 50)], nfleck=700, fsize=(3, 7),
              items=[("ball", 6, (214, 170, 100), 30, {"mind": 1.6}), ("leaf", 6, (70, 130, 50), 8)], garn=["cilantro", "pomegranate", "sev"], pr=["lemon", "chili"])


@dish("dish_154", "wood")  # Medu vada
def _(s):
    plate(s, 430, 500, 300, "white")
    for (x, y) in [(330, 420), (470, 400), (410, 560), (540, 540)]:
        lay = s.lay()
        d = ImageDraw.Draw(lay)
        E(d, x, y, 80, 76, rgba((130, 80, 36)))
        E(d, x, y, 74, 70, rgba((200, 140, 66)))
        E(d, x - 5, y - 6, 66, 60, rgba((220, 164, 86)))
        E(d, x, y, 24, 22, rgba((120, 76, 34)))
        s.put(lay, shadow=(5, 9, 8, 0.55))
    rx, ry = bowl(s, 830, 470, 200, "steel")
    fill(s, 830, 470, rx, ry, (206, 122, 48), [(226, 144, 60), (180, 98, 36)], [(120, 150, 60), (250, 190, 110)], nblob=80, nfleck=200)
    prop_dip(s, 800, 790, (248, 244, 228), 70, "steel")
    prop_dip(s, 940, 760, (92, 150, 60), 56, "steel")


@dish("dish_159", "slate")  # Egg curry
def _(s):
    rx, ry = bowl(s, CX, CY, 340, "copper")
    c = ORANGE
    fill(s, CX, CY, rx, ry, c["base"], c["blobs"], c["flecks"])
    for (x, y, rot) in [(480, 460, 0.4), (700, 440, -0.3), (580, 620, 0.9), (680, 590, 0)]:
        lay = s.lay()
        d = ImageDraw.Draw(lay)
        P(d, shape_pts("blob", x, y, 72, 52, rot), rgba((250, 246, 236)))
        P(d, shape_pts("blob", x, y, 62, 42, rot), rgba((252, 250, 244)))
        E(d, x, y, 28, 26, rgba((248, 190, 50)))
        E(d, x - 7, y - 8, 10, 7, rgba((255, 230, 140)))
        s.put(lay, shadow=(4, 7, 6, 0.5), blur=0.4)
    garnish(s, CX, CY, rx, ry, ["cilantro", "chili"])
    props(s, ["spice", "chili"])


# ============================================================================ CHINESE
def _noodle_bowl(s, vessel, base, noodle, items, garn, pr, r=345, plate_=False):
    return bowl_dish(s, vessel, r, base=base, noodle=noodle, items=items, garn=garn, pr=pr, dish_plate=plate_)


@dish("garlic_noodles", "navy")
def _(s):
    _noodle_bowl(s, "dark", (150, 96, 44), dict(color=(228, 178, 98), n=80), [("disc", 10, (232, 190, 80), 12)], ["scallion", "chili", "sesame"], ["garlic", "chili"])


@dish("dish_021", "slate")  # Veg fried rice
def _(s):
    bowl_dish(s, "white", 340, base=None, rice_=dict(color=(238, 220, 168), accent=[(214, 150, 60), (110, 170, 70), (230, 120, 50)], accent_n=0.3, n=1500),
              items=[("cube", 12, CARROT, 10), ("ball", 10, (110, 180, 70), 14)], garn=["scallion", "sesame"], pr=["lime", "chili"])


@dish("dish_045", "slate")  # Fried spring rolls
def _(s):
    plate(s, CX, CY, 360, "white")
    for (x, y, rot) in [(520, 430, -0.5), (640, 470, 0.5), (560, 590, 0.1)]:
        shaded(s, "rect", x, y, 130, 40, rot, (206, 140, 52), wob=0.03, ex=3, hl=0.28)
        sprinkle(s, x, y, 100, 24, 20, "dot", (150, 90, 30), (2, 4))
        shaded(s, "blob", x + math.cos(rot) * 126, y + math.sin(rot) * 126, 26, 34, rot, (236, 214, 150), wob=0.05)
    props(s, [("dip", {"color": (196, 40, 30)}), "chili", "lime"])


@dish("dish_139", "navy")  # Veg spring rolls
def _(s):
    board(s, CX, CY, 400, 330, "dark")
    for (x, y, rot) in [(480, 400, -0.2), (680, 430, 0.2), (520, 560, 0.1), (700, 620, -0.1)]:
        shaded(s, "rect", x, y, 120, 36, rot, (216, 150, 60), wob=0.03, ex=3, hl=0.28)
        shaded(s, "blob", x + math.cos(rot) * 116, y + math.sin(rot) * 116, 24, 32, rot, (130, 170, 70), wob=0.05)
    props(s, [("dip", {"color": (190, 70, 40)}), "lime"])


@dish("dish_046", "slate")  # Steamed veg dumplings
def _(s):
    r = steamer(s, CX, CY, 350)
    for (x, y, rot) in [(480, 430, 0.3), (630, 400, -0.2), (720, 520, 0.9), (610, 560, 0.1), (470, 590, -0.4), (650, 680, 0.5)]:
        dumpling(s, x, y, 80, rot)
    herbs(s, [(540, 500)], 9, (86, 150, 70))
    props(s, [("dip", {"color": (74, 40, 24)}), "chili"])


@dish("dish_066", "navy")  # Veg hakka noodles
def _(s):
    _noodle_bowl(s, "white", (150, 96, 44), dict(color=(226, 176, 96), n=80), [("rect", 7, (96, 160, 64), 14), ("rect", 7, CARROT, 12), ("rect", 7, (140, 90, 130), 8)], ["scallion", "sesame"], ["chili", "lime"])


@dish("dish_067", "slate")  # Schezwan fried rice
def _(s):
    bowl_dish(s, "dark", 340, base=None, rice_=dict(color=(214, 120, 60), accent=[(170, 50, 30), (110, 160, 60), (240, 190, 100)], accent_n=0.35, n=1500),
              items=[("cube", 12, CARROT, 8), ("ball", 10, (110, 180, 70), 10)], garn=["scallion", "chili", "sesame"], pr=["chili", "garlic"])


@dish("dish_068", "slate")  # Veg manchurian
def _(s):
    curry(s, "dark", sauce=SOYG, items=[("ball", 30, (130, 70, 36), 9)], garn=["scallion", "greenstrip", "sesame"], pr=["chili", "garlic"], r=335)


@dish("dish_138", "slate")  # Chicken manchurian
def _(s):
    curry(s, "white", sauce=SOYG, items=[("blob", 30, (150, 82, 40), 9)], garn=["scallion", "chili", "sesame"], pr=["chili", "garlic"], r=335)


@dish("dish_069", "navy")  # Sweet & sour chicken
def _(s):
    curry(s, "white", sauce=pal((212, 62, 34), fleck=[(160, 30, 20), (250, 130, 70)]), items=[("ball", 28, (232, 150, 70), 7), ("cube", 22, (250, 210, 70), 6), ("cube", 18, (80, 160, 60), 5), ("cube", 18, (210, 50, 40), 5)], garn=["sesame", "scallion"], pr=["chili", "lime"])


@dish("dish_071", "navy")  # Kimchi fried rice
def _(s):
    bowl_dish(s, "dark", 340, base=None, rice_=dict(color=(226, 120, 60), accent=[(180, 40, 30), (246, 190, 130)], accent_n=0.3, n=1500),
              items=[("rect", 14, (210, 70, 50), 9), ("cube", 12, (126, 170, 70), 6)], garn=["scallion", "sesame"],
              after=lambda s, cx, cy, rx, ry: (shaded(s, "blob", cx, cy - 10, 80, 74, 0, (252, 250, 244), wob=0.12, hl=0.1), E_yolk(s, cx, cy - 10)), pr=["chili"])


def E_yolk(s, x, y):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, x, y, 30, 29, rgba((246, 184, 40)))
    E(d, x - 8, y - 9, 11, 8, rgba((255, 234, 150)))
    s.put(lay, shadow=(3, 5, 4, 0.4), blur=0.4)


@dish("dish_140", "slate")  # Hot and sour soup
def _(s):
    curry(s, "white", sauce=pal((140, 80, 40), 0.9, 0.1, fleck=[(90, 50, 24), (210, 160, 100)]), items=[("cube", 14, (248, 244, 232), 10), ("disc", 14, (96, 70, 48), 8), ("rect", 8, (250, 220, 90), 14)], garn=["scallion", "pepper"], pr=["chili", "spoon"])


@dish("dish_160", "slate")  # Chicken fried rice
def _(s):
    bowl_dish(s, "white", 340, base=None, rice_=dict(color=(238, 214, 150), accent=[(214, 150, 60), (110, 170, 70)], accent_n=0.25, n=1500),
              items=[("cube", 20, (230, 190, 120), 8), ("ball", 10, (110, 180, 70), 12), ("blob", 12, (252, 224, 90), 8)], garn=["scallion", "sesame"], pr=["lime", "chili"])


# ============================================================================ THAI / SE ASIAN
@dish("pad_thai", "slate")
def _(s):
    bowl_dish(s, "white", 345, base=(166, 98, 40), noodle=dict(color=(226, 148, 70), n=80), dish_plate=True,
              items=[("cres", 30, (240, 130, 80), 5), ("blob", 10, (252, 224, 90), 6)] if False else [("blob", 30, (240, 132, 84), 5), ("blob", 10, (252, 224, 90), 6)],
              garn=["peanut", "greenstrip", "chili"], pr=["lime", "chili"])


@dish("sticky_rice", "linen")
def _(s):
    plate(s, CX, CY, 360, "white")
    rice(s, 500, 520, 170, 140, color=(246, 242, 222), n=1000)
    for k in range(5):
        a = -0.9 + k * 0.38
        shaded(s, "rect", 500 + 190 * math.cos(a) + 110, 520 + 190 * math.sin(a) + 40, 100, 26, a + 1.57, (246, 182, 44), wob=0.04, ex=2.8, hl=0.3)
    drizzle(s, 500, 500, 100, (252, 250, 244), "swirl", 8, 220)
    sprinkle(s, 500, 520, 120, 100, 90, "dot", (244, 234, 206), (1.4, 2.4))
    props(s, ["lime", ("leaf", {"color": (70, 120, 50)})])


@dish("dish_070", "slate")  # Thai green curry
def _(s):
    curry(s, "white", sauce=GREENG, items=[("blob", 28, (240, 232, 206), 5), ("ball", 16, (120, 170, 80), 8), ("leaf", 8, (50, 120, 46), 6), ("rect", 8, (196, 44, 32), 6)], swirl=True, garn=["chili"], pr=["lime", "chili"])


@dish("dish_073", "wood")  # Chicken satay
def _(s):
    board(s, 560, 480, 400, 330, "wood")
    for k in range(4):
        y = 350 + k * 85
        lay = s.lay()
        d = ImageDraw.Draw(lay)
        Ln(d, [(300, y), (820, y + 20)], rgba((214, 180, 120)), 5)
        s.put(lay, shadow=(3, 6, 5, 0.5))
        for j in range(4):
            shaded(s, "rect", 360 + j * 70, y + 4 + j * 4, 38, 30, 0.04, (176, 96, 40), wob=0.04, ex=3.2)
    prop_dip(s, 960, 330, (190, 130, 60), 74)
    props(s, ["lime"])


@dish("dish_074", "slate")  # Nasi goreng
def _(s):
    bowl_dish(s, "white", 345, base=None, dish_plate=True, rice_=dict(color=(176, 100, 50), accent=[(130, 70, 36), (214, 140, 70)], accent_n=0.4, n=1500),
              items=[("slice", 24, (100, 160, 70), 6, {"edge": (50, 110, 50)})], garn=["friedonion", "chili"],
              after=lambda s, cx, cy, rx, ry: (shaded(s, "blob", cx + 20, cy - 10, 90, 80, 0.3, (252, 250, 244), wob=0.14, hl=0.05), E_yolk(s, cx + 20, cy - 10)), pr=["chili", "lime"])


@dish("dish_075", "slate")  # Pho
def _(s):
    rx, ry = bowl(s, CX, CY, 345, "white")
    fill(s, CX, CY, rx, ry, (196, 142, 70), [(210, 160, 84), (176, 122, 56)], [(240, 214, 150)], nblob=100, nfleck=200)
    noodles(s, CX, CY, rx * 0.9, ry * 0.9, (240, 236, 220), n=50, width=6)
    place(s, CX, CY, rx, ry, [("rect", 30, (166, 70, 56), 6), ("slice", 12, (240, 236, 220), 6)])
    garnish(s, CX, CY, rx, ry, ["scallion", "basil", "cilantro"])
    props(s, ["lime", "chili", ("leaf", {"color": (60, 130, 56)})])


@dish("dish_120", "navy")  # Tom yum
def _(s):
    curry(s, "white", sauce=pal((222, 92, 36), 0.9, 0.1, fleck=[(180, 54, 24), (250, 150, 80)]), items=[("blob", 26, (244, 150, 100), 4), ("disc", 18, (236, 220, 190), 6), ("leaf", 7, (70, 140, 50), 6), ("rect", 12, (110, 160, 70), 3)], garn=["chili", "cilantro"], pr=["lime", "chili"])


@dish("dish_121", "slate")  # Thai basil fried rice
def _(s):
    bowl_dish(s, "white", 345, base=None, dish_plate=True, rice_=dict(color=(226, 190, 120), accent=[(100, 70, 40), (60, 120, 50)], accent_n=0.3, n=1500),
              items=[("leaf", 9, (56, 120, 48), 14), ("rect", 8, (196, 44, 32), 8), ("blob", 16, (214, 160, 90), 6)], garn=["chili"],
              after=lambda s, cx, cy, rx, ry: (shaded(s, "blob", cx + 20, cy - 10, 84, 76, 0.3, (252, 250, 244), wob=0.14, hl=0.05), E_yolk(s, cx + 20, cy - 10)), pr=["chili", "lime"])


@dish("dish_166", "wood")  # Thukpa
def _(s):
    rx, ry = bowl(s, CX, CY, 345, "terra")
    fill(s, CX, CY, rx, ry, (220, 150, 70), [(236, 170, 84), (196, 126, 56)], [(250, 220, 150)], nblob=90, nfleck=200)
    noodles(s, CX, CY, rx * 0.9, ry * 0.9, (246, 226, 170), n=46, width=7)
    place(s, CX, CY, rx, ry, [("cube", 14, CARROT, 8), ("leaf", 8, (80, 150, 60), 8), ("blob", 20, (200, 140, 80), 4), ("disc", 12, (240, 236, 220), 6)])
    garnish(s, CX, CY, rx, ry, ["cilantro", "scallion", "chili"])
    props(s, ["chili", "lime"])


# ============================================================================ JAPANESE
def maki(s, x, y, r, filling=((140, 184, 70), (96, 156, 84)), outside=None):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, x, y, r, r, rgba((26, 34, 28)))
    E(d, x - 2, y - 3, r * 0.9, r * 0.88, rgba((40, 52, 42)))
    E(d, x, y, r * 0.84, r * 0.84, rgba((248, 244, 232)))
    for k in range(60):
        a = s.r.uniform(0, TAU)
        rr = math.sqrt(s.r.random()) * r * 0.8
        P(d, epoly(x + math.cos(a) * rr, y + math.sin(a) * rr, 5, 2.2, s.r.uniform(0, 3), 8), rgba((232, 226, 210)))
    E(d, x - r * 0.18, y, r * 0.22, r * 0.3, rgba(filling[0]))
    E(d, x + r * 0.2, y, r * 0.2, r * 0.26, rgba(filling[1]))
    s.put(lay, shadow=(5, 9, 8, 0.55), blur=0.4)


def sushi_scene(s, fills, rows=((420, 440), (610, 440), (800, 440), (420, 640), (610, 640), (800, 640)), extras=True):
    board(s, CX, 540, 460, 300, "dark")
    for i, (x, y) in enumerate(rows):
        maki(s, x - 20, y + 50, 78, fills[i % len(fills)])
    if extras:
        dollop(s, 330, 330, 22, (130, 190, 80))
        for k in range(3):
            shaded(s, "blob", 340 + k * 30, 270 + k * 8, 34, 14, 0.4, (244, 170, 170), wob=0.1, hl=0.2)
    prop_dip(s, 1020, 250, (60, 34, 22), 62)


@dish("sushi_roll", "slate")
def _(s):
    sushi_scene(s, [((140, 184, 70), (96, 156, 84))])


@dish("dish_114", "slate")  # Veg sushi rolls
def _(s):
    sushi_scene(s, [((230, 130, 40), (96, 156, 84)), ((140, 60, 120), (140, 184, 70)), ((240, 220, 90), (180, 60, 40))])


@dish("dish_072", "slate")  # Gyoza
def _(s):
    plate(s, CX, CY, 360, "dark")
    for (x, y, rot) in [(470, 430, 0.25), (630, 400, -0.15), (740, 520, 0.7), (600, 560, 0.05), (460, 600, -0.35), (720, 660, 0.4)]:
        dumpling(s, x, y, 88, rot, fried=True)
    sprinkle(s, CX, CY, 220, 200, 30, "ring", (110, 176, 70), (3, 5))
    props(s, [("dip", {"color": (60, 34, 22)}), "chili"])


@dish("dish_113", "wood")  # Chicken teriyaki bowl
def _(s):
    bowl_dish(s, "dark", 340, base=None, rice_=dict(color=(246, 242, 230), n=1200), items=[], pr=["lime"],
              after=lambda s, cx, cy, rx, ry: (
                  [shaded(s, "blob", cx - 60 + i * 52, cy - 40 + (i % 2) * 40, 52, 40, 0.4 * i, (140, 66, 28), wob=0.08, hl=0.3, edge=(80, 36, 16)) for i in range(6)],
                  [herb(s, cx + 120 + i * 20, cy + 90 - i * 30, 14, (60, 140, 56)) for i in range(3)],
                  garnish(s, cx, cy, rx, ry, ["sesame", "scallion"])))


@dish("dish_115", "slate")  # Miso ramen
def _(s):
    rx, ry = bowl(s, CX, CY, 350, "red")
    fill(s, CX, CY, rx, ry, (214, 164, 86), [(226, 180, 100), (196, 142, 70)], [(250, 226, 160)], nblob=100, nfleck=200)
    noodles(s, CX, CY, rx * 0.9, ry * 0.9, (246, 222, 150), n=48, width=7)
    for (x, y, rot) in [(470, 470, 0.4), (560, 410, -0.2)]:
        lay = s.lay()
        d = ImageDraw.Draw(lay)
        P(d, shape_pts("blob", x, y, 54, 40, rot), rgba((240, 236, 224)))
        E(d, x, y, 24, 20, rgba((248, 180, 40)))
        s.put(lay, shadow=(4, 7, 6, 0.5), blur=0.4)
    for (x, y, rot) in [(700, 430, 0.2), (720, 560, -0.3)]:
        shaded(s, "blob", x, y, 62, 44, rot, (216, 150, 120), wob=0.06, hl=0.3)
    shaded(s, "rect", 560, 650, 60, 40, 0.3, (24, 36, 28), wob=0.0, ex=4)
    place(s, CX, CY, rx, ry, [("ball", 8, (250, 214, 70), 8, {"mind": 1.5})])
    garnish(s, CX, CY, rx, ry, ["scallion", "sesame"])
    props(s, ["chili", "spoon"])


@dish("dish_116", "wood")  # Katsu curry
def _(s):
    plate(s, CX, CY, 370, "white")
    fill(s, 520, 540, 210, 190, (146, 84, 36), [(168, 100, 44), (120, 66, 28)], [(90, 50, 22)], nblob=80, nfleck=160)
    rice(s, 780, 470, 130, 120, n=800)
    for k in range(5):
        shaded(s, "rect", 450 + k * 62, 450 + k * 8, 34, 90, -0.1, (214, 150, 62), wob=0.03, ex=3.4, hl=0.3)
        sprinkle(s, 450 + k * 62, 450 + k * 8, 28, 80, 22, "dot", (160, 100, 40), (2, 3.4))
    place(s, 520, 590, 180, 90, [("cube", 12, CARROT, 5)], 1.2)
    herbs(s, [(640, 640)], 9)
    props(s, ["lemon", "chili"])


@dish("dish_117", "slate")  # Onigiri
def _(s):
    board(s, CX, CY, 400, 300, "dark")
    for (x, y, rot) in [(470, 480, 0.2), (640, 440, -0.2), (760, 590, 0.15)]:
        shaded(s, "tri", x, y, 112, 112, rot, (248, 244, 232), wob=0.03, edge=(204, 198, 182), hl=0.1)
        cr, sr = math.cos(rot), math.sin(rot)
        shaded(s, "rect", x + sr * 52, y + 40 + cr * 6, 62, 52, rot, (26, 38, 30), wob=0.0, ex=4, hl=0.15)
        sprinkle(s, x, y - 30, 40, 30, 14, "dot", (244, 234, 206), (1.4, 2))
    prop_dip(s, 1010, 240, (120, 60, 90), 52)
    props(s, [("leaf", {"color": (70, 120, 56)})])


# ============================================================================ ITALIAN
@dish("margherita_pizza", "wood")
def _(s):
    pizza(s, toppings=[("disc", 32, (250, 246, 232), 8)], basil=True, pr=["tomato", "leaf"])


@dish("dish_133", "wood")  # Margherita pizza (variant)
def _(s):
    pizza(s, sauce=(204, 60, 40), cheese=(250, 232, 170), toppings=[("disc", 28, (252, 250, 240), 10), ("leaf", 11, (50, 120, 44), 7)], basil=False, base_plate="dark", pr=["tomato", "garlic"])


@dish("dish_156", "slate")  # Butter garlic naan pizza
def _(s):
    pizza(s, r=300, crust=(226, 176, 100), toppings=[("slice", 15, (190, 40, 32), 7, {"edge": (130, 30, 22)}), ("ring", 11, (176, 76, 124), 6, {"thick": 0.12}), ("rect", 8, (80, 150, 56), 8)], pr=["garlic", "chili"])


@dish("dish_023", "wood")  # Garlic bread
def _(s):
    board(s, CX, CY, 420, 300, "wood")
    for (x, y, rot) in [(480, 440, -0.12), (700, 460, 0.1), (520, 620, 0.08)]:
        shaded(s, "rect", x, y, 210, 70, rot, (226, 172, 90), wob=0.03, ex=3, hl=0.3, edge=(160, 108, 48))
        sprinkle(s, x, y, 180, 50, 80, "leaf", (70, 140, 56), (1.4, 2.4))
        sprinkle(s, x, y, 180, 46, 22, "dot", (252, 236, 150), (4, 9), alpha=140)
    props(s, ["garlic", ("dip", {"color": (200, 60, 40)}), "leaf"])


@dish("dish_036", "marble")  # Caprese
def _(s):
    plate(s, CX, CY, 360, "white")
    for i, (x, y) in enumerate(ring_pts(CX, CY, 220, 210, 8)):
        slice_(s, x, y, 56, (200, 50, 36), (222, 70, 54), (246, 190, 120), i)
        a = TAU * i / 8 + 0.35
        slice_(s, CX + math.cos(a) * 170, CY + math.sin(a) * 160, 56, (240, 236, 224), (252, 250, 242))
    for (x, y) in ring_pts(CX, CY, 110, 100, 5, 0.3):
        leaf(s, x, y, 36, 20, s.r.uniform(0, TAU), (52, 124, 46))
    drizzle(s, CX, CY, 150, (50, 30, 26), "zigzag", 6, 200)
    sprinkle(s, CX, CY, 240, 230, 40, "dot", (24, 22, 20), (1, 1.8))
    props(s, ["tomato", "leaf"])


@dish("dish_076", "linen")  # Carbonara
def _(s):
    bowl_dish(s, "white", 345, base=(236, 222, 180), noodle=dict(color=(238, 212, 140), n=80, width=7), dish_plate=True,
              items=[("cube", 12, (210, 130, 100), 14), ("cube", 10, (160, 80, 60), 8)], garn=["pepper", "cheese", "parsley"],
              after=lambda s, cx, cy, rx, ry: E_yolk(s, cx, cy - 20), pr=["garlic", "leaf"])


@dish("dish_077", "wood")  # Lasagna
def _(s):
    board(s, CX, CY, 400, 330, "dark")
    slab(s, 560, 470, 230, 150, 0.08, [((250, 218, 130), 1), ((196, 60, 36), 1.4), ((246, 236, 206), 0.7), ((196, 60, 36), 1.4), ((246, 236, 206), 0.7), ((196, 60, 36), 1.4), ((250, 218, 130), 1)], crust=(210, 160, 70))
    sprinkle(s, 560, 340, 200, 30, 30, "dot", (200, 120, 44), (3, 6))
    herbs(s, [(480, 340), (620, 335)], 10, (52, 124, 46))
    props(s, ["tomato", "leaf"])


@dish("dish_136", "wood")  # Veg lasagna
def _(s):
    plate(s, CX, CY, 360, "white")
    slab(s, 570, 480, 230, 150, -0.05, [((250, 220, 140), 1), ((200, 64, 40), 1.2), ((100, 160, 70), 0.6), ((246, 236, 206), 0.7), ((200, 64, 40), 1.2), ((232, 160, 60), 0.6), ((250, 220, 140), 1)], crust=(210, 160, 70))
    herbs(s, [(500, 345), (640, 340)], 10, (52, 124, 46))
    props(s, ["tomato", "garlic"])


@dish("dish_078", "linen")  # Mushroom risotto
def _(s):
    bowl_dish(s, "white", 345, base=(240, 228, 196), blobs=[(246, 238, 214), (226, 210, 172)], flecks=[(252, 246, 228), (214, 190, 140)], dish_plate=True, nfleck=1200, fsize=(3, 7),
              items=[("disc", 24, (150, 108, 70), 7, {"mind": 1.6}), ("rect", 14, (170, 124, 84), 6)], garn=["parsley", "pepper", "cheese"], pr=["garlic", "leaf"])


@dish("dish_079", "wood")  # Bruschetta
def _(s):
    board(s, CX, CY, 420, 300, "wood")
    for (x, y, rot) in [(440, 430, 0.1), (620, 410, -0.1), (790, 450, 0.15), (520, 610, -0.08), (710, 620, 0.1)]:
        shaded(s, "blob", x, y, 86, 56, rot, (220, 164, 86), wob=0.05, hl=0.25, edge=(150, 100, 44))
        place(s, x, y, 70, 40, [("cube", 14, (206, 50, 38), 5, {"mind": 1.0}), ("blob", 8, (240, 236, 220), 3)], 1.0)
        herbs(s, [(x, y)], 12, (52, 124, 46))
    props(s, ["tomato", "garlic", "leaf"])


@dish("dish_080", "linen")  # Italian minestrone
def _(s):
    curry(s, "terra", sauce=pal((196, 80, 40), 0.9, 0.1, fleck=[(140, 50, 24), (240, 150, 90)]), items=[("ball", 12, (240, 232, 214), 16, {"mind": 1.2}), ("cube", 13, CARROT, 12), ("rect", 14, (96, 160, 64), 10), ("tri", 12, (226, 190, 100), 8)], garn=["parsley", "cheese", "pepper"], pr=["leaf", "tomato"])


@dish("classic_minestrone", "sage")
def _(s):
    curry(s, "white", sauce=pal((210, 96, 44), 0.9, 0.1, fleck=[(150, 54, 24), (244, 160, 96)]), items=[("ball", 12, (240, 232, 214), 14, {"mind": 1.2}), ("cube", 13, CARROT, 12), ("rect", 14, (96, 160, 64), 10), ("cube", 13, (230, 214, 150), 8)], garn=["basil", "cheese", "pepper"], pr=["spoon", "tomato"])


@dish("dish_134", "wood")  # Penne arrabbiata
def _(s):
    plate(s, CX, CY, 360, "white")
    fill(s, CX, CY, 270, 255, (190, 50, 30), [(214, 70, 40), (160, 38, 24)], [(120, 26, 18)], nblob=60, nfleck=80)
    for (x, y) in spots(s, CX, CY, 250, 240, 22, 72):
        a = s.r.uniform(0, TAU)
        shaded(s, "rect", x, y, 54, 20, a, (226, 170, 80), wob=0.0, ex=3.4, hl=0.25)
        cx2, cy2 = x + math.cos(a) * 52, y + math.sin(a) * 52
        shaded(s, "blob", cx2, cy2, 9, 20, a, (150, 70, 40), wob=0.0, hl=0.1, edge=(110, 50, 30), rim=0.7)
    herbs(s, spots(s, CX, CY, 200, 190, 4, 70), 12, (52, 124, 46))
    garnish(s, CX, CY, 260, 250, ["chili", "cheese"])
    props(s, ["chili", "garlic"])


# ============================================================================ MEXICAN
def taco(s, x, y, size, rot, filling, tortilla=(232, 196, 112)):
    shaded(s, "cres", x, y, size, size, rot, tortilla, wob=0.02, edge=(176, 130, 56), hl=0.25)
    cr, sr = math.cos(rot), math.sin(rot)
    for k in range(int(size / 16)):
        u = s.r.uniform(-0.7, 0.7) * size
        v = -s.r.uniform(0.05, 0.42) * size
        kind, sz, col = filling[k % len(filling)]
        px, py = x + u * cr - v * sr, y + u * sr + v * cr
        if kind == "leaf":
            leaf(s, px, py, sz * 1.7, sz * 0.8, s.r.uniform(0, 3), col, vein=False)
        else:
            shaded(s, "blob", px, py, sz, sz * 0.8, s.r.uniform(0, 3), jit(col, s.r, 10), wob=0.12)
    herbs(s, [(x + s.r.uniform(-0.4, 0.4) * size, y - 0.3 * size)], 9)


@dish("tacos_al_pastor", "terracotta")
def _(s):
    plate(s, CX, CY, 370, "dark")
    fill_ = [("b", 22, (140, 96, 60)), ("b", 14, (230, 220, 200)), ("leaf", 14, (70, 140, 56)), ("b", 16, (170, 120, 80))]
    for (x, y, rot) in [(470, 480, 0.1), (680, 450, -0.1), (580, 650, 0.0)]:
        taco(s, x, y + 60, 130, rot, fill_)
    props(s, ["lime", ("dip", {"color": (190, 50, 36)}), "chili"])


@dish("dish_132", "terracotta")  # Fish tacos
def _(s):
    plate(s, CX, CY, 370, "white")
    fill_ = [("b", 22, (222, 170, 90)), ("leaf", 14, (140, 190, 90)), ("b", 12, (190, 70, 110)), ("b", 14, (252, 246, 232))]
    for (x, y, rot) in [(470, 480, 0.1), (680, 450, -0.1), (580, 650, 0.0)]:
        taco(s, x, y + 60, 130, rot, fill_)
    props(s, ["lime", ("dip", {"color": (250, 246, 232)}), "chili"])


def quesadilla(s, cx, cy, r, filling=(246, 196, 70)):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, cx, cy, r, r * 0.95, rgba((170, 110, 50)))
    E(d, cx, cy, r * 0.96, r * 0.91, rgba((226, 178, 96)))
    for _ in range(40):
        a = s.r.uniform(0, TAU)
        rr = math.sqrt(s.r.random()) * r * 0.85
        E(d, cx + math.cos(a) * rr, cy + math.sin(a) * rr, s.r.uniform(8, 20), s.r.uniform(6, 14), rgba((150, 90, 38), 140))
    for k in range(3):
        a = k * math.pi / 3 + 0.2
        Ln(d, [(cx - math.cos(a) * r, cy - math.sin(a) * r * 0.95), (cx + math.cos(a) * r, cy + math.sin(a) * r * 0.95)], rgba((90, 52, 24), 150), 3)
    s.put(lay, shadow=(8, 14, 12, 0.6))
    for _ in range(7):
        a = s.r.uniform(0, TAU)
        shaded(s, "blob", cx + math.cos(a) * r * 0.96, cy + math.sin(a) * r * 0.9, 20, 12, a, filling, wob=0.1, hl=0.3)


@dish("dish_129", "terracotta")
def _(s):
    plate(s, CX, CY, 370, "dark")
    quesadilla(s, 540, 500, 210)
    prop_dip(s, 850, 380, (190, 50, 36), 70)
    prop_dip(s, 870, 600, (120, 170, 70), 70)
    herbs(s, [(540, 420), (590, 560)], 10)
    props(s, ["lime"])


@dish("chicken_quesadilla", "wood")
def _(s):
    plate(s, CX, CY, 370, "white")
    quesadilla(s, 560, 500, 220, (250, 210, 90))
    prop_dip(s, 880, 400, (248, 244, 232), 70)
    prop_dip(s, 880, 620, (190, 50, 36), 66)
    herbs(s, [(560, 440), (610, 580)], 10)
    props(s, ["lime", "chili"])


def nachos(s, extras, bg_plate=("white", 370)):
    plate(s, CX, CY, bg_plate[1], bg_plate[0])
    for (x, y) in spots(s, CX, CY, 290, 280, 26, 78):
        shaded(s, "tri", x, y, 56, 56, s.r.uniform(0, TAU), (232, 178, 70), wob=0.03, edge=(176, 120, 44), hl=0.2)
    fill_mask = s.ell_mask(CX, CY, 250, 240)
    ch = s.lay()
    d = ImageDraw.Draw(ch)
    for _ in range(40):
        a = s.r.uniform(0, TAU)
        rr = math.sqrt(s.r.random()) * 240
        E(d, CX + math.cos(a) * rr, CY + math.sin(a) * rr, s.r.uniform(18, 42), s.r.uniform(14, 34), rgba((244, 176, 50), 190))
    s.clipped(ch, fill_mask, blur=1.5)
    place(s, CX, CY, 290, 280, extras)


@dish("dish_086", "terracotta")  # Nachos supreme
def _(s):
    nachos(s, [("ring", 14, (80, 150, 56), 8, {"thick": 0.3}), ("cube", 14, (210, 54, 40), 8), ("ball", 12, (40, 30, 30), 8, {"mind": 1.3})])
    dollop(s, CX - 40, CY, 40, (250, 248, 238))
    dollop(s, CX + 70, CY + 40, 36, (110, 170, 70))
    herbs(s, spots(s, CX, CY, 250, 240, 5, 50), 10)
    props(s, ["lime", ("dip", {"color": (196, 50, 36)})])


@dish("dish_131", "terracotta")  # Loaded nachos
def _(s):
    nachos(s, [("ring", 14, (80, 150, 56), 7, {"thick": 0.3}), ("ball", 11, (110, 60, 40), 10), ("cube", 13, (210, 54, 40), 6), ("ball", 10, (240, 220, 90), 8, {"mind": 1.3})], ("dark", 370))
    dollop(s, CX + 10, CY - 10, 44, (250, 248, 238))
    herbs(s, spots(s, CX, CY, 250, 240, 6, 50), 10)
    props(s, ["lime", ("dip", {"color": (110, 170, 70)})])


@dish("dish_084", "wood")  # Enchiladas
def _(s):
    board(s, CX, CY, 410, 320, "dark")
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    P(d, shape_pts("rect", CX, CY, 340, 250, 0, ex=10), rgba((176, 176, 180)))
    P(d, shape_pts("rect", CX, CY, 326, 236, 0, ex=10), rgba((226, 226, 230)))
    s.put(lay, shadow=(8, 14, 12, 0.6))
    for k in range(3):
        y = 400 + k * 90
        shaded(s, "rect", CX - 235, y, 38, 44, 0.0, (236, 214, 160), wob=0.04, ex=3)
        shaded(s, "rect", CX + 235, y, 38, 44, 0.0, (236, 214, 160), wob=0.04, ex=3)
        shaded(s, "rect", CX, y, 250, 52, 0.0, (204, 66, 38), wob=0.03, ex=3.4, hl=0.2, edge=(150, 40, 24))
        sprinkle(s, CX, y, 230, 34, 60, "dot", (250, 214, 100), (4, 9), alpha=210)
    dollop(s, CX - 100, CY, 34, (250, 248, 238))
    dollop(s, CX + 110, CY + 60, 34, (250, 248, 238))
    herbs(s, spots(s, CX, CY, 200, 120, 6, 50), 10)
    props(s, ["lime", "chili"])


@dish("dish_085", "slate")  # Chili con carne
def _(s):
    curry(s, "dark", sauce=pal((126, 44, 26), fleck=[(80, 26, 16), (190, 80, 50)]), items=[("ball", 14, (90, 36, 30), 18, {"mind": 1.1}), ("blob", 10, (80, 50, 36), 20, {"mind": 1.0}), ("cube", 12, (210, 54, 40), 8)], garn=["cilantro", "cheese", "chili"], dollops=[(0, -10, 38, (250, 248, 238))], pr=["lime", "chili"])


@dish("dish_087", "terracotta")  # Guacamole toast
def _(s):
    board(s, CX, CY, 400, 300, "wood")
    for (x, y, rot) in [(480, 480, -0.08), (700, 520, 0.08)]:
        shaded(s, "rect", x, y, 150, 130, rot, (214, 164, 90), wob=0.02, ex=6, hl=0.25, edge=(150, 100, 44))
        fill_ = s.lay()
        P(ImageDraw.Draw(fill_), shape_pts("rect", x, y, 128, 108, rot, ex=7), rgba((136, 176, 70)))
        s.put(fill_, shadow=(2, 3, 3, 0.3))
        sprinkle(s, x, y, 100, 80, 120, "dot", (84, 130, 50), (3, 6))
        place(s, x, y, 100, 80, [("cube", 8, (206, 50, 36), 4, {"mind": 1.2})], 1)
        herbs(s, [(x - 20, y + 10), (x + 24, y - 20)], 9)
    sprinkle(s, CX, CY, 300, 200, 40, "dot", (196, 44, 28), (1.5, 3))
    props(s, ["lime", "tomato", "chili"])


@dish("dish_088", "slate")  # Fajitas
def _(s):
    rx, ry = pan(s, 520, 500, 310, "dark")
    fill(s, 520, 500, rx, ry, (66, 48, 38), [(90, 62, 44), (50, 36, 30)], [(120, 80, 50)], nblob=60, nfleck=200)
    place(s, 520, 500, rx, ry, [("rect", 22, (214, 150, 74), 12), ("rect", 14, (226, 70, 40), 10), ("rect", 14, (80, 150, 56), 10), ("rect", 14, (232, 190, 50), 8), ("ring", 14, (176, 76, 124), 6, {"thick": 0.14})], 1.0)
    garnish(s, 520, 500, rx, ry, ["cilantro", "pepper"])
    for (x, y) in [(900, 360), (930, 560)]:
        shaded(s, "blob", x, y, 100, 98, 0, (240, 214, 150), wob=0.03, edge=(200, 160, 90), hl=0.1)
    props(s, ["lime", ("dip", {"color": (250, 248, 238)})])


@dish("dish_042", "terracotta")  # Chicken tinga tostadas
def _(s):
    plate(s, CX, CY, 370, "dark")
    for (x, y) in [(470, 450), (680, 440), (570, 640)]:
        shaded(s, "blob", x, y, 110, 106, 0, (226, 170, 80), wob=0.03, edge=(170, 120, 50), hl=0.2)
        place(s, x, y, 90, 88, [("blob", 14, (186, 70, 36), 14, {"mind": 0.9}), ("leaf", 8, (100, 170, 70), 5)], 1.0)
        dollop(s, x, y, 24, (250, 248, 238))
        sprinkle(s, x, y, 80, 76, 20, "dot", (250, 226, 150), (2, 4))
        herbs(s, [(x + 30, y - 20)], 8)
    props(s, ["lime", "chili"])


@dish("dish_130", "wood")  # Veg burrito bowl
def _(s):
    rx, ry = bowl(s, CX, CY, 345, "white")
    fill(s, CX, CY, rx, ry, (246, 240, 226), (), (), gloss=False)
    sectors = [(rice, None), ]
    # sector layout: rice, beans, corn, salsa, lettuce, guac
    cols = [((250, 246, 232), "rice"), ((92, 40, 30), "beans"), ((244, 206, 70), "corn"), ((200, 60, 40), "salsa"), ((120, 180, 70), "lettuce"), ((130, 176, 70), "guac")]
    for i, (c, name) in enumerate(cols):
        a = TAU * i / 6
        x, y = CX + math.cos(a) * 140, CY + math.sin(a) * 130
        if name == "rice":
            rice(s, x, y, 100, 92, n=240)
        else:
            fill(s, x, y, 96, 90, c, [shade(c, 0.85)], [lighten(c, 0.2)], nblob=20, nfleck=120, gloss=False, fsize=(3, 6))
    dollop(s, CX, CY, 46, (250, 248, 238))
    herbs(s, [(CX + 20, CY - 60), (CX - 40, CY + 50)], 10)
    props(s, ["lime", "tomato"])


# ============================================================================ HEALTHY / SALADS
def salad(s, leaves, items, garn=(), vessel="white", r=345, base=None, pr=()):
    rx, ry = bowl(s, CX, CY, r, vessel)
    fill(s, CX, CY, rx * 0.97, ry * 0.97, (150, 176, 100), (), (), gloss=False, nblob=0, nfleck=0)
    # a dense heap of leaves first, then the toppings on top
    place(s, CX, CY, rx * 1.02, ry * 1.02, [(k, sz, c, int(n * 14), {"mind": 0.28}) for (k, sz, c, n, *o) in leaves], 1.0)
    place(s, CX, CY, rx, ry, items, 1.45)
    garnish(s, CX, CY, rx, ry, garn)
    props(s, pr)


@dish("caesar_salad", "marble")
def _(s):
    salad(s, [("leaf", 30, (150, 198, 90), 12), ("leaf", 28, (200, 226, 140), 8)], [("cube", 15, (216, 164, 90), 10)], ["cheese", "pepper"], pr=["lemon", "garlic"])


@dish("greek_salad", "linen")
def _(s):
    salad(s, [("leaf", 24, (170, 206, 110), 6)], [("blob", 26, (222, 62, 46), 8), ("slice", 24, (190, 220, 150), 8, {"edge": (60, 120, 50)}), ("ring", 18, (176, 76, 124), 6, {"thick": 0.14}),
                                                ("ball", 12, (60, 40, 60), 8, {"mind": 1.3}), ("cube", 30, (252, 250, 240), 2), ("ring", 16, (90, 160, 60), 4, {"thick": 0.14})], ["oregano"], pr=["lemon", "tomato"])


@dish("dish_038", "marble")  # Grilled chicken salad
def _(s):
    salad(s, [("leaf", 28, (130, 190, 80), 10)], [("rect", 30, (206, 150, 80), 5), ("slice", 18, (222, 70, 54), 6, {"edge": (180, 40, 30)}), ("slice", 17, (190, 220, 150), 6, {"edge": (60, 120, 50)}), ("blob", 20, (170, 200, 90), 5)], ["pepper", "sesame"], pr=["lemon", "tomato"])


@dish("dish_099", "sage")  # Protein sprout salad
def _(s):
    salad(s, [("leaf", 20, (140, 190, 90), 4)], [("blob", 12, (214, 190, 110), 20, {"mind": 1.0}), ("cube", 14, (222, 70, 54), 8), ("cube", 12, (190, 220, 150), 8), ("ball", 8, (176, 36, 60), 16, {"mind": 1.1}), ("ball", 12, (240, 214, 120), 10)], ["cilantro", "pepper"], pr=["lemon", "chili"], base=(224, 232, 196))


@dish("dish_096", "marble")  # Quinoa salad
def _(s):
    bowl_dish(s, "white", 345, base=(186, 150, 96), rice_=dict(color=(226, 198, 142), n=4200, grain=(3.4, 2.6), accent=[(176, 130, 80), (244, 226, 180)], accent_n=0.3),
              items=[("cube", 14, (222, 70, 54), 8), ("cube", 13, (190, 220, 150), 8), ("ball", 12, (240, 200, 90), 8), ("ball", 8, (176, 36, 60), 12, {"mind": 1.1})], garn=["parsley", "pepper"], pr=["lemon", "tomato"])


@dish("dish_167", "sage")  # Couscous salad
def _(s):
    bowl_dish(s, "white", 345, base=(206, 176, 110), rice_=dict(color=(242, 222, 164), n=5200, grain=(2.6, 2.6), accent=[(222, 190, 120), (252, 240, 200)], accent_n=0.3),
              items=[("cube", 13, (222, 70, 54), 8), ("cube", 12, (190, 220, 150), 8), ("ball", 11, (226, 180, 100), 10), ("cube", 12, (250, 250, 240), 4)], garn=["parsley", "mint"], pr=["lemon", "tomato"])


def grain_bowl(s, cols, center, vessel="white", pr=()):
    rx, ry = bowl(s, CX, CY, 350, vessel)
    fill(s, CX, CY, rx, ry, (246, 240, 226), (), (), gloss=False)
    n = len(cols)
    for i, (name, c, kind) in enumerate(cols):
        a = TAU * i / n - 1.2
        x, y = CX + math.cos(a) * 150, CY + math.sin(a) * 140
        if kind == "rice":
            rice(s, x, y, 100, 94, color=c, n=260, grain=(5, 2))
        elif kind == "cubes":
            fill(s, x, y, 98, 92, shade(c, 0.9), (), (), gloss=False, nfleck=0)
            place(s, x, y, 100, 94, [("cube", 18, c, 7)], 1.3)
        elif kind == "leaf":
            fill(s, x, y, 98, 92, shade(c, 0.8), (), (), gloss=False, nfleck=0)
            place(s, x, y, 100, 94, [("leaf", 16, c, 6)], 1.3)
        elif kind == "slices":
            fill(s, x, y, 98, 92, (246, 240, 226), (), (), gloss=False, nfleck=0)
            for k in range(4):
                shaded(s, "rect", x - 50 + k * 34, y, 18, 56, 0.3, c, wob=0.04, ex=3, hl=0.3)
        else:
            fill(s, x, y, 98, 92, c, [shade(c, 0.88)], [lighten(c, 0.25)], gloss=False, nblob=20, nfleck=100, fsize=(3, 6))
    center(s, CX, CY)
    props(s, pr)


@dish("dish_097", "sage")  # Buddha bowl
def _(s):
    grain_bowl(s, [("quinoa", (232, 206, 150), "rice"), ("sweetpotato", (232, 130, 50), "cubes"), ("greens", (80, 160, 70), "leaf"), ("avocado", (150, 190, 80), "slices"), ("chickpeas", (222, 176, 96), "mash"), ("cabbage", (140, 70, 130), "mash")],
               lambda s, x, y: (sprinkle(s, x, y, 70, 64, 40, "dot", (244, 234, 206), (1.5, 2.4)), herbs(s, [(x, y)], 10)), pr=["lemon", "tomato"])


@dish("dish_147", "marble")  # Quinoa buddha bowl
def _(s):
    grain_bowl(s, [("quinoa", (240, 224, 170), "rice"), ("broccoli", (70, 150, 60), "leaf"), ("carrot", (232, 124, 40), "cubes"), ("avocado", (150, 190, 80), "slices"), ("beans", (150, 70, 50), "mash"), ("corn", (246, 208, 70), "mash")],
               lambda s, x, y: (shaded(s, "blob", x, y, 46, 40, 0, (252, 250, 244), wob=0.1, hl=0.05), E_yolk(s, x, y)), pr=["lime", "chili"])


@dish("dish_157", "slate")  # Bibimbap
def _(s):
    rx, ry = bowl(s, CX, CY, 350, "dark")
    fill(s, CX, CY, rx, ry, (248, 244, 232), (), (), gloss=False)
    rice(s, CX, CY, rx * 0.95, ry * 0.95, n=400)
    cols = [(( 126, 176, 70), "leaf"), ((232, 124, 40), "rect"), ((120, 70, 110), "rect"), ((150, 100, 60), "blob"), ((236, 220, 150), "rect"), ((190, 50, 36), "blob")]
    for i, (c, kind) in enumerate(cols):
        a = TAU * i / 6 - 0.3
        x, y = CX + math.cos(a) * 150, CY + math.sin(a) * 140
        if kind == "leaf":
            place(s, x, y, 80, 70, [("leaf", 14, c, 4)], 1.2)
        elif kind == "rect":
            for k in range(6):
                shaded(s, "rect", x - 40 + k * 16, y + (k % 2) * 6, 7, 40, 0.5, jit(c, s.r, 8), wob=0.0, ex=3, hl=0.2, shadow=(1, 2, 2, 0.4))
        else:
            place(s, x, y, 70, 60, [("blob", 12, c, 5, {"mind": 1.0})], 1.0)
    shaded(s, "blob", CX, CY, 56, 52, 0, (252, 250, 244), wob=0.12, hl=0.05)
    E_yolk(s, CX, CY)
    dollop(s, CX + 110, CY - 100, 20, (190, 50, 36))
    garnish(s, CX, CY, rx, ry, ["sesame"])
    props(s, ["chili", "spoon"])


@dish("dish_032", "marble")  # Oatmeal berry bowl
def _(s):
    bowl_dish(s, "white", 340, base=(222, 200, 164), blobs=[(232, 212, 180), (206, 182, 142)], flecks=[(190, 160, 110), (244, 232, 206)], nfleck=500, fsize=(2, 4),
              items=[("ball", 14, (60, 70, 140), 10, {"mind": 1.2}), ("disc", 18, (252, 232, 150), 5), ("ball", 14, (196, 40, 70), 6), ("blob", 12, (200, 150, 90), 6)], garn=["sesame"],
              after=lambda s, cx, cy, rx, ry: drizzle(s, cx, cy, 120, (230, 160, 40), "swirl", 6, 200), pr=["spoon", "lemon"])


@dish("dish_100", "sage")  # Protein smoothie bowl
def _(s):
    rx, ry = bowl(s, CX, CY, 345, "white")
    fill(s, CX, CY, rx, ry, (170, 80, 140), [(190, 100, 156), (146, 60, 120)], [(214, 150, 190)], nblob=100, nfleck=100)
    for i in range(5):
        x = 420 + i * 90
        slice_(s, x, 400, 32, (252, 240, 190), (252, 246, 214))
    place(s, CX, 590, 230, 70, [("ball", 12, (60, 70, 140), 12, {"mind": 1.2}), ("ball", 12, (196, 40, 70), 8)], 1.2)
    sprinkle(s, CX, 500, 200, 40, 80, "dot", (244, 234, 206), (1.6, 2.6))
    herbs(s, [(CX + 160, 470)], 10, (80, 150, 70))
    props(s, ["spoon", "lemon"])


@dish("dish_050", "marble")  # Honey fruit salad
def _(s):
    bowl_dish(s, "white", 340, base=(248, 236, 200), nfleck=0, nblob=40, blobs=[(252, 244, 216)], gloss=False,
              items=[("cube", 28, (214, 50, 56), 5), ("cube", 26, (250, 150, 50), 5), ("slice", 24, (120, 170, 60), 5, {"edge": (90, 140, 50)}), ("ball", 16, (136, 190, 70), 8), ("ball", 14, (50, 60, 130), 10), ("cube", 26, (252, 230, 100), 5)],
              after=lambda s, cx, cy, rx, ry: drizzle(s, cx, cy, 130, (232, 168, 40), "zigzag", 6, 200), pr=["lemon", "spoon"])


@dish("dish_098", "sage")  # Grilled fish with veggies
def _(s):
    plate(s, CX, CY, 370, "white")
    shaded(s, "rect", 540, 480, 200, 80, -0.2, (232, 200, 150), wob=0.04, ex=3, hl=0.3, edge=(190, 150, 100))
    marks = s.lay()
    d = ImageDraw.Draw(marks)
    for k in range(-3, 4):
        Ln(d, [(540 + k * 44 - 20, 420), (540 + k * 44 + 30, 540)], rgba((110, 70, 40), 190), 7)
    s.clipped(marks, s.poly_mask(shape_pts("rect", 540, 480, 190, 70, -0.2, ex=3)))
    for k in range(6):
        shaded(s, "rect", 700 + k * 6, 360 + k * 40, 90, 8, 0.2, (90, 160, 60), wob=0.0, ex=6, hl=0.3)
    place(s, 700, 650, 100, 60, [("slice", 20, (232, 140, 50), 5), ("ball", 14, (220, 60, 46), 4)], 1.1)
    prop_lemon_wedge = None
    props(s, ["lemon", "leaf"])


@dish("dish_040", "marble")  # Veg clear soup
def _(s):
    curry(s, "white", sauce=CLEARB, items=[("cube", 11, CARROT, 12, {"mind": 1.3}), ("rect", 10, (96, 160, 64), 10), ("ball", 8, (236, 220, 140), 10), ("slice", 11, (200, 220, 160), 6)], garn=["scallion", "pepper"], pr=["spoon", "lemon"])


@dish("dish_039", "wood")  # Red lentil soup
def _(s):
    curry(s, "terra", sauce=pal((226, 120, 44), 0.9, 0.1, fleck=[(190, 80, 30), (250, 180, 100)]), items=[], swirl=True, swirl_color=(250, 246, 234), garn=["cumin", "parsley", "paprika"], pr=["lemon", "spoon"])


@dish("dish_033", "wood")  # Scrambled eggs toast
def _(s):
    plate(s, CX, CY, 360, "white")
    for (x, y, rot) in [(450, 480, -0.1), (680, 520, 0.12)]:
        toast(s, x, y, 120, rot, marks=False)
        for k in range(5):
            shaded(s, "blob", x + s.r.uniform(-60, 60), y + s.r.uniform(-50, 50), 36, 28, s.r.uniform(0, 3), (250, 212, 70), wob=0.2, hl=0.3)
    sprinkle(s, CX, CY, 260, 120, 24, "ring", (80, 160, 60), (2.5, 4))
    place(s, 600, 700, 120, 40, [("disc", 18, (222, 60, 46), 4)], 1)
    props(s, ["tomato", "chili"])


@dish("avocado_toast", "marble")
def _(s):
    board(s, CX, CY, 400, 300, "wood")
    for (x, y, rot) in [(470, 480, -0.1), (700, 520, 0.1)]:
        toast(s, x, y, 125, rot, marks=False, color=(206, 156, 84))
        for k in range(4):
            shaded(s, "rect", x - 54 + k * 34, y + s.r.uniform(-8, 8), 22, 70, 0.3, (146, 190, 76), wob=0.06, ex=3, hl=0.3)
        sprinkle(s, x, y, 90, 80, 30, "dot", (196, 44, 28), (1.5, 2.6))
        sprinkle(s, x, y, 90, 80, 50, "dot", (244, 234, 206), (1.4, 2))
    place(s, 600, 720, 200, 40, [("slice", 20, (212, 60, 44), 4, {"edge": (170, 30, 24)})], 1)
    props(s, ["lemon", "chili", "leaf"])


@dish("dish_034", "linen")  # Banana honey smoothie
def _(s):
    glass(s, CX, CY, 260, (244, 222, 150), foam=(252, 244, 214), straw=(240, 190, 60))
    for k in range(3):
        slice_(s, 880 + k * 40, 760 - k * 20, 44, (248, 232, 150), (252, 244, 200))
    dollop(s, CX, CY, 40, (252, 246, 224))
    drizzle(s, CX, CY, 80, (230, 160, 40), "swirl", 5, 210)
    props(s, ["spoon", "lemon"])


# ============================================================================ COMFORT
@dish("ginger_tea", "wood")
def _(s):
    mug(s, 560, 520, 250, (186, 112, 56), vessel="cream")
    for k in range(3):
        slice_(s, 900, 640 + k * 70, 44, (206, 160, 90), (240, 214, 150))
    for k in range(3):
        shaded(s, "blob", 190 + k * 40, 760, 18, 12, 0.4, (130, 170, 80), wob=0.06, hl=0.3)
    props(s, ["cinnamon", ("spice", {"color": (180, 130, 70)})])


@dish("matcha_latte", "marble")
def _(s):
    rx, ry = mug(s, 560, 520, 250, (132, 168, 80), vessel="white")
    latte_art(s, 560, 520, 170)
    prop_dip(s, 1010, 240, (110, 160, 60), 62, "white")
    props(s, [])


@dish("pancakes", "wood")
def _(s):
    plate(s, CX, CY, 370, "white")
    stack(s, CX, 470, 200, 4, (222, 164, 84), edge=(196, 140, 70), top=(210, 148, 70), offset=28)
    syrup = s.lay()
    d = ImageDraw.Draw(syrup)
    E(d, CX, 450, 150, 120, rgba((150, 70, 20), 190))
    s.clipped(syrup, s.ell_mask(CX, 460, 200, 170), blur=6)
    shaded(s, "rect", CX, 430, 46, 36, 0.2, (252, 232, 130), wob=0.0, ex=4)
    place(s, 600, 700, 150, 50, [("disc", 24, (206, 40, 52), 3), ("ball", 12, (50, 60, 130), 6, {"mind": 1.2})], 1.1)
    props(s, [("dip", {"color": (150, 70, 20)}), "spoon"])


@dish("dish_048", "wood")  # Belgian waffles
def _(s):
    plate(s, CX, CY, 370, "white")
    waffle(s, CX, CY - 10, 230)
    sprinkle(s, CX, CY, 200, 190, 120, "dot", (252, 250, 244), (1.5, 3), alpha=150)
    dollop(s, CX - 30, CY - 20, 52, (252, 250, 244))
    for (x, y) in [(450, 400), (700, 420), (680, 620)]:
        shaded(s, "blob", x, y, 38, 32, 0.3, (206, 40, 52), wob=0.08, hl=0.35)
    drizzle(s, CX, CY, 150, (150, 70, 20), "zigzag", 7, 200)
    props(s, [("dip", {"color": (150, 70, 20)}), "spoon"])


@dish("dish_035", "wood")  # Grilled cheese
def _(s):
    plate(s, CX, CY, 360, "white")
    for (x, y, rot) in [(470, 500, -0.35), (700, 470, 0.4)]:
        slab(s, x, y, 140, 90, rot, [((206, 150, 76), 0.7), ((250, 204, 70), 0.7), ((244, 190, 60), 0.5), ((250, 204, 70), 0.7), ((206, 150, 76), 0.7)], crust=(176, 120, 54))
    sprinkle(s, CX, CY, 250, 150, 30, "dot", (150, 90, 30), (1.5, 2.6))
    props(s, ["tomato", "leaf"])


@dish("dish_049", "wood")  # Club sandwich
def _(s):
    board(s, CX, CY, 420, 310, "wood")
    for (x, y, rot) in [(470, 480, -0.1), (700, 470, 0.1)]:
        slab(s, x, y, 140, 110, rot, [((226, 190, 120), 0.5), ((120, 180, 70), 0.3), ((220, 70, 56), 0.3), ((236, 190, 150), 0.6), ((226, 190, 120), 0.5), ((246, 226, 120), 0.3), ((226, 190, 120), 0.5)], crust=(190, 140, 70))
        shaded(s, "blob", x, y - 100, 6, 6, 0, (190, 40, 40), wob=0.0)
    fries(s, 600, 720, 100, 6, ln=80)
    props(s, ["tomato", "leaf"])


@dish("dish_165", "slate")  # Veg club sandwich grilled
def _(s):
    board(s, CX, CY, 420, 310, "dark")
    for (x, y, rot) in [(470, 480, -0.1), (700, 470, 0.1)]:
        slab(s, x, y, 140, 110, rot, [((214, 160, 84), 0.5), ((120, 180, 70), 0.3), ((220, 70, 56), 0.3), ((250, 220, 130), 0.3), ((226, 130, 50), 0.25), ((214, 160, 84), 0.5), ((150, 190, 80), 0.25), ((214, 160, 84), 0.5)], crust=(170, 116, 50))
    fries(s, 600, 730, 100, 6, ln=80)
    props(s, ["tomato", ("dip", {"color": (196, 44, 30)})])


@dish("dish_024", "wood")  # Creamy tomato soup
def _(s):
    curry(s, "white", sauce=pal((218, 88, 52), 0.9, 0.08, fleck=[(180, 56, 34), (246, 150, 110)]), swirl=True, garn=["basil", "pepper"], pr=["tomato", "spoon"])


@dish("dish_025", "wood")  # Classic chicken soup
def _(s):
    curry(s, "white", sauce=CLEARB, items=[("blob", 22, (236, 214, 170), 4), ("cube", 12, CARROT, 10, {"mind": 1.3}), ("ball", 10, (240, 220, 150), 8), ("slice", 11, (200, 220, 160), 5)], garn=["parsley", "pepper"], noodle=dict(color=(240, 214, 140), n=26, width=6), pr=["lemon", "spoon"])


@dish("dish_144", "wood")  # Mac and cheese
def _(s):
    rx, ry = bowl(s, CX, CY, 340, "white")
    fill(s, CX, CY, rx, ry, (246, 180, 40), [(250, 200, 70), (226, 150, 30)], [(252, 224, 120)], nblob=100, nfleck=200)
    for (x, y) in spots(s, CX, CY, rx * 0.85, ry * 0.85, 46, 56):
        a = s.r.uniform(0, TAU)
        shaded(s, "rect", x, y, 30, 12, a, (244, 196, 80), wob=0.0, ex=3, hl=0.25, edge=(206, 140, 40))
    sprinkle(s, CX, CY, rx * 0.8, ry * 0.8, 100, "dot", (214, 150, 70), (2, 4))
    garnish(s, CX, CY, rx, ry, ["parsley", "paprika"])
    props(s, ["spoon", "chili"])


@dish("dish_145", "linen")  # Moong dal khichdi
def _(s):
    bowl_dish(s, "terra", 335, base=(236, 204, 100), blobs=[(246, 220, 124), (216, 180, 76)], flecks=[(250, 232, 150), (190, 150, 60)], nfleck=700, fsize=(2, 5),
              items=[("cube", 10, (232, 150, 50), 6, {"mind": 1.6})], garn=["cumin", "cilantro", "chili"],
              after=lambda s, cx, cy, rx, ry: shaded(s, "rect", cx, cy, 40, 30, 0.2, (252, 236, 150), wob=0.0, ex=4), pr=["lemon", "chili"])


@dish("dish_146", "wood")  # Tomato soup with grilled cheese
def _(s):
    bowl_dish(s, "white", 240, base=pal((218, 88, 52))["base"], blobs=pal((218, 88, 52))["blobs"], flecks=pal((218, 88, 52))["flecks"], cx=410, cy=500, swirl=True, garn=["basil"])
    for (x, y, rot) in [(820, 400, 0.4), (840, 620, -0.3)]:
        slab(s, x, y, 120, 80, rot, [((206, 150, 76), 0.7), ((250, 204, 70), 0.7), ((244, 190, 60), 0.5), ((250, 204, 70), 0.7), ((206, 150, 76), 0.7)], crust=(176, 120, 54))
    props(s, ["tomato", "leaf"])


@dish("dish_030", "wood")  # Spicy chicken wrap
def _(s):
    board(s, CX, CY, 420, 310, "wood")
    roll(s, 540, 460, 250, 58, -0.12, filling=((226, 190, 120), (120, 170, 70), (200, 60, 40)))
    roll(s, 600, 640, 240, 56, 0.1, filling=((226, 190, 120), (120, 170, 70), (200, 60, 40)))
    props(s, ["chili", ("dip", {"color": (196, 50, 36)}), "lime"])


@dish("dish_164", "slate")  # Chocolate mug cake
def _(s):
    rx, ry = mug(s, 560, 520, 250, (70, 40, 28), vessel="white")
    fill(s, 560, 520, rx * 0.95, ry * 0.95, (86, 48, 30), [(110, 64, 40), (60, 32, 22)], [(140, 90, 60), (40, 22, 16)], nblob=120, nfleck=500, fsize=(3, 7))
    sprinkle(s, 560, 520, 200, 190, 160, "dot", (252, 250, 244), (1.5, 3), alpha=170)
    shaded(s, "blob", 540, 500, 46, 40, 0, (200, 30, 56), wob=0.08, hl=0.35)
    drizzle(s, 560, 520, 120, (50, 26, 18), "swirl", 6, 220)
    props(s, ["spoon", "napkin"])


@dish("dish_161", "terracotta")  # Rasam rice
def _(s):
    bowl_dish(s, "steel", 230, base=(214, 120, 40), blobs=[(230, 140, 56), (190, 96, 30)], flecks=[(110, 80, 30), (240, 190, 100)], cx=400, cy=500, garn=["cumin", "cilantro", "chili"],
              items=[("leaf", 6, (70, 130, 50), 6)])
    rice(s, 810, 500, 190, 170, n=1000)
    shaded(s, "rect", 810, 480, 36, 26, 0.2, (252, 236, 150), wob=0.0, ex=4)
    herbs(s, [(780, 520)], 9)
    props(s, ["chili", "lemon"])


# ============================================================================ FAST FOOD
@dish("french_fries", "slate")
def _(s):
    paper_tray(s, CX, CY, 400, 320, 0.0)
    fries(s, CX, CY, 220, 40, ln=130, wd=11)
    sprinkle(s, CX, CY, 300, 220, 60, "dot", (252, 250, 244), (1, 2), alpha=180)
    props(s, [("dip", {"color": (200, 40, 30)}), "tomato"])


@dish("dish_150", "slate")  # Peri peri loaded fries
def _(s):
    plate(s, CX, CY, 370, "dark")
    fries(s, CX, CY, 220, 36, ln=110)
    drizzle(s, CX, CY, 180, (210, 60, 30), "zigzag", 12, 220)
    sprinkle(s, CX, CY, 230, 200, 80, "strip", (250, 226, 150), (1.6, 2.6))
    place(s, CX, CY, 260, 230, [("ring", 12, (80, 150, 56), 6, {"thick": 0.3}), ("ball", 8, (176, 40, 30), 8, {"mind": 1.3})], 1.2)
    garnish(s, CX, CY, 250, 230, ["scallion", "paprika"])
    props(s, ["chili", ("dip", {"color": (250, 248, 238)})])


@dish("dish_043", "wood")  # Veg burger
def _(s):
    burger(s, patty=(196, 150, 70), extras=("lettuce", "tomato", "cheese", "onion"), pr=["tomato", "leaf"])


@dish("dish_044", "wood")  # Crispy chicken burger
def _(s):
    burger(s, patty=(214, 160, 70), extras=("lettuce", "tomato"), pr=["chili", ("dip", {"color": (250, 248, 238)})])


@dish("dish_047", "slate")  # Onion rings
def _(s):
    plate(s, CX, CY, 360, "white")
    for (x, y) in spots(s, CX, CY, 260, 250, 9, 120):
        ring_item(s, x, y, 60, (216, 160, 70), 0.3)
    props(s, [("dip", {"color": (200, 40, 30)}), "lemon"])


# ============================================================================ STREET FOOD / MISC INDIAN
@dish("dish_089", "wood")  # Bhel puri
def _(s):
    paper_tray(s, CX, CY, 360, 300, 0.1, (236, 190, 80))
    fill(s, CX, CY, 260, 220, (236, 210, 150), [(246, 226, 176)], [(250, 236, 190)], nblob=40, nfleck=800, fsize=(3, 7), gloss=False)
    sprinkle(s, CX, CY, 250, 210, 120, "strip", (236, 196, 100), (1.4, 2))
    place(s, CX, CY, 250, 210, [("cube", 8, (226, 70, 46), 14, {"mind": 1.3}), ("ring", 8, (176, 76, 124), 8, {"thick": 0.2}), ("ball", 8, (214, 170, 100), 10, {"mind": 1.3})], 1)
    drizzle(s, CX, CY, 150, (96, 150, 60), "zigzag", 8, 180)
    drizzle(s, CX, CY + 10, 150, (120, 66, 30), "zigzag", 8, 180)
    garnish(s, CX, CY, 250, 210, ["cilantro", "pomegranate"])
    props(s, ["lemon", "chili"])


@dish("dish_090", "wood")  # Sev puri
def _(s):
    plate(s, CX, CY, 370, "white")
    for (x, y) in spots(s, CX, CY, 250, 240, 8, 118):
        shaded(s, "blob", x, y, 56, 54, 0, (222, 172, 86), wob=0.04, edge=(170, 120, 50), hl=0.2)
        place(s, x, y, 40, 40, [("cube", 7, (226, 70, 46), 3, {"mind": 1}), ("ball", 7, (246, 230, 170), 3, {"mind": 1})], 1)
        sprinkle(s, x, y, 44, 44, 40, "strip", (236, 196, 100), (1.1, 1.6))
        sprinkle(s, x, y, 30, 30, 3, "dot", (96, 150, 60), (4, 6))
    props(s, ["lemon", ("dip", {"color": (96, 150, 60)})])


@dish("dish_091", "wood")  # Dabeli
def _(s):
    board(s, CX, CY, 420, 310, "wood")
    for (x, y) in [(450, 470), (600, 440), (750, 480), (530, 620)]:
        shaded(s, "blob", x, y, 78, 62, 0, (222, 168, 86), wob=0.04, edge=(166, 116, 50), hl=0.3)
        place(s, x, y - 6, 54, 38, [("ball", 8, (174, 120, 70), 5, {"mind": 1}), ("ball", 6, (176, 36, 60), 4, {"mind": 1}), ("ball", 7, (214, 170, 100), 3)], 1)
        sprinkle(s, x, y, 56, 40, 24, "strip", (236, 196, 100), (1, 1.5))
    props(s, ["chili", "lemon"])


@dish("dish_092", "wood")  # Khasta kachori
def _(s):
    plate(s, CX, CY, 360, "white")
    for (x, y) in [(470, 450), (680, 440), (580, 630)]:
        shaded(s, "blob", x, y, 84, 80, 0, (214, 156, 72), wob=0.06, edge=(160, 108, 44), hl=0.3)
        sprinkle(s, x, y, 60, 56, 30, "dot", (150, 94, 40), (2, 4))
    dollop(s, 580, 540, 44, (250, 248, 238))
    drizzle(s, 580, 540, 100, (96, 150, 60), "swirl", 6, 200)
    drizzle(s, 580, 540, 130, (120, 66, 30), "zigzag", 6, 160)
    props(s, [("dip", {"color": (96, 150, 60)}), ("dip", {"color": (120, 66, 30)})])


@dish("dish_093", "wood")  # Samosa chaat
def _(s):
    bowl_dish(s, "white", 340, base=(150, 98, 44), blobs=[(176, 118, 56), (126, 80, 36)], flecks=[(110, 62, 24), (220, 170, 90)], dish_plate=True,
              items=[("tri", 40, (214, 156, 72), 4), ("ball", 12, (222, 176, 100), 14, {"mind": 1})], garn=["sev", "pomegranate", "redonion", "cilantro"],
              after=lambda s, cx, cy, rx, ry: (drizzle(s, cx, cy, 130, (250, 248, 238), "zigzag", 9, 220), drizzle(s, cx, cy, 130, (96, 150, 60), "lines", 6, 190)), pr=["lemon", "chili"])


@dish("dish_094", "wood")  # Bread pakora
def _(s):
    plate(s, CX, CY, 360, "white")
    for (x, y, rot) in [(470, 440, 0.3), (680, 430, -0.2), (560, 590, 0.9), (720, 600, 0.1)]:
        shaded(s, "tri", x, y, 100, 100, rot, (210, 150, 60), wob=0.06, edge=(156, 100, 36), hl=0.25)
        sprinkle(s, x, y, 60, 60, 24, "dot", (150, 90, 30), (2, 4))
    props(s, [("dip", {"color": (96, 150, 60)}), ("dip", {"color": (196, 40, 30)}), "chili"])


@dish("dish_095", "wood")  # Paneer frankie
def _(s):
    board(s, CX, CY, 420, 310, "wood")
    roll(s, 540, 470, 250, 58, -0.1, body=(224, 178, 100), filling=((248, 238, 208), (90, 160, 60), (200, 60, 40)))
    roll(s, 600, 650, 240, 56, 0.08, body=(224, 178, 100), filling=((248, 238, 208), (90, 160, 60), (200, 60, 40)))
    props(s, ["chili", ("dip", {"color": (96, 150, 60)}), "lemon"])


@dish("dish_123", "wood")  # Vada pav
def _(s):
    board(s, CX, CY, 420, 310, "wood")
    for (x, y) in [(470, 480), (700, 470)]:
        shaded(s, "blob", x, y, 120, 96, 0, (222, 168, 86), wob=0.04, edge=(166, 116, 50), hl=0.3)
        shaded(s, "blob", x, y - 8, 78, 70, 0, (226, 156, 56), wob=0.1, edge=(150, 96, 36), hl=0.3)
        sprinkle(s, x, y, 70, 60, 24, "dot", (150, 90, 30), (2, 4))
    herbs(s, [(580, 380)], 9)
    props(s, ["chili", ("dip", {"color": (96, 150, 60)}), "lemon"])


@dish("dish_124", "wood")  # Pani puri
def _(s):
    plate(s, 520, 500, 330, "steel")
    for (x, y) in spots(s, 520, 500, 230, 220, 10, 100):
        shaded(s, "blob", x, y, 50, 48, 0, (226, 176, 90), wob=0.05, edge=(170, 120, 50), hl=0.3)
        E_hole = s.lay()
        E(ImageDraw.Draw(E_hole), x, y - 4, 20, 18, (120, 76, 34, 255))
        s.put(E_hole, blur=1.5)
    prop_dip(s, 930, 460, (96, 160, 70), 100, "steel")
    props(s, ["lemon", "chili"])


@dish("dish_125", "slate")  # Veg steamed momos
def _(s):
    steamer(s, CX, CY, 350)
    for (x, y) in [(480, 440), (640, 400), (730, 540), (590, 560), (450, 600), (660, 680)]:
        momo(s, x, y, 76)
    props(s, [("dip", {"color": (190, 50, 30)}), "chili"])


@dish("dish_126", "wood")  # Chicken kathi roll
def _(s):
    board(s, CX, CY, 420, 310, "dark")
    roll(s, 540, 470, 250, 56, -0.12, body=(214, 166, 90), filling=((220, 150, 80), (176, 76, 124), (110, 170, 70)))
    roll(s, 600, 650, 240, 54, 0.1, body=(214, 166, 90), filling=((220, 150, 80), (176, 76, 124), (110, 170, 70)))
    props(s, ["lemon", "chili", ("dip", {"color": (96, 150, 60)})])


@dish("dish_162", "wood")  # Paneer butter masala wrap
def _(s):
    board(s, CX, CY, 420, 310, "wood")
    roll(s, 540, 470, 250, 58, -0.1, body=(226, 186, 120), filling=((226, 110, 50), (248, 238, 208), (110, 170, 70)))
    roll(s, 600, 650, 240, 56, 0.08, body=(226, 186, 120), filling=((226, 110, 50), (248, 238, 208), (110, 170, 70)))
    props(s, ["tomato", ("dip", {"color": (214, 98, 40)}), "leaf"])


@dish("dish_151", "slate")  # Misal pav
def _(s):
    bowl_dish(s, "steel", 260, base=(196, 60, 24), blobs=[(220, 84, 34), (160, 44, 20)], flecks=[(236, 190, 90), (120, 70, 30)], cx=430, cy=500, nfleck=500,
              items=[("ball", 10, (222, 176, 100), 14, {"mind": 1})], garn=["sev", "redonion", "cilantro"], after=lambda s, cx, cy, rx, ry: drizzle(s, cx, cy, 80, (250, 220, 90), "swirl", 5, 160))
    for (x, y) in [(850, 380), (880, 600)]:
        shaded(s, "blob", x, y, 90, 80, 0, (226, 176, 98), wob=0.05, hl=0.35, edge=(170, 120, 56))
    props(s, ["lemon", "chili"])


# ============================================================================ SKEWERS / MED / MISC
@dish("dish_031", "linen")  # Falafel wrap
def _(s):
    board(s, CX, CY, 420, 310, "wood")
    roll(s, 540, 440, 240, 56, -0.1, body=(226, 190, 126), filling=((120, 170, 70), (246, 240, 226), (200, 60, 40)))
    roll(s, 600, 600, 230, 54, 0.1, body=(226, 190, 126), filling=((120, 170, 70), (246, 240, 226), (200, 60, 40)))
    for (x, y) in [(860, 700), (920, 650), (830, 640)]:
        shaded(s, "blob", x, y, 36, 34, 0, (150, 100, 44), wob=0.08, edge=(100, 62, 24))
    props(s, ["lemon", ("dip", {"color": (250, 248, 238)})])


@dish("dish_037", "linen")  # German potato salad
def _(s):
    bowl_dish(s, "cream", 330, base=(238, 218, 150), blobs=[(246, 230, 170), (222, 198, 126)], flecks=[(250, 240, 200)], nfleck=300,
              items=[("slice", 26, (232, 196, 108), 10, {"edge": (206, 160, 76)}), ("cube", 12, (190, 100, 70), 8), ("ring", 8, (120, 180, 70), 8, {"thick": 0.3})], garn=["parsley", "pepper"], pr=["lemon", "leaf"])


@dish("dish_041", "slate")  # Garlic prawns
def _(s):
    rx, ry = pan(s, CX, CY, 330, "copper")
    fill(s, CX, CY, rx, ry, (216, 150, 50), [(236, 176, 70), (190, 120, 36)], [(250, 220, 130)], nblob=100, nfleck=300)
    for (x, y) in spots(s, CX, CY, rx * 0.78, ry * 0.78, 8, 98):
        shaded(s, "cres", x, y, 46, 46, s.r.uniform(0, TAU), (240, 140, 96), wob=0.05, hl=0.3)
    sprinkle(s, CX, CY, rx * 0.85, ry * 0.85, 60, "dot", (252, 236, 170), (2, 4))
    garnish(s, CX, CY, rx, ry, ["parsley", "chili"])
    props(s, ["lemon", "garlic"])


@dish("dish_082", "linen")  # Chicken gyro
def _(s):
    plate(s, CX, CY, 370, "white")
    for (x, y, rot) in [(480, 430, -0.1), (680, 480, 0.1)]:
        roll(s, x, y, 170, 54, rot, body=(238, 224, 190), filling=((214, 160, 90), (200, 60, 40), (246, 240, 226)))
    fries(s, 600, 700, 90, 6, ln=80)
    prop_dip(s, 880, 700, (250, 248, 238), 56)
    props(s, ["lemon", "tomato"])


@dish("dish_163", "linen")  # Greek chicken souvlaki
def _(s):
    board(s, CX, CY, 440, 330, "wood")
    for k in range(3):
        y = 380 + k * 90
        lay = s.lay()
        Ln(ImageDraw.Draw(lay), [(260, y), (900, y + 20)], rgba((200, 170, 120)), 5)
        s.put(lay, shadow=(3, 6, 5, 0.5))
        for j in range(5):
            col = [(220, 160, 84), (80, 150, 56), (220, 70, 46), (220, 160, 84), (240, 200, 70)][j]
            shaded(s, "rect", 330 + j * 100, y + 4 + j * 4, 36, 32, 0.04, col, wob=0.04, ex=3.2)
    shaded(s, "blob", 560, 700, 120, 90, 0, (240, 224, 190), wob=0.03, edge=(206, 180, 130), hl=0.1)
    prop_dip(s, 1000, 560, (250, 248, 238), 66)
    props(s, ["lemon", "tomato"])


@dish("dish_083", "wood")  # Spanish omelette
def _(s):
    plate(s, CX, CY, 370, "white")
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, CX, CY, 250, 240, rgba((190, 130, 50)))
    E(d, CX, CY, 238, 228, rgba((234, 188, 76)))
    s.put(lay, shadow=(8, 12, 12, 0.5))
    sprinkle(s, CX, CY, 220, 210, 80, "dot", (206, 150, 56), (6, 14), alpha=120)
    # cut wedge
    cut = s.lay()
    d = ImageDraw.Draw(cut)
    P(d, [(CX * 2, CY * 2), ((CX + 250) * 2, (CY - 70) * 2), ((CX + 230) * 2, (CY + 90) * 2)], rgba((250, 216, 120)))
    s.clipped(cut, s.ell_mask(CX, CY, 250, 240), blur=0.4)
    place(s, CX + 170, CY + 10, 60, 70, [("cube", 8, (236, 214, 150), 4, {"mind": 1}), ("ball", 6, (250, 190, 60), 3)], 1)
    herbs(s, [(450, 450), (600, 380)], 9)
    props(s, ["tomato", "garlic"])


@dish("dish_143", "linen")  # Hummus and pita platter
def _(s):
    plate(s, CX, CY, 370, "white")
    fill(s, CX, CY - 20, 170, 160, (232, 202, 148), [(240, 214, 164), (216, 184, 130)], [(250, 236, 196)], nblob=60, nfleck=100)
    drizzle(s, CX, CY - 20, 90, (200, 160, 40), "swirl", 6, 170)
    sprinkle(s, CX, CY - 20, 150, 140, 40, "dot", (190, 70, 30), (1.5, 3))
    place(s, CX, CY - 20, 160, 150, [("ball", 10, (226, 190, 110), 6, {"mind": 1.3})], 1.2)
    for i, (x, y) in enumerate(ring_pts(CX, CY - 20, 270, 250, 6, 0.4)):
        shaded(s, "tri", x, y, 70, 70, i * 1.05 + 0.2, (232, 204, 150), wob=0.04, edge=(188, 154, 100), hl=0.25)
    place(s, CX, CY + 160, 150, 50, [("slice", 20, (60, 120, 50), 4, {"edge": (60, 120, 50)}), ("ball", 12, (60, 40, 60), 6, {"mind": 1.3})], 1)
    props(s, ["lemon", ("leaf", {"color": (70, 130, 56)})])


@dish("hummus_platter", "wood")
def _(s):
    board(s, CX, CY, 440, 340, "wood")
    fill(s, CX, CY - 10, 180, 168, (230, 198, 140), [(240, 214, 164), (212, 180, 126)], [(250, 236, 196)], nblob=60, nfleck=100)
    drizzle(s, CX, CY - 10, 96, (210, 170, 40), "swirl", 6, 170)
    sprinkle(s, CX, CY - 10, 160, 150, 50, "dot", (190, 70, 30), (1.5, 3))
    for i, (x, y) in enumerate(ring_pts(CX, CY - 10, 280, 250, 7, 0.1)):
        if i % 2 == 0:
            shaded(s, "tri", x, y, 66, 66, i + 0.2, (232, 204, 150), wob=0.04, edge=(188, 154, 100), hl=0.25)
        else:
            shaded(s, "rect", x, y, 46, 10, i + 0.5, (232, 124, 40), wob=0.0, ex=4, hl=0.3)
    place(s, CX, CY + 200, 200, 50, [("slice", 18, (190, 220, 150), 4, {"edge": (60, 120, 50)}), ("ball", 12, (60, 40, 60), 8, {"mind": 1.3})], 1)
    props(s, ["lemon", ("leaf", {"color": (70, 130, 56)})])


@dish("dish_155", "slate")  # Shakshuka
def _(s):
    rx, ry = pan(s, CX, CY, 340, "dark")
    fill(s, CX, CY, rx, ry, (196, 56, 30), [(220, 76, 40), (164, 40, 24)], [(240, 140, 80), (120, 26, 18)], nblob=160, nfleck=300)
    place(s, CX, CY, rx, ry, [("rect", 12, (80, 150, 56), 8), ("ring", 12, (230, 200, 60), 4, {"thick": 0.16})], 1.2)
    for (x, y) in [(480, 460), (660, 450), (570, 610)]:
        shaded(s, "blob", x, y, 74, 64, s.r.uniform(0, 3), (252, 250, 244), wob=0.14, hl=0.05, edge=(214, 210, 200))
        E_yolk(s, x, y)
    garnish(s, CX, CY, rx, ry, ["parsley", "paprika", "cheese"])
    props(s, ["lemon", "chili"])


@dish("dish_158", "wood")  # Chicken shawarma plate
def _(s):
    plate(s, CX, CY, 380, "white")
    for k in range(7):
        shaded(s, "rect", 450 + k * 36, 450 + (k % 2) * 8, 20, 90, 0.2 + k * 0.03, jit((200, 140, 72), s.r, 14), wob=0.04, ex=3, hl=0.3)
    rice(s, 740, 470, 130, 120, color=(240, 214, 160), n=700)
    place(s, 600, 680, 220, 50, [("cube", 14, (222, 70, 54), 5), ("slice", 14, (190, 220, 150), 5, {"edge": (60, 120, 50)}), ("ball", 10, (170, 120, 80), 5)], 1)
    dollop(s, 450, 650, 40, (250, 248, 238))
    for (x, y) in [(900, 330)]:
        shaded(s, "tri", x, y, 70, 70, 0.4, (232, 204, 150), wob=0.04, hl=0.25)
    props(s, ["lemon", "chili"])


@dish("dish_081", "wood")  # Fish & chips
def _(s):
    paper_tray(s, CX, CY, 400, 320, 0.1, (60, 80, 140))
    for (x, y, rot) in [(480, 440, -0.3), (700, 440, 0.3)]:
        shaded(s, "rect", x, y, 150, 62, rot, (214, 152, 60), wob=0.06, ex=2.6, hl=0.28, edge=(160, 100, 36))
        sprinkle(s, x, y, 110, 40, 40, "dot", (176, 112, 40), (3, 6))
    fries(s, 590, 640, 160, 22, ln=110)
    props(s, ["lemon", ("dip", {"color": (214, 220, 170)}), "tomato"])

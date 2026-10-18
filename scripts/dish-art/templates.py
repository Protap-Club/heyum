"""
Dish-level compositions built from dishart primitives.

Each template draws a complete scene (vessel, food, garnish, props) onto a Scene. dishes.py
then describes all 150 dishes as short calls into these.
"""
import math

from PIL import ImageDraw

from dishart import (CORNERS, E, Ln, P, TAU, bowl, board, chunks, cube, dollop, drizzle, epoly, fill, herb,
                     herbs, jit, lighten, mixc, noodles, pan, plate, props, rgba, rice, ring_pts, shade,
                     shape_pts, shaded, spots, sprinkle, steamer)

CX, CY = 600, 520

# name -> (sprinkle kind, colour(s), size range, default count)
GARN = {
    "chili": ("dot", (196, 44, 28), (1.5, 3.2), 50),
    "paprika": ("dot", (196, 72, 30), (1.5, 3.2), 60),
    "ginger": ("strip", (232, 200, 120), (1.4, 2.2), 30),
    "scallion": ("ring", (110, 176, 70), (3, 5), 40),
    "greenstrip": ("strip", (90, 160, 60), (1.4, 2.2), 28),
    "sesame": ("dot", (244, 234, 206), (1.4, 2.3), 90),
    "blackseed": ("dot", (28, 26, 24), (1.2, 2.2), 60),
    "pepper": ("dot", (24, 22, 20), (1, 2), 80),
    "redstrip": ("strip", (206, 56, 36), (1.4, 2.2), 24),
    "peanut": ("dot", (214, 168, 106), (3, 5.5), 24),
    "redonion": ("ring", (176, 76, 124), (4, 7), 26),
    "crumb": ("dot", (214, 170, 100), (1.6, 3), 80),
    "cheese": ("strip", (250, 226, 150), (1.5, 2.4), 60),
    "cumin": ("dot", (150, 108, 52), (1.4, 2.4), 50),
    "corn": ("dot", (244, 206, 70), (3, 4.6), 22),
    "pomegranate": ("dot", (170, 30, 54), (3, 4.4), 28),
    "sev": ("strip", (236, 196, 100), (1.2, 1.8), 70),
    "oregano": ("leaf", (86, 120, 60), (1.4, 2.2), 50),
    "parsley": ("leaf", (80, 150, 60), (1.4, 2.4), 60),
    "zest": ("strip", (240, 214, 90), (1.2, 1.8), 24),
    "saffron": ("strip", (236, 140, 30), (1.2, 1.8), 18),
    "friedonion": ("strip", (176, 112, 40), (1.6, 2.8), 40),
}


def garnish(s, cx, cy, rx, ry, items):
    for g in items:
        name, n = (g, None) if isinstance(g, str) else g
        if name in ("cilantro", "basil", "mint"):
            col = {"cilantro": (72, 142, 58), "basil": (52, 118, 46), "mint": (96, 168, 84)}[name]
            herbs(s, spots(s, cx, cy, rx * 0.9, ry * 0.9, n or 6, 40), 10 if name != "basil" else 13, col)
        else:
            kind, col, size, dn = GARN[name]
            sprinkle(s, cx, cy, rx * 0.92, ry * 0.92, n or dn, kind, col, size)


def leaf(s, x, y, ln, wd, rot, color, vein=True):
    seed = s.r.randint(0, 10 ** 6)
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    P(d, shape_pts("blob", x, y, ln, wd, rot, 0.12, seed), rgba(shade(color, 0.7)))
    P(d, shape_pts("blob", x, y, ln * 0.93, wd * 0.88, rot, 0.12, seed), rgba(color))
    P(d, shape_pts("blob", x - ln * 0.1, y - wd * 0.2, ln * 0.55, wd * 0.4, rot, 0.12, seed), rgba(lighten(color, 0.25), 200))
    if vein:
        cr, sr = math.cos(rot), math.sin(rot)
        Ln(d, [(x - cr * ln * 0.8, y - sr * ln * 0.8), (x + cr * ln * 0.8, y + sr * ln * 0.8)], rgba(lighten(color, 0.4), 190), 1.8)
    s.put(lay, shadow=(3, 6, 5, 0.5), blur=0.8)


def slice_(s, x, y, r, edge, inner, seeds=None, rot=0.0):
    """Round slice (cucumber, tomato, olive, pepperoni...)."""
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, x, y, r, r * 0.96, rgba(edge))
    E(d, x, y, r * 0.86, r * 0.82, rgba(inner))
    E(d, x - r * 0.22, y - r * 0.26, r * 0.4, r * 0.24, rgba(lighten(inner, 0.35), 180))
    if seeds:
        for k in range(6):
            a = rot + k * TAU / 6
            E(d, x + math.cos(a) * r * 0.45, y + math.sin(a) * r * 0.42, r * 0.09, r * 0.07, rgba(seeds))
    s.put(lay, shadow=(3, 5, 4, 0.5), blur=0.5)


def ring_item(s, x, y, r, color, thick=0.28):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    d.ellipse([(x - r) * S_, (y - r) * S_, (x + r) * S_, (y + r) * S_], outline=rgba(shade(color, 0.7)), width=int(r * thick * 2.1 * S_))
    d.ellipse([(x - r) * S_, (y - r) * S_, (x + r) * S_, (y + r) * S_], outline=rgba(color), width=int(r * thick * 1.6 * S_))
    s.put(lay, shadow=(3, 5, 4, 0.5), blur=0.5)


S_ = 2


def place(s, cx, cy, rx, ry, items, base_mind=1.7):
    """items: (kind, size, color, n[, opts]). kinds: cube blob rect tri disc slice leaf ring ball."""
    placed = []
    for it in items:
        kind, size, color, n = it[:4]
        opt = it[4] if len(it) > 4 else {}
        pts = spots(s, cx, cy, rx * 0.84, ry * 0.84, n, size * opt.get("mind", base_mind), opt.get("inner", 0.0), existing=placed)
        placed += pts
        for x, y in pts:
            k = s.r.uniform(0.8, 1.2)
            if kind == "cube":
                cube(s, x, y, size * k, jit(color, s.r, 8))
            elif kind == "disc":
                shaded(s, "blob", x, y, size * k, size * k * 0.94, 0, jit(color, s.r, 8), wob=0.03)
            elif kind == "ball":
                shaded(s, "blob", x, y, size * k, size * k, 0, jit(color, s.r, 10), wob=0.05, hl=0.4)
            elif kind == "slice":
                slice_(s, x, y, size * k, opt.get("edge", shade(color, 0.7)), color, opt.get("seeds"), s.r.uniform(0, 6))
            elif kind == "leaf":
                leaf(s, x, y, size * 1.7 * k, size * 0.8 * k, s.r.uniform(0, TAU), jit(color, s.r, 12))
            elif kind == "ring":
                ring_item(s, x, y, size * k, color, opt.get("thick", 0.28))
            elif kind == "rect":
                shaded(s, "rect", x, y, size * 1.5 * k, size * 0.7 * k, s.r.uniform(0, TAU), jit(color, s.r, 8), wob=0.04, ex=5)
            elif kind == "tri":
                shaded(s, "tri", x, y, size * k, size * k, s.r.uniform(0, TAU), jit(color, s.r, 8), wob=0.03)
            else:
                shaded(s, "blob", x, y, size * k, size * k * s.r.uniform(0.72, 0.95), s.r.uniform(0, TAU), jit(color, s.r, 10), wob=0.1)
    return placed


# --------------------------------------------------------------------------- generic bowl / plate dish
def bowl_dish(s, vessel="copper", r=340, base=(120, 80, 40), blobs=(), flecks=(), items=(), swirl=None, garn=(),
              pr=(), cx=CX, cy=CY, nfleck=1100, nblob=220, dish_plate=False, swirl_color=(247, 240, 222), dollops=(),
              fsize=(1.5, 4.5), noodle=None, rice_=None, after=None, gloss=True):
    rx, ry = (plate if dish_plate else bowl)(s, cx, cy, r, vessel)
    if base is not None:
        fill(s, cx, cy, rx, ry, base, blobs, flecks, nblob=nblob, nfleck=nfleck, fsize=fsize, gloss=gloss)
    if noodle:
        noodles(s, cx, cy, rx * 0.95, ry * 0.95, **noodle)
    if rice_:
        rice(s, cx, cy, rx * 0.9, ry * 0.9, **rice_)
    if swirl:
        drizzle(s, cx - 10, cy - 20, r * 0.34, swirl_color if swirl is True else swirl)
    place(s, cx, cy, rx, ry, items)
    for (x, y, rr, col) in dollops:
        dollop(s, cx + x, cy + y, rr, col)
    if after:
        after(s, cx, cy, rx, ry)
    garnish(s, cx, cy, rx, ry, garn)
    props(s, pr)
    return rx, ry


# --------------------------------------------------------------------------- pizza
def pizza(s, cx=CX, cy=CY, r=330, sauce=(190, 50, 34), cheese=(246, 220, 140), toppings=(), cut=True, base_plate="wood",
          crust=(224, 170, 92), basil=False, pr=()):
    if base_plate:
        board(s, cx, cy, r + 70, r + 50, base_plate)
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, cx, cy, r, r * 0.97, rgba(shade(crust, 0.75)))
    E(d, cx, cy, r * 0.97, r * 0.94, rgba(crust))
    s.put(lay, shadow=(8, 14, 12, 0.6))
    # charred crust spots
    sp = s.lay()
    d = ImageDraw.Draw(sp)
    for _ in range(46):
        a = s.r.uniform(0, TAU)
        x, y = cx + math.cos(a) * r * 0.93, cy + math.sin(a) * r * 0.9
        E(d, x, y, s.r.uniform(6, 15), s.r.uniform(5, 11), rgba((120, 66, 28), 150))
    s.put(sp, blur=2)
    fill(s, cx, cy, r * 0.82, r * 0.79, sauce, [shade(sauce, 0.85), lighten(sauce, 0.1)], [(150, 30, 20), (210, 80, 50)], nblob=70, nfleck=120)
    # melted cheese
    mc = s.lay()
    d = ImageDraw.Draw(mc)
    for _ in range(150):
        a = s.r.uniform(0, TAU)
        rr = math.sqrt(s.r.random()) * r * 0.76
        x, y = cx + math.cos(a) * rr, cy + math.sin(a) * rr * 0.96
        E(d, x, y, s.r.uniform(22, 48), s.r.uniform(18, 38), rgba(jit(cheese, s.r, 10), 230))
    mask = s.ell_mask(cx, cy, r * 0.82, r * 0.79)
    s.clipped(mc, mask, blur=0.5)
    brown = s.lay()
    d = ImageDraw.Draw(brown)
    for _ in range(40):
        a = s.r.uniform(0, TAU)
        rr = math.sqrt(s.r.random()) * r * 0.74
        E(d, cx + math.cos(a) * rr, cy + math.sin(a) * rr, s.r.uniform(6, 16), s.r.uniform(5, 12), rgba((196, 130, 52), 130))
    s.clipped(brown, mask, blur=3)
    for kind, size, color, n, *opt in toppings:
        place(s, cx, cy, r * 0.8, r * 0.78, [(kind, size, color, n, opt[0] if opt else {})], base_mind=1.5)
    if basil:
        for x, y in spots(s, cx, cy, r * 0.7, r * 0.7, 7, 80):
            leaf(s, x, y, 30, 16, s.r.uniform(0, TAU), (52, 124, 46))
    if cut:
        cl = s.lay()
        d = ImageDraw.Draw(cl)
        for k in range(4):
            a = math.pi * k / 4 + 0.2
            Ln(d, [(cx - math.cos(a) * r * 0.97, cy - math.sin(a) * r * 0.94), (cx + math.cos(a) * r * 0.97, cy + math.sin(a) * r * 0.94)], rgba((70, 40, 20), 120), 3)
        s.put(cl, blur=0.8)
    props(s, pr)


# --------------------------------------------------------------------------- burger
def bun_top(s, x, y, r, color=(206, 134, 60)):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, x, y, r, r * 0.96, rgba(shade(color, 0.7)))
    E(d, x, y, r * 0.94, r * 0.9, rgba(color))
    E(d, x - r * 0.15, y - r * 0.2, r * 0.62, r * 0.5, rgba(lighten(color, 0.22), 220))
    E(d, x - r * 0.28, y - r * 0.38, r * 0.26, r * 0.12, rgba(lighten(color, 0.6), 190))
    for _ in range(46):
        a = s.r.uniform(0, TAU)
        rr = math.sqrt(s.r.random()) * r * 0.72
        P(d, epoly(x + math.cos(a) * rr, y + math.sin(a) * rr * 0.9, 6.5, 3.2, s.r.uniform(0, 3), 10), rgba((246, 232, 196)))
    s.put(lay, shadow=(8, 14, 12, 0.6))


def burger(s, patty=(110, 64, 40), extras=("cheese", "lettuce", "tomato"), side="fries", sauce=(196, 40, 30), bun=(206, 134, 60), pr=()):
    plate(s, CX, CY, 360, "white")
    # open-face build on the left, closed bun on the right
    ox, oy, r = 450, 520, 150
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, ox, oy, r, r * 0.96, rgba(shade(bun, 0.8)))
    E(d, ox, oy, r * 0.93, r * 0.9, rgba(lighten(bun, 0.35)))
    s.put(lay, shadow=(6, 10, 10, 0.5))
    if "lettuce" in extras:
        for k in range(14):
            a = k * TAU / 14
            leaf(s, ox + math.cos(a) * r * 0.78, oy + math.sin(a) * r * 0.74, 46, 24, a + 1.57, (96, 168, 60), vein=False)
    if "tomato" in extras:
        slice_(s, ox, oy, r * 0.82, (190, 44, 32), (226, 74, 56), (246, 190, 120))
    if "onion" in extras:
        ring_item(s, ox, oy, r * 0.62, (176, 76, 124), 0.1)
    shaded(s, "blob", ox, oy, r * 0.76, r * 0.73, 0, patty, wob=0.06, hl=0.18)
    sprinkle(s, ox, oy, r * 0.7, r * 0.66, 70, "dot", shade(patty, 0.6), (1.5, 3))
    if "cheese" in extras:
        shaded(s, "rect", ox, oy, r * 0.86, r * 0.86, 0.7, (246, 190, 52), wob=0.0, ex=6, hl=0.3)
    bun_top(s, 745, 505, 150, bun)
    if side == "fries":
        fries(s, 790, 770, 90, 8, (232, 184, 80))
    props(s, pr)


# --------------------------------------------------------------------------- fries & fried bits
def fries(s, cx, cy, rad, n, color=(232, 184, 80), ln=95, wd=9):
    for _ in range(n * 4):
        a = s.r.uniform(0, TAU)
        rr = math.sqrt(s.r.random()) * rad
        x, y = cx + math.cos(a) * rr * 1.5, cy + math.sin(a) * rr
        shaded(s, "rect", x, y, ln * s.r.uniform(0.7, 1.1) * 0.5, wd, s.r.uniform(-0.9, 0.9) + 1.0, jit(color, s.r, 14), wob=0.0, ex=6, hl=0.2, shadow=(2, 4, 3, 0.45))


def paper_tray(s, cx, cy, rw, rh, rot=0.0, color=(220, 60, 50)):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    pts = shape_pts("rect", cx, cy, rw, rh, rot, ex=14)
    P(d, pts, rgba((246, 240, 228)))
    # checker
    n = 14
    for i in range(n):
        for j in range(n):
            if (i + j) % 2 == 0:
                x0 = cx - rw + (2 * rw) * i / n
                y0 = cy - rh + (2 * rh) * j / n
                P(d, [(x0 * 2, y0 * 2), ((x0 + 2 * rw / n) * 2, y0 * 2), ((x0 + 2 * rw / n) * 2, (y0 + 2 * rh / n) * 2), (x0 * 2, (y0 + 2 * rh / n) * 2)], rgba(color))
    s.clipped(lay, s.poly_mask(pts), shadow=(10, 16, 16, 0.6))


# --------------------------------------------------------------------------- sandwiches / slabs
def slab(s, x, y, wd, ht, rot, layers, crust=(196, 140, 70)):
    """A sandwich half lying on its side: horizontal bands (top to bottom)."""
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    P(d, shape_pts("rect", x, y, wd, ht, rot, ex=9), rgba(shade(crust, 0.7)))
    total = sum(t for _, t in layers)
    cr, sr = math.cos(rot), math.sin(rot)
    yy = -ht * 0.94
    for col, t in layers:
        bh = (t / total) * ht * 1.88
        cyl = yy + bh / 2
        px = x - sr * cyl
        py = y + cr * cyl
        P(d, shape_pts("rect", px, py, wd * 0.95, bh / 2 + 0.6, rot, ex=10), rgba(col))
        yy += bh
    mask = s.poly_mask(shape_pts("rect", x, y, wd, ht, rot, ex=9))
    sh = s.lay()
    s.clipped(lay, mask, shadow=(7, 12, 12, 0.6))


def toast(s, x, y, size, rot, color=(214, 164, 90), marks=True, shape="rect"):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    P(d, shape_pts(shape, x, y, size, size, rot, ex=5), rgba(shade(color, 0.7)))
    P(d, shape_pts(shape, x, y, size * 0.9, size * 0.9, rot, ex=5), rgba(lighten(color, 0.3)))
    if marks:
        cr, sr = math.cos(rot + 0.7), math.sin(rot + 0.7)
        for k in range(-3, 4):
            off = k * size * 0.28
            Ln(d, [(x + off * -sr - cr * size * 0.8, y + off * cr - sr * size * 0.8), (x + off * -sr + cr * size * 0.8, y + off * cr + sr * size * 0.8)], rgba((140, 88, 40), 130), 5)
    s.clipped(lay, s.poly_mask(shape_pts(shape, x, y, size, size, rot, ex=5)), shadow=(6, 10, 10, 0.55))


# --------------------------------------------------------------------------- rolls / wraps
def roll(s, x, y, ln, th, rot, body=(214, 176, 112), filling=((230, 200, 120), (120, 160, 70), (190, 60, 40)), wrap=(242, 234, 212)):
    """A wrap/roll lying on the board, back half in paper, filled cut end facing us."""
    ln, th = ln * 0.62, th * 1.28
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    cr, sr = math.cos(rot), math.sin(rot)
    P(d, shape_pts("rect", x, y, ln, th, rot, ex=3.0), rgba(shade(body, 0.72)))
    P(d, shape_pts("rect", x, y - 2, ln * 0.97, th * 0.9, rot, ex=3.0), rgba(body))
    P(d, shape_pts("rect", x - 3, y - th * 0.3, ln * 0.86, th * 0.34, rot, ex=3.0), rgba(lighten(body, 0.25), 200))
    for _ in range(30):
        t = s.r.uniform(-0.85, 0.85) * ln
        o = s.r.uniform(-0.6, 0.6) * th
        E(d, x + cr * t - sr * o, y + sr * t + cr * o, s.r.uniform(2, 5), s.r.uniform(2, 4), rgba(shade(body, 0.5), 170))
    if wrap:
        wx, wy = x - cr * ln * 0.38, y - sr * ln * 0.38
        P(d, shape_pts("rect", wx, wy, ln * 0.64, th * 1.04, rot, ex=10), rgba(shade(wrap, 0.82)))
        P(d, shape_pts("rect", wx, wy - 1, ln * 0.62, th * 1.0, rot, ex=10), rgba(wrap))
        for k in range(-2, 3):
            o = k * th * 0.3
            Ln(d, [(wx - cr * ln * 0.6 - sr * o, wy - sr * ln * 0.6 + cr * o), (wx + cr * ln * 0.6 - sr * o, wy + sr * ln * 0.6 + cr * o)], rgba(shade(wrap, 0.9), 150), 1.6)
    ex_, ey_ = x + cr * ln * 0.98, y + sr * ln * 0.98
    P(d, epoly(ex_, ey_, th * 0.5, th * 0.98, rot, 28), rgba(shade(body, 0.78)))
    P(d, epoly(ex_, ey_, th * 0.42, th * 0.84, rot, 28), rgba(filling[0]))
    for k in range(9):
        a = s.r.uniform(0, TAU)
        rr = s.r.uniform(0.1, 0.62)
        E(d, ex_ + math.cos(a) * rr * th * 0.3, ey_ + math.sin(a) * rr * th * 0.75, s.r.uniform(4, 9), s.r.uniform(5, 10), rgba(jit(s.r.choice(filling), s.r, 10)))
    s.put(lay, shadow=(8, 12, 12, 0.6), blur=0.5)


# --------------------------------------------------------------------------- dumplings
def dumpling(s, x, y, size, rot, color=(244, 232, 206), fried=False, pleats=True):
    seed = s.r.randint(0, 10 ** 6)
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    P(d, shape_pts("cres", x, y, size, size * 0.62, rot, 0.03, seed), rgba(shade(color, 0.78)))
    body = shape_pts("cres", x, y + 1, size * 0.93, size * 0.56, rot, 0.03, seed)
    P(d, body, rgba(color))
    if fried:
        P(d, shape_pts("cres", x, y + size * 0.22, size * 0.86, size * 0.2, rot, 0.02, seed), rgba((206, 150, 70), 210))
    P(d, shape_pts("cres", x - size * 0.1, y - size * 0.16, size * 0.52, size * 0.26, rot, 0.03, seed), rgba(lighten(color, 0.4), 200))
    if pleats:
        cr, sr = math.cos(rot), math.sin(rot)
        for k in range(9):
            a = math.pi * (0.12 + 0.76 * k / 8)
            px, py = math.cos(a) * size * 0.8, -math.sin(a) * size * 0.5
            qx, qy = math.cos(a) * size * 0.95, -math.sin(a) * size * 0.6
            Ln(d, [(x + px * cr - py * sr, y + px * sr + py * cr), (x + qx * cr - qy * sr, y + qx * sr + qy * cr)], rgba(shade(color, 0.7), 190), 2.2)
    s.put(lay, shadow=(4, 7, 6, 0.5), blur=0.5)


def momo(s, x, y, r, color=(244, 234, 212)):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, x, y, r, r * 0.95, rgba(shade(color, 0.78)))
    E(d, x, y, r * 0.93, r * 0.88, rgba(color))
    E(d, x - r * 0.2, y - r * 0.22, r * 0.5, r * 0.36, rgba(lighten(color, 0.4), 190))
    a0 = s.r.uniform(0, 3)
    for k in range(16):
        a = a0 + k * TAU / 16
        Ln(d, [(x + math.cos(a) * r * 0.12, y + math.sin(a) * r * 0.1), (x + math.cos(a + 0.45) * r * 0.62, y + math.sin(a + 0.45) * r * 0.58)], rgba(shade(color, 0.7), 200), 2)
    E(d, x, y, r * 0.1, r * 0.09, rgba(shade(color, 0.72)))
    s.put(lay, shadow=(4, 7, 6, 0.5), blur=0.5)


# --------------------------------------------------------------------------- drinks / mugs
def mug(s, cx, cy, r, liquid, foam=None, art=None, vessel="white", handle=True):
    if handle:
        lay = s.lay()
        d = ImageDraw.Draw(lay)
        o, m, i = BOWLS_[vessel]
        d.ellipse([(cx + r * 0.88) * 2, (cy - r * 0.3) * 2, (cx + r * 1.42) * 2, (cy + r * 0.3) * 2], outline=rgba(m), width=int(r * 0.14 * 2))
        s.put(lay, shadow=(6, 10, 10, 0.5))
    rx, ry = bowl(s, cx, cy, r, vessel, 1.0)
    fill(s, cx, cy, rx * 0.97, ry * 0.97, liquid, (), (), nblob=0, nfleck=0)
    if foam:
        E_lay = s.lay()
        E(ImageDraw.Draw(E_lay), cx, cy, rx * 0.86, ry * 0.86, rgba(foam, 235))
        s.put(E_lay, blur=3)
    return rx, ry


from dishart import BOWLS as BOWLS_  # noqa: E402


def heart(s, cx, cy, r, color, alpha=240):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, cx - r * 0.5, cy - r * 0.2, r * 0.56, r * 0.56, rgba(color, alpha))
    E(d, cx + r * 0.5, cy - r * 0.2, r * 0.56, r * 0.56, rgba(color, alpha))
    P(d, [((cx - r * 1.03) * 2, (cy - r * 0.02) * 2), ((cx + r * 1.03) * 2, (cy - r * 0.02) * 2), (cx * 2, (cy + r * 1.1) * 2)], rgba(color, alpha))
    s.put(lay, blur=1.4)


def latte_art(s, cx, cy, r, color=(246, 240, 226), liquid=(132, 168, 80)):
    heart(s, cx, cy - r * 0.05, r * 0.62, color)
    heart(s, cx, cy + r * 0.02, r * 0.38, mixc(liquid, color, 0.35))
    heart(s, cx, cy + r * 0.06, r * 0.2, color)


def glass(s, cx, cy, r, liquid, foam=None, straw=(230, 60, 80), garn=None):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, cx, cy, r, r, rgba((176, 196, 208)))
    E(d, cx, cy, r * 0.95, r * 0.95, rgba((222, 234, 240)))
    s.put(lay, shadow=(12, 22, 20, 0.7))
    fill(s, cx, cy, r * 0.86, r * 0.86, liquid, [shade(liquid, 0.9), lighten(liquid, 0.1)], [lighten(liquid, 0.25)], nblob=60, nfleck=60, fsize=(2, 5))
    if foam:
        f = s.lay()
        E(ImageDraw.Draw(f), cx, cy, r * 0.8, r * 0.8, rgba(foam, 215))
        s.put(f, blur=4)
    if straw:
        st = s.lay()
        d = ImageDraw.Draw(st)
        Ln(d, [(cx + r * 0.05, cy - r * 0.1), (cx + r * 0.72, cy - r * 1.18)], rgba(straw), 13)
        for k in range(1, 6):
            t = k / 6
            Ln(d, [(cx + r * (0.05 + 0.67 * t) - 4, cy + r * (-0.1 - 1.08 * t) + 3), (cx + r * (0.05 + 0.67 * t) + 4, cy + r * (-0.1 - 1.08 * t) - 3)], (255, 255, 255, 200), 5)
        E(d, cx + r * 0.72, cy - r * 1.18, 7, 7, rgba(shade(straw, 0.6)))
        s.put(st, shadow=(6, 10, 8, 0.5), blur=0.4)
    return r * 0.86, r * 0.86


# --------------------------------------------------------------------------- stacks (pancakes, rotis)
def stack(s, cx, cy, r, n, color, edge=None, top=None, offset=15, wob=0.03):
    edge = edge or shade(color, 0.7)
    for i in range(n - 1, -1, -1):
        y = cy + i * offset
        lay = s.lay()
        d = ImageDraw.Draw(lay)
        E(d, cx, y, r, r * 0.9, rgba(edge))
        E(d, cx, y - 6, r, r * 0.9, rgba(mixc(color, edge, 0.4)))
        s.put(lay, shadow=(4, 8, 8, 0.45), blur=0.4)
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, cx, cy - 6, r, r * 0.9, rgba(shade(color, 0.85)))
    E(d, cx, cy - 6, r * 0.95, r * 0.85, rgba(top or color))
    E(d, cx - r * 0.2, cy - r * 0.28, r * 0.5, r * 0.3, rgba(lighten(top or color, 0.2), 200))
    s.put(lay, blur=0.5)


def waffle(s, cx, cy, r, color=(222, 166, 84)):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, cx, cy, r, r * 0.93, rgba(shade(color, 0.7)))
    E(d, cx, cy, r * 0.96, r * 0.9, rgba(color))
    n = 7
    step = r * 2 / n
    for i in range(n + 1):
        off = -r + i * step
        Ln(d, [(cx + off, cy - r), (cx + off, cy + r)], rgba(shade(color, 0.62), 220), 5)
        Ln(d, [(cx - r, cy + off * 0.93), (cx + r, cy + off * 0.93)], rgba(shade(color, 0.62), 220), 5)
    mask = s.ell_mask(cx, cy, r, r * 0.93)
    s.clipped(lay, mask, shadow=(8, 14, 12, 0.6))
    hl = s.lay()
    E(ImageDraw.Draw(hl), cx - r * 0.3, cy - r * 0.35, r * 0.4, r * 0.2, (255, 240, 200, 70))
    s.put(hl, blur=10)

"""
Drawing primitives for the Heyum dish illustrations.

Everything here is original vector-style art rendered with Pillow, so there is no third-party
photography or AI model involved and nothing to license. Scenes are drawn at 2x and downsampled
for smooth edges.

Coordinates passed to every helper are in the 1200x1000 output space; the 2x scaling is internal.
"""
import math
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter

W, H, S = 1200, 1000, 2
w, h = W * S, H * S
TAU = math.pi * 2


# --------------------------------------------------------------------------- colour helpers
def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def lighten(c, t):
    return tuple(int(v + (255 - v) * t) for v in c[:3])


def mixc(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def rgba(c, a=255):
    return tuple(c[:3]) + (a,)


def jit(c, rnd, amt=14):
    return tuple(max(0, min(255, v + rnd.randint(-amt, amt))) for v in c[:3])


# --------------------------------------------------------------------------- raw drawing
def E(d, x, y, rx, ry, fill):
    d.ellipse([(x - rx) * S, (y - ry) * S, (x + rx) * S, (y + ry) * S], fill=fill)


def P(d, pts, fill):
    d.polygon(pts, fill=fill)


def Ln(d, pts, fill, width):
    d.line([(px * S, py * S) for px, py in pts], fill=fill, width=max(1, int(width * S)), joint="curve")


def epoly(x, y, rx, ry, rot=0.0, n=16):
    cr, sr = math.cos(rot), math.sin(rot)
    out = []
    for i in range(n):
        t = TAU * i / n
        lx, ly = math.cos(t) * rx, math.sin(t) * ry
        out.append(((x + lx * cr - ly * sr) * S, (y + lx * sr + ly * cr) * S))
    return out


def shape_pts(kind, x, y, rx, ry, rot=0.0, wob=0.0, seed=0, ex=4.0, n=56):
    """Polygon (in scaled px) for blob / rect / tri / cres shapes, optionally wobbled."""
    rr = random.Random(seed)
    harm = [(rr.uniform(0, TAU), rr.uniform(0.4, 1.0), j + 2) for j in range(3)] if wob else []
    cr, sr = math.cos(rot), math.sin(rot)
    pts = []
    for i in range(n):
        t = TAU * i / n
        if kind == "rect":
            c, s_ = math.cos(t), math.sin(t)
            px = math.copysign(abs(c) ** (2 / ex), c)
            py = math.copysign(abs(s_) ** (2 / ex), s_)
        elif kind == "tri":
            k = 0.5 / math.cos(((t + math.pi / 2) % (TAU / 3)) - math.pi / 3)
            k = 0.78 * k + 0.22
            px, py = math.cos(t) * k, math.sin(t) * k
        elif kind == "cres":
            half = n // 2
            if i < half:
                a = math.pi * i / (half - 1)
                px, py = math.cos(a), -math.sin(a)
            else:
                a = math.pi + math.pi * (i - half) / (n - half - 1)
                px, py = math.cos(a), -0.22 * math.sin(a)
        else:
            px, py = math.cos(t), math.sin(t)
        m = (1 + wob * sum(a_ * math.sin(hn * t + ph) for ph, a_, hn in harm)) if harm else 1
        lx, ly = px * rx * m, py * ry * m
        pts.append(((x + lx * cr - ly * sr) * S, (y + lx * sr + ly * cr) * S))
    return pts


# --------------------------------------------------------------------------- backgrounds
def _grain(amount):
    return Image.effect_noise((w, h), amount).convert("L")


def make_bg(kind, rnd):
    if kind == "wood":
        base = Image.new("RGB", (w, h), (112, 74, 46))
        d = ImageDraw.Draw(base)
        pw = 210 * S
        x = 0
        while x < w:
            tone = rnd.randint(-14, 14)
            d.rectangle([x, 0, x + pw, h], fill=(112 + tone, 74 + tone, 46 + tone // 2))
            d.line([(x, 0), (x, h)], fill=(52, 32, 20), width=3 * S)
            x += pw
        for _ in range(520):
            gx = rnd.uniform(0, w)
            gy = rnd.uniform(0, h)
            ln = rnd.uniform(120, 520) * S
            tone = rnd.randint(-26, 22)
            d.line([(gx, gy), (gx + rnd.uniform(-8, 8) * S, gy + ln)], fill=(112 + tone, 72 + tone, 44 + tone), width=rnd.randint(1, 3) * S)
        base = base.filter(ImageFilter.GaussianBlur(1.2 * S))
        tint, spot_c, strength, spot_k = (30, 20, 12), (255, 214, 160), 0.30, 0.4
    elif kind == "marble":
        base = Image.new("RGB", (w, h), (232, 230, 226))
        d = ImageDraw.Draw(base)
        for _ in range(9):
            x, y = rnd.uniform(0, w), rnd.uniform(0, h)
            pts = []
            for _k in range(26):
                x += rnd.uniform(-60, 90) * S
                y += rnd.uniform(-40, 70) * S
                pts.append((x, y))
            d.line(pts, fill=(196, 194, 192), width=rnd.randint(2, 5) * S)
        base = base.filter(ImageFilter.GaussianBlur(5 * S))
        tint, spot_c, strength, spot_k = (150, 146, 140), (255, 255, 252), 0.18, 0.4
    elif kind == "linen":
        base = Image.new("RGB", (w, h), (216, 200, 174))
        d = ImageDraw.Draw(base)
        for yy in range(0, h, 7 * S):
            d.line([(0, yy), (w, yy)], fill=(206, 190, 164), width=S)
        for xx in range(0, w, 7 * S):
            d.line([(xx, 0), (xx, h)], fill=(210, 194, 168), width=S)
        tint, spot_c, strength, spot_k = (120, 100, 76), (255, 246, 226), 0.16, 0.4
    elif kind == "terracotta":
        base = Image.new("RGB", (w, h), (170, 96, 64))
        tint, spot_c, strength, spot_k = (60, 28, 16), (255, 190, 140), 0.26, 0.4
    elif kind == "sage":
        base = Image.new("RGB", (w, h), (116, 134, 112))
        tint, spot_c, strength, spot_k = (30, 44, 30), (220, 236, 200), 0.22, 0.4
    elif kind == "navy":
        base = Image.new("RGB", (w, h), (34, 48, 70))
        tint, spot_c, strength, spot_k = (8, 12, 22), (120, 150, 190), 0.30, 0.6
    else:  # slate
        base = Image.new("RGB", (w, h), (46, 44, 43))
        tint, spot_c, strength, spot_k = (10, 8, 8), (96, 88, 80), 0.80, 0.8
    g = _grain(24 if kind != "marble" else 8)
    base = ImageChops.add(base, Image.merge("RGB", (g, g, g)), scale=7 if kind != "marble" else 14, offset=-18 if kind != "marble" else -9)
    spot = Image.new("L", (w, h), 0)
    ImageDraw.Draw(spot).ellipse([w * 0.1, h * 0.02, w * 0.9, h * 1.0], fill=255)
    spot = spot.filter(ImageFilter.GaussianBlur(170 * S))
    light = Image.new("RGB", (w, h), spot_c)
    base = Image.composite(light, base, spot.point(lambda v: int(v * spot_k)))
    v = Image.new("L", (w, h), 0)
    ImageDraw.Draw(v).ellipse([-w * 0.12, -h * 0.12, w * 1.12, h * 1.12], fill=255)
    v = v.filter(ImageFilter.GaussianBlur(240 * S))
    dark = Image.new("RGB", (w, h), tint)
    return Image.composite(base, dark, v.point(lambda p: min(255, int(p * (1.0 + strength)) + 30)))


# --------------------------------------------------------------------------- scene
class Scene:
    def __init__(self, seed, bg="slate"):
        self.r = random.Random(seed)
        self.seed = seed
        self.bg = bg
        self.img = make_bg(bg, self.r)

    def lay(self):
        return Image.new("RGBA", (w, h), (0, 0, 0, 0))

    def put(self, lay, shadow=None, blur=0.0):
        bb = lay.getbbox()
        if not bb:
            return
        m = int((blur + (shadow[2] if shadow else 0)) * S * 3 + (max(abs(shadow[0]), abs(shadow[1])) * S if shadow else 0)) + 4
        box = (max(0, bb[0] - m), max(0, bb[1] - m), min(w, bb[2] + m), min(h, bb[3] + m))
        crop = lay.crop(box)
        if blur:
            crop = crop.filter(ImageFilter.GaussianBlur(blur * S))
        if shadow:
            dx, dy, sb, sa = shadow
            a = crop.getchannel("A").point(lambda v: int(v * sa))
            sh = Image.new("RGBA", crop.size, (0, 0, 0, 0))
            sh.putalpha(a)
            sh = sh.filter(ImageFilter.GaussianBlur(sb * S))
            self.img.paste(sh, (box[0] + int(dx * S), box[1] + int(dy * S)), sh)
        self.img.paste(crop, (box[0], box[1]), crop)

    def clipped(self, lay, mask, shadow=None, blur=0.0):
        a = ImageChops.multiply(lay.getchannel("A"), mask)
        lay.putalpha(a)
        self.put(lay, shadow, blur)

    def ell_mask(self, cx, cy, rx, ry):
        m = Image.new("L", (w, h), 0)
        ImageDraw.Draw(m).ellipse([(cx - rx) * S, (cy - ry) * S, (cx + rx) * S, (cy + ry) * S], fill=255)
        return m

    def poly_mask(self, pts):
        m = Image.new("L", (w, h), 0)
        ImageDraw.Draw(m).polygon(pts, fill=255)
        return m

    def finish(self):
        return self.img.resize((W, H), Image.LANCZOS)


# --------------------------------------------------------------------------- vessels
BOWLS = {
    "copper": ((150, 82, 44), (196, 118, 66), (232, 222, 205)),
    "white": ((206, 202, 192), (236, 232, 224), (250, 248, 242)),
    "dark": ((26, 26, 28), (54, 54, 58), (84, 84, 90)),
    "blue": ((40, 66, 112), (66, 100, 152), (240, 242, 246)),
    "green": ((54, 92, 70), (86, 130, 98), (240, 238, 228)),
    "terra": ((134, 66, 42), (176, 98, 62), (224, 198, 172)),
    "steel": ((130, 132, 136), (188, 190, 194), (222, 224, 228)),
    "cream": ((190, 170, 140), (226, 210, 184), (248, 242, 230)),
    "red": ((140, 36, 34), (186, 56, 50), (244, 238, 230)),
    "wood": ((96, 62, 38), (140, 96, 60), (170, 122, 80)),
}


def bowl(s, cx, cy, r, kind="copper", ry_f=0.95):
    """Round bowl seen from above. Returns (rx, ry) of the usable interior."""
    o, m, i = BOWLS[kind]
    ry = r * ry_f
    sh = s.lay()
    E(ImageDraw.Draw(sh), cx + 14, cy + 30, r, ry, (0, 0, 0, 200))
    s.put(sh, blur=22)
    b = s.lay()
    d = ImageDraw.Draw(b)
    E(d, cx, cy, r, ry, rgba(o))
    E(d, cx, cy, r * 0.96, ry * 0.96, rgba(m))
    E(d, cx, cy, r * 0.915, ry * 0.915, rgba(shade(i, 0.82)))
    E(d, cx + 5, cy + 7, r * 0.9, ry * 0.9, rgba(i))
    s.put(b)
    hl = s.lay()
    d = ImageDraw.Draw(hl)
    d.arc([(cx - r * 0.975) * S, (cy - ry * 0.975) * S, (cx + r * 0.975) * S, (cy + ry * 0.975) * S], 200, 285, fill=rgba(lighten(m, 0.55), 190), width=int(8 * S))
    s.put(hl, blur=1.8)
    return r * 0.86, ry * 0.86


def plate(s, cx, cy, r, kind="white", ry_f=0.95):
    o, m, i = BOWLS[kind]
    ry = r * ry_f
    sh = s.lay()
    E(ImageDraw.Draw(sh), cx + 14, cy + 28, r, ry, (0, 0, 0, 190))
    s.put(sh, blur=22)
    b = s.lay()
    d = ImageDraw.Draw(b)
    E(d, cx, cy, r, ry, rgba(o))
    E(d, cx, cy, r * 0.98, ry * 0.98, rgba(m))
    E(d, cx, cy, r * 0.74, ry * 0.74, rgba(shade(i, 0.9)))
    E(d, cx + 3, cy + 4, r * 0.73, ry * 0.73, rgba(i))
    s.put(b)
    hl = s.lay()
    d = ImageDraw.Draw(hl)
    d.arc([(cx - r * 0.97) * S, (cy - ry * 0.97) * S, (cx + r * 0.97) * S, (cy + ry * 0.97) * S], 200, 280, fill=rgba(lighten(m, 0.6), 170), width=int(6 * S))
    s.put(hl, blur=1.6)
    return r * 0.78, ry * 0.78


def board(s, cx, cy, rw, rh, kind="wood", rot=0.0):
    col = {"wood": (176, 126, 78), "dark": (74, 52, 36), "slate": (60, 60, 64), "paper": (236, 226, 204)}[kind]
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    P(d, shape_pts("rect", cx, cy, rw, rh, rot, ex=14), rgba(shade(col, 0.72)))
    P(d, shape_pts("rect", cx, cy, rw * 0.985, rh * 0.975, rot, ex=14), rgba(col))
    for _ in range(40):
        gx = cx + s.r.uniform(-rw, rw) * 0.95
        gy = cy + s.r.uniform(-rh, rh) * 0.9
        ln = s.r.uniform(40, 200)
        t = s.r.randint(-16, 14)
        Ln(d, [(gx, gy), (gx + ln * math.cos(rot), gy + ln * math.sin(rot))], rgba((col[0] + t, col[1] + t, col[2] + t), 120), s.r.uniform(1, 3))
    mask = s.poly_mask(shape_pts("rect", cx, cy, rw, rh, rot, ex=14))
    s.clipped(lay, mask, shadow=(12, 20, 20, 0.7))


def pan(s, cx, cy, r, kind="dark"):
    col = {"dark": (34, 34, 36), "steel": (150, 152, 156), "copper": (170, 100, 58)}[kind]
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    P(d, shape_pts("rect", cx + r * 1.28, cy, r * 0.5, r * 0.09, 0, ex=6), rgba(shade(col, 0.8)))
    E(d, cx, cy, r, r * 0.97, rgba(shade(col, 0.7)))
    E(d, cx, cy, r * 0.95, r * 0.92, rgba(col))
    E(d, cx + 4, cy + 6, r * 0.9, r * 0.87, rgba(shade(col, 0.55 if kind == "dark" else 0.8)))
    s.put(lay, shadow=(14, 26, 22, 0.8))
    return r * 0.88, r * 0.85


def steamer(s, cx, cy, r):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, cx, cy, r, r, rgba((150, 110, 62)))
    E(d, cx, cy, r * 0.96, r * 0.96, rgba((196, 154, 96)))
    E(d, cx, cy, r * 0.88, r * 0.88, rgba((170, 128, 74)))
    for k in range(1, 6):
        rr = r * 0.88 * k / 5
        d.ellipse([(cx - rr) * S, (cy - rr) * S, (cx + rr) * S, (cy + rr) * S], outline=rgba((120, 86, 46)), width=3 * S)
    s.put(lay, shadow=(14, 26, 24, 0.75))
    return r * 0.86


# --------------------------------------------------------------------------- fills & textures
def fill(s, cx, cy, rx, ry, base, blobs=(), flecks=(), nblob=220, nfleck=1100, fsize=(1.5, 4.5), gloss=True, bsize=(14, 40)):
    """Sauce / soup / grain surface inside a vessel."""
    rnd = s.r
    mask = s.ell_mask(cx, cy, rx, ry)
    lay = s.lay()
    E(ImageDraw.Draw(lay), cx, cy, rx, ry, rgba(base))
    s.put(lay)
    if blobs:
        t = s.lay()
        d = ImageDraw.Draw(t)
        for _ in range(nblob):
            a = rnd.uniform(0, TAU)
            r_ = math.sqrt(rnd.random()) * 0.95
            x, y = cx + math.cos(a) * rx * r_, cy + math.sin(a) * ry * r_
            E(d, x, y, rnd.uniform(*bsize), rnd.uniform(*bsize) * 0.9, rgba(rnd.choice(blobs), 105))
        t = t.filter(ImageFilter.GaussianBlur(10 * S))
        s.clipped(t, mask)
    if flecks:
        f = s.lay()
        d = ImageDraw.Draw(f)
        for _ in range(nfleck):
            a = rnd.uniform(0, TAU)
            r_ = math.sqrt(rnd.random())
            x, y = cx + math.cos(a) * rx * r_, cy + math.sin(a) * ry * r_
            E(d, x, y, rnd.uniform(*fsize), rnd.uniform(*fsize) * 0.8, rgba(rnd.choice(flecks), rnd.randint(110, 220)))
        s.clipped(f, mask)
    if gloss:
        g = s.lay()
        E(ImageDraw.Draw(g), cx - rx * 0.4, cy - ry * 0.5, rx * 0.3, ry * 0.12, (255, 255, 255, 40))
        s.put(g, blur=16)
    e = s.lay()
    d = ImageDraw.Draw(e)
    d.ellipse([(cx - rx) * S, (cy - ry) * S, (cx + rx) * S, (cy + ry) * S], outline=(0, 0, 0, 70), width=int(9 * S))
    e = e.filter(ImageFilter.GaussianBlur(5 * S))
    s.clipped(e, mask)


def spots(s, cx, cy, rx, ry, n, mind, inner=0.0, tries=60, existing=None):
    """Rejection-sampled points inside an ellipse, keeping a minimum spacing."""
    pts = list(existing or [])
    out = []
    for _ in range(n):
        for _t in range(tries):
            a = s.r.uniform(0, TAU)
            r_ = math.sqrt(s.r.uniform(inner * inner, 1))
            x, y = cx + math.cos(a) * rx * r_, cy + math.sin(a) * ry * r_
            if all((x - px) ** 2 + (y - py) ** 2 > mind * mind for px, py in pts):
                pts.append((x, y))
                out.append((x, y))
                break
    return out


def shaded(s, kind, x, y, rx, ry, rot, color, wob=0.07, shadow=(4, 7, 6, 0.5), edge=None, hl=0.3, ex=4.0, rim=0.9):
    """One 3D-ish chunk: dark rim, body, soft top-left highlight, contact shadow."""
    seed = s.r.randint(0, 10 ** 6)
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    P(d, shape_pts(kind, x, y, rx, ry, rot, wob, seed, ex), rgba(edge or shade(color, 0.66)))
    P(d, shape_pts(kind, x, y, rx * rim, ry * rim, rot, wob, seed, ex), rgba(color))
    P(d, shape_pts(kind, x - rx * 0.16, y - ry * 0.2, rx * 0.5, ry * 0.5, rot, wob, seed, ex), rgba(lighten(color, hl), 210))
    s.put(lay, shadow=shadow, blur=1.2)


def chunks(s, pts, kind, size, color, var=0.25, rot_any=True, wob=0.09, **kw):
    for x, y in pts:
        k = s.r.uniform(1 - var, 1 + var)
        rx = size * k
        ry = size * k * s.r.uniform(0.78, 1.0) if kind != "rect" else size * k
        shaded(s, kind, x, y, rx, ry, s.r.uniform(0, TAU) if rot_any else 0, jit(color, s.r, 10), wob=wob, **kw)


def sprinkle(s, cx, cy, rx, ry, n, kind, color, size=(2, 4), alpha=230, colors=None):
    """Garnish confetti: dots, strips, rings or leaves, within an ellipse."""
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    rnd = s.r
    for _ in range(n):
        a = rnd.uniform(0, TAU)
        r_ = math.sqrt(rnd.random())
        x, y = cx + math.cos(a) * rx * r_, cy + math.sin(a) * ry * r_
        c = rnd.choice(colors) if colors else color
        sz = rnd.uniform(*size)
        if kind == "dot":
            E(d, x, y, sz, sz * 0.85, rgba(c, alpha))
        elif kind == "strip":
            t = rnd.uniform(0, math.pi)
            ln = sz * rnd.uniform(2.5, 5)
            Ln(d, [(x, y), (x + math.cos(t) * ln, y + math.sin(t) * ln)], rgba(c, alpha), sz * 0.9)
        elif kind == "ring":
            d.ellipse([(x - sz) * S, (y - sz) * S, (x + sz) * S, (y + sz) * S], outline=rgba(c, alpha), width=int(max(1, sz * 0.45) * S))
        elif kind == "leaf":
            P(d, epoly(x, y, sz * 2.2, sz, rnd.uniform(0, TAU), 12), rgba(c, alpha))
    s.put(lay, shadow=(1.5, 2.5, 2, 0.45), blur=0.4)


def herb(s, x, y, size=11, color=(70, 140, 56)):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    k0 = s.r.random() * 6
    for k in range(5):
        a = k * 1.256 + k0
        E(d, x + math.cos(a) * size * 0.9, y + math.sin(a) * size * 0.9, size * 0.85, size * 0.62, rgba(jit(color, s.r, 12)))
    E(d, x, y, size * 0.55, size * 0.55, rgba(lighten(color, 0.3)))
    s.put(lay, shadow=(2, 3, 2.5, 0.5), blur=0.4)


def herbs(s, pts, size=11, color=(70, 140, 56)):
    for x, y in pts:
        herb(s, x, y, size * s.r.uniform(0.8, 1.2), color)


def drizzle(s, cx, cy, r, color, kind="swirl", width=9, alpha=235):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    pts = []
    if kind == "swirl":
        n = 420
        for i in range(n):
            t = i / n * 5.4 * math.pi
            rr = 8 + t * r / 17
            pts.append((cx + math.cos(t) * rr, cy + math.sin(t) * rr * 0.9))
        for i in range(n - 1):
            wdt = max(2, width * (1 - i / (n * 1.1)))
            Ln(d, [pts[i], pts[i + 1]], rgba(color, alpha), wdt)
    elif kind == "zigzag":
        n = 90
        cyc = 5.5
        for i in range(n):
            t = i / (n - 1)
            x0 = cx - r + 2 * r * t
            pts.append((x0, cy + math.sin(t * cyc * TAU) * r * 0.34 + math.sin(t * 3.1) * r * 0.08))
        Ln(d, pts, rgba(color, alpha), width * 0.8)
    elif kind == "lines":
        for k in range(-3, 4):
            Ln(d, [(cx - r, cy + k * r * 0.22), (cx + r, cy + k * r * 0.22 + 14)], rgba(color, alpha), width * 0.7)
    s.put(lay, shadow=(1, 2, 2, 0.35), blur=1.0)


def dollop(s, x, y, r, color):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, x, y, r, r * 0.92, rgba(shade(color, 0.85)))
    E(d, x - r * 0.08, y - r * 0.1, r * 0.88, r * 0.8, rgba(color))
    E(d, x - r * 0.25, y - r * 0.3, r * 0.34, r * 0.22, rgba(lighten(color, 0.5), 200))
    s.put(lay, shadow=(3, 5, 4, 0.5), blur=0.8)


# --------------------------------------------------------------------------- food building blocks
def noodles(s, cx, cy, rx, ry, color, n=70, width=7.5):
    rnd = s.r
    mask = s.ell_mask(cx, cy, rx, ry)
    for _ in range(n):
        lay = s.lay()
        d = ImageDraw.Draw(lay)
        a0 = rnd.uniform(0, TAU)
        r0 = rnd.uniform(0.0, 0.9)
        x, y = cx + math.cos(a0) * rx * r0, cy + math.sin(a0) * ry * r0
        ang = rnd.uniform(0, TAU)
        pts = [(x, y)]
        for _k in range(22):
            ang += rnd.uniform(-0.55, 0.55)
            x += math.cos(ang) * 22
            y += math.sin(ang) * 22
            pts.append((x, y))
        c = jit(color, rnd, 10)
        Ln(d, pts, rgba(shade(c, 0.78)), width)
        Ln(d, pts, rgba(c), width * 0.72)
        Ln(d, [(px - 1, py - 1.5) for px, py in pts], rgba(lighten(c, 0.35), 150), width * 0.22)
        s.clipped(lay, mask, shadow=(1.5, 3, 2.5, 0.45))


def rice(s, cx, cy, rx, ry, color=(244, 240, 228), n=900, accent=None, accent_n=0.0, grain=(7, 2.6)):
    rnd = s.r
    mask = s.ell_mask(cx, cy, rx, ry)
    base = s.lay()
    E(ImageDraw.Draw(base), cx, cy, rx, ry, rgba(shade(color, 0.86)))
    s.put(base, shadow=(4, 8, 8, 0.4))
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    for _ in range(n):
        a = rnd.uniform(0, TAU)
        r_ = math.sqrt(rnd.random())
        x, y = cx + math.cos(a) * rx * r_, cy + math.sin(a) * ry * r_
        c = rnd.choice(accent) if (accent and rnd.random() < accent_n) else jit(color, rnd, 8)
        P(d, epoly(x, y, grain[0] * rnd.uniform(0.8, 1.2), grain[1], rnd.uniform(0, math.pi), 10), rgba(c))
    s.clipped(lay, mask, shadow=(0.8, 1.4, 1.2, 0.35))


def cube(s, x, y, size, color, rot=None, hl=0.35):
    shaded(s, "rect", x, y, size, size, s.r.uniform(0, TAU) if rot is None else rot, color, wob=0.0, hl=hl, ex=5)


def ring_pts(cx, cy, rx, ry, n, rot0=0.0):
    return [(cx + math.cos(rot0 + TAU * i / n) * rx, cy + math.sin(rot0 + TAU * i / n) * ry) for i in range(n)]


# --------------------------------------------------------------------------- props
def prop_spice(s, x, y, color=(190, 60, 28), r=64):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, x, y, r, r, rgba((58, 46, 40)))
    E(d, x, y, r * 0.86, r * 0.86, rgba((122, 88, 54)))
    E(d, x, y, r * 0.72, r * 0.72, rgba(shade(color, 0.7)))
    for _ in range(420):
        a = s.r.uniform(0, TAU)
        r_ = math.sqrt(s.r.random()) * r * 0.7
        E(d, x + math.cos(a) * r_, y + math.sin(a) * r_, 2.2, 2.2, rgba(jit(color, s.r, 26)))
    s.put(lay, shadow=(6, 10, 10, 0.55))


def prop_lime(s, x, y, r=50, color=(88, 150, 44), inner=(214, 230, 120)):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, x, y, r, r, rgba(shade(color, 0.8)))
    E(d, x, y, r * 0.88, r * 0.88, rgba(lighten(inner, 0.35)))
    E(d, x, y, r * 0.78, r * 0.78, rgba(inner))
    for k in range(8):
        a = k * TAU / 8
        Ln(d, [(x + math.cos(a) * r * 0.12, y + math.sin(a) * r * 0.12), (x + math.cos(a) * r * 0.72, y + math.sin(a) * r * 0.72)], rgba(lighten(inner, 0.55)), 2.4)
    E(d, x, y, r * 0.1, r * 0.1, rgba(lighten(inner, 0.6)))
    s.put(lay, shadow=(4, 7, 6, 0.5), blur=0.5)


def prop_lemon(s, x, y, r=50):
    prop_lime(s, x, y, r, (214, 176, 40), (250, 226, 120))


def prop_leaf(s, x, y, rot=-0.35, size=80, color=(94, 112, 52)):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    P(d, shape_pts("blob", x, y, size, size * 0.3, rot), rgba(color))
    Ln(d, [(x - math.cos(rot) * size * 0.95, y - math.sin(rot) * size * 0.95), (x + math.cos(rot) * size * 0.95, y + math.sin(rot) * size * 0.95)], rgba(shade(color, 0.65)), 2.4)
    s.put(lay, shadow=(4, 7, 6, 0.5), blur=0.5)


def prop_tomato(s, x, y, r=46):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, x, y, r, r * 0.95, rgba((176, 38, 30)))
    E(d, x - r * 0.15, y - r * 0.2, r * 0.7, r * 0.6, rgba((214, 62, 46)))
    E(d, x - r * 0.3, y - r * 0.4, r * 0.28, r * 0.16, rgba((250, 170, 150), 200))
    for k in range(5):
        a = k * TAU / 5
        P(d, epoly(x + math.cos(a) * r * 0.2, y - r * 0.55 + math.sin(a) * r * 0.1, r * 0.3, r * 0.1, a, 10), rgba((70, 120, 50)))
    s.put(lay, shadow=(5, 8, 7, 0.55), blur=0.5)


def prop_chili(s, x, y, rot=0.5, size=70, color=(190, 36, 28)):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    pts = []
    for i in range(20):
        t = i / 19
        px = (t - 0.5) * size * 2
        py = math.sin(t * 2.2) * size * 0.22
        pts.append((x + px * math.cos(rot) - py * math.sin(rot), y + px * math.sin(rot) + py * math.cos(rot)))
    Ln(d, pts, rgba(shade(color, 0.75)), size * 0.2)
    Ln(d, pts, rgba(color), size * 0.15)
    Ln(d, [(px - 1, py - 2) for px, py in pts[2:-2]], rgba(lighten(color, 0.5), 160), size * 0.04)
    E(d, pts[0][0], pts[0][1], size * 0.07, size * 0.07, rgba((70, 120, 50)))
    s.put(lay, shadow=(4, 7, 6, 0.5), blur=0.5)


def prop_napkin(s, x, y, color=(200, 70, 60), rot=0.4, size=140):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    pts = shape_pts("rect", x, y, size, size * 0.7, rot, ex=14)
    P(d, pts, rgba(color))
    for k in range(-5, 6):
        off = k * size * 0.17
        Ln(d, [(x - size * 1.1 * math.cos(rot) - off * math.sin(rot), y - size * 1.1 * math.sin(rot) + off * math.cos(rot)),
               (x + size * 1.1 * math.cos(rot) - off * math.sin(rot), y + size * 1.1 * math.sin(rot) + off * math.cos(rot))], rgba(lighten(color, 0.55), 110), 7)
    s.clipped(lay, s.poly_mask(pts), shadow=(6, 10, 10, 0.55))


def prop_spoon(s, x, y, rot=-0.6, size=130):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    cr, sr = math.cos(rot), math.sin(rot)
    Ln(d, [(x - cr * size, y - sr * size), (x + cr * size * 0.35, y + sr * size * 0.35)], rgba((184, 186, 190)), 12)
    P(d, epoly(x + cr * size * 0.65, y + sr * size * 0.65, size * 0.36, size * 0.24, rot, 24), rgba((196, 198, 202)))
    P(d, epoly(x + cr * size * 0.65 - 3, y + sr * size * 0.65 - 3, size * 0.28, size * 0.17, rot, 24), rgba((226, 228, 232)))
    s.put(lay, shadow=(6, 10, 8, 0.5), blur=0.4)


def prop_dip(s, x, y, color=(60, 130, 56), r=58, bowl_kind="white"):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    o, m, i = BOWLS[bowl_kind]
    E(d, x, y, r, r, rgba(o))
    E(d, x, y, r * 0.92, r * 0.92, rgba(m))
    E(d, x, y, r * 0.78, r * 0.78, rgba(color))
    E(d, x - r * 0.25, y - r * 0.3, r * 0.3, r * 0.14, rgba(lighten(color, 0.4), 160))
    s.put(lay, shadow=(6, 10, 10, 0.55))


def prop_garlic(s, x, y, r=48):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, x, y, r, r * 0.9, rgba((226, 216, 196)))
    for k in range(6):
        a = k * TAU / 6
        Ln(d, [(x, y - r * 0.8), (x + math.cos(a) * r * 0.5, y + r * 0.6)], rgba((190, 176, 150)), 2.4)
    E(d, x - r * 0.2, y - r * 0.25, r * 0.4, r * 0.25, rgba((246, 240, 226), 200))
    s.put(lay, shadow=(5, 8, 7, 0.5), blur=0.5)


def prop_glass(s, x, y, color=(230, 170, 60), r=62):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    E(d, x, y, r, r, rgba((210, 220, 226)))
    E(d, x, y, r * 0.92, r * 0.92, rgba(shade(color, 0.7)))
    E(d, x + 3, y + 4, r * 0.86, r * 0.86, rgba(color))
    E(d, x - r * 0.3, y - r * 0.35, r * 0.25, r * 0.12, rgba((255, 255, 255), 130))
    s.put(lay, shadow=(6, 10, 10, 0.5))


def prop_cinnamon(s, x, y, rot=0.5):
    lay = s.lay()
    d = ImageDraw.Draw(lay)
    for off in (-10, 0, 10):
        a = rot + off * 0.02
        Ln(d, [(x - math.cos(a) * 60 + off, y - math.sin(a) * 60), (x + math.cos(a) * 60 + off, y + math.sin(a) * 60)], rgba((130, 70, 36)), 14)
        Ln(d, [(x - math.cos(a) * 60 + off - 1, y - math.sin(a) * 60 - 2), (x + math.cos(a) * 60 + off - 1, y + math.sin(a) * 60 - 2)], rgba((176, 108, 60), 200), 4)
    s.put(lay, shadow=(4, 7, 6, 0.5), blur=0.4)


PROPS = {
    "spice": prop_spice, "lime": prop_lime, "lemon": prop_lemon, "leaf": prop_leaf, "tomato": prop_tomato,
    "chili": prop_chili, "napkin": prop_napkin, "spoon": prop_spoon, "dip": prop_dip, "garlic": prop_garlic,
    "glass": prop_glass, "cinnamon": prop_cinnamon,
}
CORNERS = [(1010, 215), (190, 800), (180, 200), (1020, 800)]


def props(s, specs):
    """specs: list of kind or (kind, kwargs). Placed in free corners, away from the main vessel."""
    for i, sp in enumerate(specs):
        kind, kw = (sp, {}) if isinstance(sp, str) else sp
        x, y = CORNERS[i % len(CORNERS)]
        PROPS[kind](s, x, y, **kw)

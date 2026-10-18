#!/usr/bin/env python3
"""
Render the dish illustrations.

  python3 generate.py                      # all dishes
  python3 generate.py palak_paneer dish_021
  python3 generate.py --sheet              # contact sheets only, from existing output

Output (default, relative to this folder's repo root):
  build/dish-art/hires/<id>.jpg   1200px, uploaded to Firebase Storage by sync-dish-images.sh
  <bundled dir>/<id>.jpg          600px, <60KB, shipped inside the app as the offline fallback
"""
import argparse, csv, os, sys, time
from concurrent.futures import ProcessPoolExecutor
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)

HIRES = os.path.join(ROOT, "build", "dish-art", "hires")
BUNDLED = os.path.join(ROOT, "shared", "src", "commonMain", "composeResources", "files", "dishes")
BUNDLED_MAX_BYTES = 60 * 1024


def all_ids():
    import dishes
    return list(dishes.REG)


def render(id_):
    try:
        return _render(id_)
    except Exception as e:  # keep going; report at the end
        import traceback
        return id_, "ERR " + "".join(traceback.format_exception_only(type(e), e)).strip() + " @ " + str(traceback.extract_tb(e.__traceback__)[-1].lineno)


def _render(id_):
    import dishes
    from dishart import Scene
    bg, fn = dishes.REG[id_]
    s = Scene(dishes.seed_for(id_), bg)
    fn(s)
    img = s.finish()
    os.makedirs(HIRES, exist_ok=True)
    os.makedirs(BUNDLED, exist_ok=True)
    img.save(os.path.join(HIRES, id_ + ".jpg"), quality=88, optimize=True, progressive=True)
    small = img.resize((600, 500), Image.LANCZOS)
    q = 78
    path = os.path.join(BUNDLED, id_ + ".jpg")
    while True:
        small.save(path, quality=q, optimize=True, progressive=True)
        if os.path.getsize(path) <= BUNDLED_MAX_BYTES or q <= 40:
            break
        q -= 6
    return id_, os.path.getsize(path)


def sheet(ids, name, cols=4, tile=(400, 333)):
    rows = (len(ids) + cols - 1) // cols
    out = Image.new("RGB", (cols * tile[0], rows * tile[1]), (20, 20, 20))
    from PIL import ImageDraw
    d = ImageDraw.Draw(out)
    for i, id_ in enumerate(ids):
        p = os.path.join(HIRES, id_ + ".jpg")
        if not os.path.exists(p):
            continue
        im = Image.open(p).resize(tile, Image.LANCZOS)
        x, y = (i % cols) * tile[0], (i // cols) * tile[1]
        out.paste(im, (x, y))
        d.rectangle([x, y, x + 170, y + 14], fill=(0, 0, 0))
        d.text((x + 3, y + 1), id_, fill=(255, 255, 255))
    os.makedirs(os.path.join(ROOT, "build", "dish-art"), exist_ok=True)
    p = os.path.join(ROOT, "build", "dish-art", name)
    out.save(p, quality=85)
    return p


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("ids", nargs="*")
    ap.add_argument("--sheet", action="store_true")
    ap.add_argument("-j", type=int, default=4)
    a = ap.parse_args()
    import dishes
    every = all_ids()
    missing = [i for i in every if i not in dishes.REG]
    extra = [i for i in dishes.REG if i not in every]
    if missing or extra:
        print("missing recipes:", missing, "\nunknown ids:", extra)
    ids = a.ids or [i for i in every if i in dishes.REG]
    if not a.sheet:
        t = time.time()
        with ProcessPoolExecutor(a.j) as ex:
            for id_, size in ex.map(render, ids):
                print(f"{id_:28s} {size // 1024:3d} KB" if isinstance(size, int) else f"{id_:28s} {size}", flush=True)
        print(f"{len(ids)} done in {time.time() - t:.0f}s")
    for n in range(0, len(ids), 16):
        print(sheet(ids[n:n + 16], f"sheet_{n // 16 + 1}.jpg"))

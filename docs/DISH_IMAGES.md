# Dish photos

150 dishes, two copies of each: a high-resolution one served from Firebase Storage, and a
small one bundled in the app as an offline fallback.

The full list with filenames is in [`dish-images.csv`](dish-images.csv).

---

## Answering the obvious question: yes, both copies, same photo

The same image goes in **both** places, at **different sizes**:

| | Where | Size | Purpose |
| --- | --- | --- | --- |
| Hosted | Firebase Storage `dishes/<id>.jpg` | up to ~1200px, ~200KB | what users normally see |
| Bundled | `shared/src/commonMain/composeResources/files/dishes/<id>.jpg` | ~600px, **under 60KB** | offline / hosted-image-failed fallback |

Why both: the hosted copy can be updated without shipping an app release, and doesn't bloat the
download. The bundled copy means the app still looks right on a plane or a dead connection.

Why the bundled one must be small: every byte ships in **both** the Android and iOS downloads.
At 60KB × 150 that's ~9MB. At full resolution it would be 30MB+, which hurts install conversion.

Skipping the bundled copy is fine — that dish just falls back to the gradient when offline.

---

## Step 1 — Create the Storage bucket ✅ done

Verified live: bucket exists in **ASIA-SOUTH1**, and rules are published with public read working.
Kept here for reference / rebuilding the project elsewhere.

1. [Firebase Console](https://console.firebase.google.com/project/heyum-de1f9/storage) → **Build → Storage** → **Get started**
2. **Start in production mode** → Next
3. Location: **asia-south1** (Mumbai) — matches your existing Cloud Functions region and is
   closest to your users. *This cannot be changed later.*
4. Done.

### Make the photos publicly readable

Dish photos are not user data; the app reads them without signing in. In **Storage → Rules**,
publish:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Dish photos: world-readable, writable only from the console/CLI.
    match /dishes/{image} {
      allow read: if true;
      allow write: if false;
    }
    // Everything else stays locked down.
    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

`allow write: if false` blocks writes from *clients* only — you can still upload from the
console and with `gsutil`.

---

## Step 2 — Get the images

`dish-images.csv` has a `suggested_source` column splitting the work:

- **88 dishes → `stock`** — Italian, Mexican, Japanese, Thai, Mediterranean, Chinese, healthy,
  fast food. Generic food photography represents these accurately.
- **62 dishes → `ai-generate`** — Indian, street food, comfort. Free stock coverage for vada pav,
  dabeli, chole bhature and similar is poor to nonexistent, and a wrong photo is worse than a
  gradient.

Free stock that is safe for commercial use without attribution: [Unsplash](https://unsplash.com),
[Pexels](https://pexels.com). Avoid Google Images — those are almost always someone's copyright.

**Naming is the only hard requirement:** each file must be named exactly `<dish_id>.jpg`, taken
from the `filename` column. `palak_paneer.jpg`, `dish_101.jpg`, and so on. The app resolves images
by that name; a typo means the dish silently falls back to a gradient.

---

## Step 3 — Wire them up

Put every image in one folder named `<dish_id>.jpg`, then let the script do the rest:

```bash
./scripts/sync-dish-images.sh ~/dish-images
```

That runs **check-only** and changes nothing. It reports how many dishes are covered, and — most
importantly — flags filenames that don't match any dish id. A typo like `palak_panner.jpg` would
otherwise fail silently at runtime, showing a gradient with no error anywhere.

When the report looks right:

```bash
./scripts/sync-dish-images.sh ~/dish-images --apply
```

which then:

1. resizes every image into `composeResources/files/dishes/` at 600px / q75 (the bundled fallback),
   warning if the folder grows past ~20MB,
2. uploads the full-resolution originals to `gs://heyum-de1f9.firebasestorage.app/dishes/` with a
   24-hour cache header,
3. fetches one uploaded image back over HTTP to prove the bucket, path, and rules all line up.

You can run it repeatedly as images trickle in — it only touches the files present in the folder,
so adding 10 more dishes later is the same command.

**No code or database change is needed.** The app derives each URL from the dish id
(`storageUrlForDish` in `DishImage.kt`), so uploading the file is the whole step.

## Step 4 — Test

Rebuild and check in this order:

1. **Online** — dish cards show the hosted photo.
2. **Airplane mode** — cards still show photos, from the bundled copies.
3. **A dish with no image at all** — falls back to the cuisine gradient and emoji, no crash, no
   blank box.

Verify a single URL resolves before doing the whole batch:

```bash
curl -sI "https://firebasestorage.googleapis.com/v0/b/heyum-de1f9.firebasestorage.app/o/dishes%2Fpalak_paneer.jpg?alt=media" | head -1
```

`HTTP/2 200` means the bucket, path and rules are all correct. `403` means the rules from Step 1
weren't published; `404` means the filename doesn't match the dish id.

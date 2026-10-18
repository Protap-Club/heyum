# Bundled dish photos (offline fallback)

Drop one file per dish here, named exactly `<dish id>.jpg` — e.g. `dish_101.jpg`,
`palak_paneer.jpg`. Dish ids are in
`shared/src/commonMain/kotlin/com/pgstorm148/heyum/model/DishesPool.kt`.

These are the **offline fallback**, not the primary image. `DishImage` loads
`Dish.imageUrl` (Firebase Storage) first and only falls back to the file here when the
device is offline or the hosted image fails.

Because every one of these ships inside the app download, keep them small:

- ~600px on the long edge
- JPEG quality ~75
- target under 60KB each

At 60KB × 150 dishes that's ~9MB added to both the Android and iOS downloads. The
high-resolution versions belong in Firebase Storage, not here.

Resize a folder of images in place:

    sips -Z 600 *.jpg

No code change is needed when adding a file — `DishImage` resolves the path from the
dish id at runtime. A dish with no file here simply keeps the gradient + emoji.

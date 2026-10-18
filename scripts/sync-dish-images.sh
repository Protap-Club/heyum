#!/usr/bin/env bash
#
# Validate, resize, and upload dish photos.
#
#   ./scripts/sync-dish-images.sh ~/dish-images            # check only, changes nothing
#   ./scripts/sync-dish-images.sh ~/dish-images --apply    # resize + upload
#
# Source folder should hold full-resolution images named <dish_id>.jpg, e.g.
# palak_paneer.jpg. Names come from docs/dish-images.csv.
#
# The check pass runs first and always: it catches filenames that don't match any
# dish, which would otherwise fail silently at runtime (the dish just shows a
# gradient and you'd never know why).

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CSV="$REPO_ROOT/docs/dish-images.csv"
BUNDLED_DIR="$REPO_ROOT/shared/src/commonMain/composeResources/files/dishes"
BUCKET="gs://heyum-de1f9.firebasestorage.app"
GSUTIL="${GSUTIL:-/Users/family/google-cloud-sdk/bin/gsutil}"

# Bundled copies ship inside both app downloads, so they stay small.
# Override from the environment. BUNDLED_MAX_PX=0 keeps the source images exactly as they are
# (no resize, no recompression) — best quality, at the cost of a bigger app download.
BUNDLED_MAX_PX="${BUNDLED_MAX_PX:-600}"
BUNDLED_QUALITY="${BUNDLED_QUALITY:-75}"

SRC_DIR="${1:-}"
APPLY=0
[[ "${2:-}" == "--apply" ]] && APPLY=1

info() { printf '\n\033[1;34m==>\033[0m \033[1m%s\033[0m\n' "$1"; }
warn() { printf '\033[1;33m  ! %s\033[0m\n' "$1"; }
fail() { printf '\n\033[1;31mError:\033[0m %s\n' "$1" >&2; exit 1; }

[[ -n "$SRC_DIR" ]] || fail "Usage: $0 <folder-of-images> [--apply]"
[[ -d "$SRC_DIR" ]] || fail "Not a folder: $SRC_DIR"
[[ -f "$CSV" ]] || fail "Missing $CSV"
command -v sips >/dev/null || fail "sips not found (should be built into macOS)"

# --- Validate ---------------------------------------------------------------
info "Checking filenames against the dish catalog"

VALID_IDS=$(tail -n +2 "$CSV" | cut -d, -f2)
TOTAL_DISHES=$(echo "$VALID_IDS" | wc -l | tr -d ' ')

present=0; unknown=0
declare -a UNKNOWN_FILES=()
declare -a PRESENT_IDS=()

shopt -s nullglob nocaseglob
for f in "$SRC_DIR"/*.jpg "$SRC_DIR"/*.jpeg "$SRC_DIR"/*.png; do
  base="$(basename "$f")"
  id="${base%.*}"
  if echo "$VALID_IDS" | grep -qx "$id"; then
    present=$((present+1)); PRESENT_IDS+=("$id")
  else
    unknown=$((unknown+1)); UNKNOWN_FILES+=("$base")
  fi
done
shopt -u nullglob nocaseglob

echo "  matched : $present / $TOTAL_DISHES dishes"

if (( unknown > 0 )); then
  warn "$unknown file(s) don't match any dish id and will be ignored:"
  for u in "${UNKNOWN_FILES[@]}"; do echo "      $u"; done
  echo "    Compare against the 'filename' column in docs/dish-images.csv."
fi

missing=$((TOTAL_DISHES - present))
if (( missing > 0 )); then
  warn "$missing dish(es) still have no image — they'll fall back to the gradient."
  echo "    First few:"
  for id in $VALID_IDS; do
    if ! printf '%s\n' "${PRESENT_IDS[@]:-}" | grep -qx "$id"; then echo "      $id"; fi
  done | head -8 || true
fi

(( present > 0 )) || fail "No usable images found in $SRC_DIR"

if (( APPLY == 0 )); then
  info "Check only — nothing changed. Re-run with --apply to resize and upload."
  exit 0
fi

# --- Resize into the bundled folder ----------------------------------------
if (( BUNDLED_MAX_PX == 0 )); then
  info "Writing bundled copies (originals, untouched)"
else
  info "Writing bundled fallbacks (${BUNDLED_MAX_PX}px, q${BUNDLED_QUALITY})"
fi
mkdir -p "$BUNDLED_DIR"

shopt -s nullglob nocaseglob
for f in "$SRC_DIR"/*.jpg "$SRC_DIR"/*.jpeg "$SRC_DIR"/*.png; do
  base="$(basename "$f")"; id="${base%.*}"
  echo "$VALID_IDS" | grep -qx "$id" || continue
  # Always writes .jpg regardless of source extension, since DishImage looks for .jpg.
  if (( BUNDLED_MAX_PX == 0 )) && [[ "${base##*.}" =~ ^([jJ][pP][eE]?[gG])$ ]]; then
    cp "$f" "$BUNDLED_DIR/$id.jpg"
  else
    sips -Z "$BUNDLED_MAX_PX" -s format jpeg -s formatOptions "$BUNDLED_QUALITY" \
         "$f" --out "$BUNDLED_DIR/$id.jpg" >/dev/null 2>&1 \
      || warn "resize failed: $base"
  fi
done
shopt -u nullglob nocaseglob

BUNDLE_SIZE=$(du -sh "$BUNDLED_DIR" | cut -f1)
echo "  bundled folder is now $BUNDLE_SIZE"
# Roughly 9MB is expected at 150 images; well past that means the resize didn't take.
BUNDLE_MB=$(du -sm "$BUNDLED_DIR" | cut -f1)
if (( BUNDLED_MAX_PX > 0 && BUNDLE_MB > 20 )); then
  warn "That's larger than expected — every byte ships in both app downloads. Check the resize."
fi

# --- Upload full resolution -------------------------------------------------
# Everything is normalised to .jpg first: the app builds URLs ending in .jpg
# (storageUrlForDish), so a PNG uploaded as-is would 404 even though the file is there.
# PNG/JPEG sources are both fine — this is just the conversion step.
info "Preparing full-resolution uploads"
[[ -x "$GSUTIL" ]] || fail "gsutil not found at $GSUTIL (override with GSUTIL=...)"

STAGE_DIR="$(mktemp -d)"
trap 'rm -rf "$STAGE_DIR"' EXIT

converted=0
shopt -s nullglob nocaseglob
for f in "$SRC_DIR"/*.jpg "$SRC_DIR"/*.jpeg "$SRC_DIR"/*.png; do
  base="$(basename "$f")"; id="${base%.*}"
  echo "$VALID_IDS" | grep -qx "$id" || continue
  if [[ "${base##*.}" =~ ^([jJ][pP][gG])$ ]]; then
    cp "$f" "$STAGE_DIR/$id.jpg"
  else
    # Full quality here — this is the copy users actually see.
    sips -s format jpeg -s formatOptions 90 "$f" --out "$STAGE_DIR/$id.jpg" >/dev/null 2>&1 \
      || { warn "convert failed: $base"; continue; }
    converted=$((converted+1))
  fi
done
shopt -u nullglob nocaseglob

(( converted > 0 )) && echo "  converted $converted non-JPEG source(s) to .jpg"

info "Uploading to Firebase Storage"
"$GSUTIL" -m -h "Cache-Control:public, max-age=86400" \
  cp "$STAGE_DIR"/*.jpg "$BUCKET/dishes/" 2>&1 | tail -3

# --- Verify one actually serves --------------------------------------------
info "Verifying a served image"
PROBE="${PRESENT_IDS[0]}"
CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  "https://firebasestorage.googleapis.com/v0/b/heyum-de1f9.firebasestorage.app/o/dishes%2F$PROBE.jpg?alt=media")
case "$CODE" in
  200) echo "  HTTP 200 — $PROBE.jpg is live and publicly readable." ;;
  403) fail "HTTP 403 — Storage rules are blocking public read. Re-publish the rules in docs/DISH_IMAGES.md." ;;
  404) fail "HTTP 404 — upload didn't land at dishes/$PROBE.jpg." ;;
  *)   warn "HTTP $CODE — unexpected, worth checking in the console." ;;
esac

info "Done. Rebuild the app and check online, then airplane mode."

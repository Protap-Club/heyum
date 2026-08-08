#!/usr/bin/env bash
#
# One-command iOS release: archive → export → upload to TestFlight.
#
#   ./scripts/ios-release.sh              # auto-bumps build number, uploads
#   ./scripts/ios-release.sh --build 12   # use an explicit build number
#   ./scripts/ios-release.sh --no-upload  # archive + export only, no upload
#
# Auth comes from the App Store Connect API key stored by `asc auth login`
# (see docs/IOS_CD.md). No Apple ID password or app-specific password needed.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="$REPO_ROOT/iosApp/iosApp.xcodeproj"
SCHEME="iosApp"
BUNDLE_ID="com.pgstorm148.heyum.iosApp"
EXPORT_OPTIONS="$REPO_ROOT/iosApp/ExportOptions.plist"
BUILD_DIR="$REPO_ROOT/build/ios-release"
ARCHIVE_PATH="$BUILD_DIR/heyum.xcarchive"
EXPORT_DIR="$BUILD_DIR/export"

BUILD_NUMBER=""
DO_UPLOAD=1

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build) BUILD_NUMBER="$2"; shift 2 ;;
    --no-upload) DO_UPLOAD=0; shift ;;
    -h|--help) sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

info() { printf '\n\033[1;34m==>\033[0m \033[1m%s\033[0m\n' "$1"; }
fail() { printf '\n\033[1;31mError:\033[0m %s\n' "$1" >&2; exit 1; }

# --- Preflight -------------------------------------------------------------
command -v xcodebuild >/dev/null || fail "xcodebuild not found. Install Xcode and run: sudo xcode-select -s /Applications/Xcode.app/Contents/Developer"
[[ $DO_UPLOAD -eq 1 ]] && { command -v asc >/dev/null || fail "asc not found. Install with: brew install asc"; }

# xcodebuild needs a full Xcode, not just the Command Line Tools. Rather than demand
# a sudo xcode-select, point DEVELOPER_DIR at Xcode for this process when the global
# setting is wrong — same result, no password prompt.
if [[ -z "${DEVELOPER_DIR:-}" ]] && ! xcode-select -p 2>/dev/null | grep -q "Xcode.app"; then
  if [[ -d /Applications/Xcode.app ]]; then
    export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
    echo "Note: xcode-select points at the Command Line Tools; using DEVELOPER_DIR=$DEVELOPER_DIR for this build."
    echo "      To make it permanent: sudo xcode-select -s $DEVELOPER_DIR"
  else
    fail "Xcode not found at /Applications/Xcode.app. Install Xcode from the App Store."
  fi
fi

if [[ $DO_UPLOAD -eq 1 ]] && ! asc auth status --validate >/dev/null 2>&1; then
  fail "asc has no valid credentials. Run the login command in docs/IOS_CD.md, then: asc auth status --validate"
fi

# --- Build number ----------------------------------------------------------
# App Store Connect rejects a build number it has already seen, so default to
# "one past whatever is currently in the project".
if [[ -z "$BUILD_NUMBER" ]]; then
  CURRENT=$(xcodebuild -project "$PROJECT" -target "$SCHEME" -showBuildSettings -configuration Release 2>/dev/null \
    | awk '/ CURRENT_PROJECT_VERSION =/ {print $3; exit}')
  CURRENT=${CURRENT:-0}
  BUILD_NUMBER=$((CURRENT + 1))
fi

MARKETING_VERSION=$(xcodebuild -project "$PROJECT" -target "$SCHEME" -showBuildSettings -configuration Release 2>/dev/null \
  | awk '/ MARKETING_VERSION =/ {print $3; exit}')

info "Building heyum $MARKETING_VERSION ($BUILD_NUMBER)"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

# --- Archive ---------------------------------------------------------------
# CURRENT_PROJECT_VERSION is passed on the command line so the bump doesn't
# dirty the checked-in project file.
info "Archiving (this also builds the shared Kotlin framework)"
xcodebuild archive \
  -project "$PROJECT" \
  -scheme "$SCHEME" \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath "$ARCHIVE_PATH" \
  CURRENT_PROJECT_VERSION="$BUILD_NUMBER" \
  -allowProvisioningUpdates \
  | grep -E "error:|warning: .*(signing|provisioning)|ARCHIVE SUCCEEDED|ARCHIVE FAILED" || true

[[ -d "$ARCHIVE_PATH" ]] || fail "Archive failed — no .xcarchive produced. Re-run without the grep filter to see full output."

# Confirm the widget extension actually made it into the archive; a missing
# appex silently ships an app with no widget.
if [[ ! -d "$ARCHIVE_PATH/Products/Applications/iosApp.app/PlugIns/HeyumWidgetExtension.appex" ]]; then
  echo "Warning: HeyumWidgetExtension.appex is not in the archive — the widget will be missing from this build." >&2
fi

# --- Export ----------------------------------------------------------------
info "Exporting signed .ipa"
xcodebuild -exportArchive \
  -archivePath "$ARCHIVE_PATH" \
  -exportPath "$EXPORT_DIR" \
  -exportOptionsPlist "$EXPORT_OPTIONS" \
  -allowProvisioningUpdates \
  | grep -E "error:|EXPORT SUCCEEDED|EXPORT FAILED" || true

IPA=$(find "$EXPORT_DIR" -name '*.ipa' -maxdepth 1 | head -1)
[[ -n "$IPA" ]] || fail "Export failed — no .ipa produced. Check signing: the App Group and widget App ID must exist in the developer portal (see docs/IOS_CD.md)."

info "Built $(basename "$IPA") ($(du -h "$IPA" | cut -f1))"

if [[ $DO_UPLOAD -eq 0 ]]; then
  info "Skipping upload (--no-upload). IPA is at: $IPA"
  exit 0
fi

# --- Upload ----------------------------------------------------------------
info "Uploading to TestFlight"
# --verify-timeout makes the upload surface an immediate post-commit rejection
# (bad entitlements, duplicate build number) here rather than in a silent email.
asc builds upload \
  --app "$BUNDLE_ID" \
  --ipa "$IPA" \
  --verify-timeout 90s

info "Done. heyum $MARKETING_VERSION ($BUILD_NUMBER) is processing in App Store Connect."
echo "Track it with:  asc builds list --app $BUNDLE_ID --limit 5"

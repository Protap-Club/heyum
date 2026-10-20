# iOS release setup

Ship a TestFlight build with one command:

```bash
./scripts/ios-release.sh
```

That archives, signs, exports, and uploads. Everything below is the one-time setup
behind it.

---

## Why an API key instead of certificates

The common GitHub-Actions recipe stores a `.p12` certificate and a provisioning
profile as base64 secrets. That works, but the certificate and profile expire on
their own schedules and re-creating them needs Developer-portal access.

This setup uses an **App Store Connect API key** instead. One key covers uploads,
metadata, TestFlight, *and* signing (via `-allowProvisioningUpdates`, where Xcode
fetches or creates profiles itself). Keys don't expire unless revoked, and the
Account Holder can issue one without handing over their Apple ID.

---

## One-time setup

### 1. Create the API key — Rajesh (Account Holder) does this

1. **App Store Connect → Users and Access → Integrations → App Store Connect API**
2. **+**, name it `heyum-ci`, role **App Manager** (or Admin).
3. **Download the `.p8`.** It is downloadable exactly once.
4. Note the **Key ID** (10 chars) and **Issuer ID** (UUID, shown at the top of the page).

Send the `.p8` plus those two IDs over something private — it's a credential, not a
config file. Anyone holding it can upload builds and edit store metadata.

### 2. Register the App Group and widget App ID — needs Developer-portal access

The widget added in 1.1 will not sign until these exist:

1. **[developer.apple.com](https://developer.apple.com/account) → Identifiers → App Groups →** create
   `group.com.pgstorm148.heyum.widget`
2. **Identifiers → App IDs →** create `com.pgstorm148.heyum.iosApp.HeyumWidget`
3. Enable the **App Groups** capability on **both** `com.pgstorm148.heyum.iosApp` and the new
   widget App ID, and tick that group on each.

Without step 3 the app and widget can't share data, and the widget renders empty.

The group is `.widget`-suffixed because plain `group.com.pgstorm148.heyum` (and
`...heyum.iosApp`) are already registered to another Apple team, and group IDs are unique
across all of Apple. The group and both App IDs live under team `H58VG6UP2D`.

### 3. Store the key locally — on your Mac

```bash
brew install asc
mkdir -p ~/.appstoreconnect/private_keys
mv ~/Downloads/AuthKey_XXXXXXXXXX.p8 ~/.appstoreconnect/private_keys/
```

```bash
asc auth login --name heyum --key-id "XXXXXXXXXX" --issuer-id "YOUR-ISSUER-UUID" --private-key ~/.appstoreconnect/private_keys/AuthKey_XXXXXXXXXX.p8
```

Verify:

```bash
asc auth status --validate
```

Credentials land in the macOS Keychain, not in the repo.

### 4. Point Xcode at the right team

Already committed: `DEVELOPMENT_TEAM = H58VG6UP2D` in all four build configs.
It previously pointed at a **Personal Team**, which cannot submit to the App Store.

---

## Releasing

```bash
./scripts/ios-release.sh                 # auto-bumps the build number
./scripts/ios-release.sh --build 12      # explicit build number
./scripts/ios-release.sh --no-upload     # archive + export only, no upload
```

The build number auto-increments from `CURRENT_PROJECT_VERSION` and is passed on the
command line, so the release doesn't dirty the checked-in project file. Bump the
user-facing version (`MARKETING_VERSION`) in Xcode when you want 1.1 → 1.2.

The script fails loudly if the widget extension is missing from the archive, since
that silently ships an app with no widget.

---

## Useful `asc` commands

```bash
asc builds list --app com.pgstorm148.heyum.iosApp --limit 5
```

```bash
asc apps versions list --app com.pgstorm148.heyum.iosApp
```

```bash
asc auth doctor
```

`asc --help` lists the rest — it also covers screenshots, metadata localisation, and
submitting for review.

---

## CI (optional)

`.github/workflows/ios-release.yml` does the same thing on a `macos-15` runner,
triggered manually or by pushing an `ios-v*` tag.

Add three repository secrets under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `ASC_KEY_ID` | the 10-character Key ID |
| `ASC_ISSUER_ID` | the Issuer UUID |
| `ASC_PRIVATE_KEY_B64` | `openssl base64 < AuthKey_XXXX.p8 \| tr -d '\n'` |

Note that macOS runners bill at 10× the Linux rate on private repos, so the local
script is usually the cheaper habit.

---

## Troubleshooting

**`xcode-select points at the Command Line Tools`**
```bash
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

**`No profiles for 'com.pgstorm148.heyum.iosApp.HeyumWidget' were found`**
Step 2 hasn't been done, or the account signed into Xcode lacks Developer-portal
access. Check **Xcode → Settings → Accounts** shows the team without a red mark next
to *Certificates, Identifiers & Profiles*.

**`The provided entity includes an attribute with a value that has already been used`**
That build number is taken. Re-run with `--build <higher number>`.

**Upload succeeds but the build never appears**
It's processing; give it a few minutes, then `asc builds list`. If it vanished,
Apple emails the reason — usually a missing entitlement or an invalid icon.

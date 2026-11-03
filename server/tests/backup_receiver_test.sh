#!/usr/bin/env bash
# Contract test for backup_receiver.php against a RUNNING server (live GoDaddy or local php -S).
#
#   VRAJ_URL=https://example.com/vraj/backup_receiver.php \
#   VRAJ_KEY='<the key from vraj_config.php>' \
#   bash server/tests/backup_receiver_test.sh [path/to/test.zip]
#
# Covers: 405 (GET), 401 (wrong key), 400 (bad sha256), 200 ping, 200 upload.
# Makes 5 requests; the server allows 10/hour/IP by default, so do not loop it.
# The 200 upload is stored as vraj_20000101.zip (VRAJ_DATE) so it can never
# overwrite a real nightly backup. Delete it afterwards in cPanel File Manager.
# The key is never printed.

set -u

URL="${VRAJ_URL:?set VRAJ_URL to the full backup_receiver.php URL}"
KEY="${VRAJ_KEY:?set VRAJ_KEY to the API key}"
DATE="${VRAJ_DATE:-2000-01-01}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1
  elif command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | cut -d' ' -f1
  else openssl dgst -sha256 -r "$1" | cut -d' ' -f1
  fi
}

ZIP="${1:-}"
if [ -z "$ZIP" ]; then
  ZIP="$WORK/test.zip"
  echo '{"app":"vraj_kheti_hisab","version":"1.0","counts":{"txn":1}}' > "$WORK/manifest.json"
  if command -v zip >/dev/null 2>&1; then
    (cd "$WORK" && zip -q test.zip manifest.json)
  elif command -v python3 >/dev/null 2>&1; then
    python3 -c 'import sys,zipfile; z=zipfile.ZipFile(sys.argv[1],"w",zipfile.ZIP_DEFLATED); z.write(sys.argv[2],"manifest.json"); z.close()' "$ZIP" "$WORK/manifest.json"
  else
    echo "Need 'zip' or 'python3' to build a test zip, or pass one as the first argument." >&2
    exit 2
  fi
fi
SHA="$(sha256_of "$ZIP")"

PASS=0; FAIL=0
expect() { # expect <code> <label> <curl args...>
  local want="$1" label="$2"; shift 2
  local got
  got="$(curl -sS -o "$WORK/body" -w '%{http_code}' "$@")"
  if [ "$got" = "$want" ]; then PASS=$((PASS+1)); echo "PASS  $label ($got) $(cat "$WORK/body")"
  else FAIL=$((FAIL+1)); echo "FAIL  $label: want $want got $got $(cat "$WORK/body")"; fi
}

expect 405 "GET without ping"   "$URL"
expect 401 "POST wrong key"     -H "X-Vraj-Key: wrong-key-for-test" -F "file=@$ZIP;type=application/zip" -F "date=$DATE" -F "sha256=$SHA" "$URL"
expect 400 "POST bad sha256"    -H "X-Vraj-Key: $KEY" -F "file=@$ZIP;type=application/zip" -F "date=$DATE" -F "sha256=$(printf 'a%.0s' $(seq 1 64))" "$URL"
expect 200 "GET ping"           -H "X-Vraj-Key: $KEY" "$URL?ping=1"
expect 200 "POST valid upload"  -H "X-Vraj-Key: $KEY" -H "X-Vraj-Device: curl-test" -F "file=@$ZIP;type=application/zip" -F "date=$DATE" -F "sha256=$SHA" "$URL"

echo
echo "$PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]

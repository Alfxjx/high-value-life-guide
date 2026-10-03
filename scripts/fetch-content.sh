#!/usr/bin/env bash
set -euo pipefail

URL="https://raw.githubusercontent.com/cdyforever/how-to-live-better/main/index.html"
# 备用源：GitHub Contents API（raw.githubusercontent.com 在部分网络环境下被限速/阻断）
FALLBACK_URL="https://api.github.com/repos/cdyforever/how-to-live-better/contents/index.html"
OUT="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/assets/index.html"

mkdir -p "$(dirname "$OUT")"

if ! curl -fSL --connect-timeout 20 --max-time 60 --retry 1 --retry-delay 3 "$URL" -o "$OUT"; then
    echo "WARN: primary source failed, falling back to GitHub API" >&2
    curl -fSL --connect-timeout 30 --max-time 120 -H "Accept: application/vnd.github.raw" "$FALLBACK_URL" -o "$OUT"
fi

if [ ! -f "$OUT" ]; then
    echo "ERROR: download failed, $OUT does not exist" >&2
    exit 1
fi

SIZE=$(wc -c < "$OUT" | tr -d ' ')
if [ "$SIZE" -le 1048576 ]; then
    echo "ERROR: $OUT is only ${SIZE} bytes (< 1MB), likely not the real content" >&2
    exit 1
fi

echo "OK: downloaded $OUT (${SIZE} bytes)"

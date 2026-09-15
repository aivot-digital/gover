#!/bin/sh
# Run only in a disposable nginx container; this creates fixtures in /app/www.
set -eu

trap 'nginx -s quit >/dev/null 2>&1 || true' EXIT
mkdir -p /app/www/assets/fonts /app/www/staff/assets /app/www/sbom
cp /tests/nginx.conf /etc/nginx/http.d/default.conf
printf 'customer entry' > /app/www/index.html
printf 'staff entry' > /app/www/staff/index.html
printf '{}' > /app/www/sbom/manifest.json
printf 'unversioned' > /app/www/assets/runtime.js
printf 'font' > /app/www/assets/fonts/public-sans.woff2
printf 'not a hashed asset' > /app/www/assets/test-12345678Xjs
awk 'BEGIN {for (i = 0; i < 1000; i++) print "console.log(\"compression fixture\");"}' > /app/www/assets/index-Ab12_-34.js
cp /app/www/assets/index-Ab12_-34.js /app/www/staff/assets/index-Ab12_-34.js
awk 'BEGIN {for (i = 0; i < 1000; i++) print ".fixture { color: black; }"}' > /app/www/assets/index-Ab12_-34.css
node /tests/compress-assets.mjs /app/www/assets
node /tests/compress-assets.mjs /app/www/staff/assets
cp /app/www/assets/index-Ab12_-34.js /app/www/assets/plain-Ab12_-34.js
cp /app/www/assets/index-Ab12_-34.js.br /app/www/assets/orphan-Ab12_-34.js.br
nginx -t
nginx

fetch() {
    curl --silent --show-error --location --max-time 10 --dump-header /tmp/headers --output /tmp/body "$@"
}

for asset_path in /assets/index-Ab12_-34.js /staff/assets/index-Ab12_-34.js /assets/index-Ab12_-34.css; do
    fetch "http://127.0.0.1:8080$asset_path"
    grep -q 'HTTP/1.1 200' /tmp/headers
    grep -q 'Cache-Control: public, max-age=31536000, immutable' /tmp/headers
done

# Cached variants need distinct validators and must keep Vary even on a 304 response.
fetch --header='Accept-Encoding: br' http://127.0.0.1:8080/assets/index-Ab12_-34.js
brotli_etag=$(awk 'tolower($1) == "etag:" {gsub("\r", "", $2); print $2}' /tmp/headers)
test -n "$brotli_etag"
fetch --head --header='Accept-Encoding: br' http://127.0.0.1:8080/assets/index-Ab12_-34.js
grep -q 'HTTP/1.1 200' /tmp/headers
grep -q 'Content-Encoding: br' /tmp/headers
grep -q 'Vary: Accept-Encoding' /tmp/headers
fetch --header='Accept-Encoding: br' --header="If-None-Match: $brotli_etag" http://127.0.0.1:8080/assets/index-Ab12_-34.js
grep -q 'HTTP/1.1 304' /tmp/headers
grep -q 'Vary: Accept-Encoding' /tmp/headers
grep -q 'Cache-Control: public, max-age=31536000, immutable' /tmp/headers
fetch --header='Accept-Encoding: identity' --header="If-None-Match: $brotli_etag" http://127.0.0.1:8080/assets/index-Ab12_-34.js
grep -q 'HTTP/1.1 200' /tmp/headers
cmp /tmp/body /app/www/assets/index-Ab12_-34.js

fetch --header='Accept-Encoding: gzip' http://127.0.0.1:8080/assets/index-Ab12_-34.js
grep -q 'Content-Encoding: gzip' /tmp/headers
grep -q 'Vary: Accept-Encoding' /tmp/headers
gzip -dc /tmp/body > /tmp/decoded
cmp /tmp/decoded /app/www/assets/index-Ab12_-34.js

for asset_path in /assets/index-Ab12_-34.js /staff/assets/index-Ab12_-34.js /assets/index-Ab12_-34.css; do
    for encoding in br 'gzip, br'; do
        fetch --header="Accept-Encoding: $encoding" "http://127.0.0.1:8080$asset_path"
        grep -q 'HTTP/1.1 200' /tmp/headers
        grep -q 'Content-Encoding: br' /tmp/headers
        grep -q 'Vary: Accept-Encoding' /tmp/headers
        grep -q 'Cache-Control: public, max-age=31536000, immutable' /tmp/headers
        case "$asset_path" in
            *.css) grep -q 'Content-Type: text/css' /tmp/headers ;;
            *.js) grep -q 'Content-Type: application/javascript' /tmp/headers ;;
        esac
        node -e 'const fs = require("node:fs"); fs.writeFileSync("/tmp/decoded", require("node:zlib").brotliDecompressSync(fs.readFileSync("/tmp/body")));'
        cmp /tmp/decoded "/app/www$asset_path"
    done
    for encoding in identity 'br;q=0'; do
        fetch --header="Accept-Encoding: $encoding" "http://127.0.0.1:8080$asset_path"
        if grep -q 'Content-Encoding:' /tmp/headers; then exit 1; fi
        grep -q 'Vary: Accept-Encoding' /tmp/headers
        cmp /tmp/body "/app/www$asset_path"
    done
done

fetch --header='Accept-Encoding: gzip, br;q=0' http://127.0.0.1:8080/assets/index-Ab12_-34.js
grep -q 'Content-Encoding: gzip' /tmp/headers
gzip -dc /tmp/body > /tmp/decoded
cmp /tmp/decoded /app/www/assets/index-Ab12_-34.js

# A missing sidecar remains a valid original response; gzip-only clients still use gzip.
fetch --header='Accept-Encoding: br' http://127.0.0.1:8080/assets/plain-Ab12_-34.js
if grep -q 'Content-Encoding:' /tmp/headers; then exit 1; fi
cmp /tmp/body /app/www/assets/plain-Ab12_-34.js
fetch --header='Accept-Encoding: gzip' http://127.0.0.1:8080/assets/plain-Ab12_-34.js
grep -q 'Content-Encoding: gzip' /tmp/headers

for asset_path in /assets/missing-Ab12_-34.js /staff/assets/missing-Ab12_-34.js /assets/missing.js /staff/assets/missing.css /assets/orphan-Ab12_-34.js; do
    fetch --header='Accept-Encoding: gzip, br' "http://127.0.0.1:8080$asset_path"
    grep -q 'HTTP/1.1 404' /tmp/headers
    if grep -q 'immutable' /tmp/headers || grep -q 'entry' /tmp/body; then
        exit 1
    fi
done

for asset_path in /assets/runtime.js /assets/fonts/public-sans.woff2 /assets/test-12345678Xjs; do
    fetch "http://127.0.0.1:8080$asset_path"
    grep -q 'HTTP/1.1 200' /tmp/headers
    grep -q 'Cache-Control: no-cache' /tmp/headers
    if grep -q 'immutable' /tmp/headers; then exit 1; fi
done

for route_path in / /form/example /staff /staff/processes /sbom/manifest.json; do
    fetch "http://127.0.0.1:8080$route_path"
    grep -q 'HTTP/1.1 200' /tmp/headers
    grep -q 'Cache-Control: no-store, no-cache, must-revalidate' /tmp/headers
    if grep -q 'immutable' /tmp/headers; then exit 1; fi
done

printf 'Nginx compression, cache and missing-asset checks passed.\n'

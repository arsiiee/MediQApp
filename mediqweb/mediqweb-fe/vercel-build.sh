#!/bin/sh
# Vercel build script for MediQ Angular frontend.
# Builds the app, then injects runtime env vars into dist/mediqapp-fe/index.html.
set -e

npm run build

API_BASE_URL="${API_BASE_URL:-http://localhost:8099}"
BFF_BASE_URL="${BFF_BASE_URL:-http://localhost:3000}"

sed -i "s|window.__env.API_BASE_URL = '[^']*'|window.__env.API_BASE_URL = '${API_BASE_URL}'|g" dist/mediqapp-fe/index.html
sed -i "s|window.__env.BFF_BASE_URL = '[^']*'|window.__env.BFF_BASE_URL = '${BFF_BASE_URL}'|g" dist/mediqapp-fe/index.html

# Fail if placeholders were not replaced (injection did not happen)
if grep -q '__API_BASE_URL__' dist/mediqapp-fe/index.html || grep -q '__BFF_BASE_URL__' dist/mediqapp-fe/index.html; then
    echo "ERROR: environment variables were not injected into index.html" >&2
    exit 1
fi

# Fail if localhost would be deployed
if grep -q 'localhost' dist/mediqapp-fe/index.html; then
    echo "ERROR: localhost detected in index.html - refusing to deploy" >&2
    exit 1
fi

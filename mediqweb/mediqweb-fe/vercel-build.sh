#!/bin/sh
# Vercel build script for MediQ Angular frontend.
# Injects runtime environment variables into index.html via window.__env,
# then runs the Angular production build.
set -e

sed -i "s|__API_BASE_URL__|${API_BASE_URL:-http://localhost:8099}|g" src/index.html
sed -i "s|__BFF_BASE_URL__|${BFF_BASE_URL:-http://localhost:3000}|g" src/index.html

npm run build

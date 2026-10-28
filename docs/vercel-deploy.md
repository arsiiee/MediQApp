# Vercel Deployment — Angular Frontend Notes

## Symptom

Browser console shows:

```
runtime.b7ca919b011f849f.js:1 Failed to load module script: Expected a JavaScript-or-Wasm module script but the server responded with a MIME type of "text/html". Strict MIME type checking is enforced for module scripts per HTML spec.
```

## Root Cause

The Angular production build outputs to `dist/mediqapp-fe` (set in `angular.json`). Vercel's static build serves files from that directory.

The original `vercel.json` catch-all route:

```json
{ "src": "/(.*)", "dest": "/index.html" }
```

matched **every** request — including `.js` and `.css` asset URLs. Vercel returned `index.html` (`text/html`) instead of the actual JavaScript file, so the browser rejected it with a strict MIME-type error.

## Fix

`mediqweb/mediqweb-fe/vercel.json` now serves static assets directly and only falls back to `index.html` for pathless routes:

```json
{
  "builds": [
    { "src": "package.json", "use": "@vercel/static-build", "config": { "distDir": "dist/mediqapp-fe" } }
  ],
  "routes": [
    { "src": "/assets/(.*)", "headers": { "Cache-Control": "public, max-age=31536000, immutable" }, "continue": true },
    { "src": "/(.+\\..+)", "dest": "/$1" },
    { "src": "/(.*)", "dest": "/index.html" }
  ]
}
```

- `/(.+\\..+)` serves files with extensions (`.js`, `.css`, `.ico`, etc.) directly — correct MIME type.
- `/(.*)` is the SPA fallback for client-side routes (`/login`, `/appointments`, etc.).

## Deployment

Commit and push `vercel.json`, then redeploy on Vercel:

```powershell
git add mediqweb/mediqweb-fe/vercel.json
git commit -m "fix(vercel): serve static JS/CSS assets directly before SPA fallback"
git push origin <branch>
```

The MIME error resolves because `.js` files are served with `application/javascript` rather than `index.html` (`text/html`).

## Related

- Build output dir: `dist/mediqapp-fe` (`mediqweb/mediqweb-fe/angular.json`)
- Vercel config: `mediqweb/mediqweb-fe/vercel.json`
- Build command: `npm run build` (`ng build`)

// MediQ web frontend environment configuration.
// apiBaseUrl / bffBaseUrl are read from window.__env at runtime (injected via index.html),
// falling back to localhost for local development. Vercel sets these at deploy time.
export const environment = {
  production: false,
  get apiBaseUrl(): string {
    return (window as unknown as { __env?: Record<string, string> })?.__env?.['API_BASE_URL'] ?? 'http://localhost:8099';
  },
  get bffBaseUrl(): string {
    return (window as unknown as { __env?: Record<string, string> })?.__env?.['BFF_BASE_URL'] ?? 'http://localhost:3000';
  },
  useHttpApi: true,
};

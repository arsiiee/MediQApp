// MediQ web frontend environment configuration.
// apiBaseUrl / bffBaseUrl are read from window.__env at runtime (injected via index.html).
// Vercel sets these at deploy time; local dev uses .env.
export const environment = {
  production: false,
  get apiBaseUrl(): string | undefined {
    return (window as unknown as { __env?: Record<string, string> })?.__env?.['API_BASE_URL'];
  },
  useHttpApi: true,
};

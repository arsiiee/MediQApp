export const environment = {
  production: true,
  get apiBaseUrl(): string {
    return (window as unknown as { __env?: Record<string, string> })?.__env?.['API_BASE_URL'] ?? 'http://localhost:8099';
  },
  get bffBaseUrl(): string {
    return (window as unknown as { __env?: Record<string, string> })?.__env?.['BFF_BASE_URL'] ?? 'http://localhost:3000';
  },
  useHttpApi: true,
};

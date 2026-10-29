export const environment = {
  production: true,
  get apiBaseUrl(): string | undefined {
    return (window as unknown as { __env?: Record<string, string> })?.__env?.['API_BASE_URL'];
  },
  useHttpApi: true,
};

// MediQ web frontend environment configuration.
// Values are injected at build/runtime from window.__env (see src/index.html and vercel-build.sh).
declare global {
  interface Window {
    __env?: Record<string, string | undefined>;
  }
}

const runtime = typeof window !== 'undefined' ? window.__env ?? {} : {};

export const environment = {
  production: false,
  supabaseUrl: runtime['SUPABASE_URL'] ?? '',
  supabaseAnonKey: runtime['SUPABASE_ANON_KEY'] ?? '',
  supabaseServiceRoleKey: runtime['SUPABASE_SERVICE_ROLE_KEY'],
  useHttpApi: true,
};
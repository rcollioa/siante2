/** Prefijo de todas las rutas del backend. Debe coincidir con `app.api.prefijo` del backend
 * (`ApiPrefijoConfig`), con los `proxy.conf*.json` y con el `path` de los ingress de k8s. */
export const API_PREFIJO = '/siante2/api';

/** Si la petición va al backend propio — los interceptores solo actúan sobre esas. */
export function esRutaApi(url: string): boolean {
  return url.startsWith(`${API_PREFIJO}/`);
}

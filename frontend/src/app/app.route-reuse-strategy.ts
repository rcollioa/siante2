import { ActivatedRouteSnapshot, DetachedRouteHandle, RouteReuseStrategy } from '@angular/router';

/**
 * Por defecto, Angular solo reutiliza la instancia de un componente cuando `future.routeConfig` y
 * `curr.routeConfig` son el mismo objeto (p. ej. solo cambió un parámetro de la misma ruta). Al navegar
 * entre dos rutas distintas del array `routes`, aunque apunten al mismo componente (`/capa-a` y
 * `/capa-b`), destruye la instancia vieja y crea una nueva, perdiendo el estado ya cargado.
 *
 * Esta estrategia agrega una única excepción: si ambas rutas comparten `data.reuseGrupo`, se reutiliza
 * la instancia igual que si fuera la misma ruta, y el componente reacciona al nuevo `route.data` en vez
 * de recrearse. No usa `shouldDetach`/`store`: no guarda instancias para volver después, solo evita
 * destruirlas al pasar de una ruta del grupo a otra.
 */
export class GrupoRouteReuseStrategy implements RouteReuseStrategy {
  shouldDetach(): boolean {
    return false;
  }

  store(): void {
    // No-op: sin detach no hay nada que guardar.
  }

  shouldAttach(): boolean {
    return false;
  }

  retrieve(): DetachedRouteHandle | null {
    return null;
  }

  shouldReuseRoute(future: ActivatedRouteSnapshot, curr: ActivatedRouteSnapshot): boolean {
    if (future.routeConfig === curr.routeConfig) {
      return true; // Comportamiento estándar de Angular.
    }
    const grupo = future.data['reuseGrupo'];
    return grupo != null && grupo === curr.data['reuseGrupo'];
  }
}

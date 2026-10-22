import { ActivatedRouteSnapshot, DetachedRouteHandle, RouteReuseStrategy } from '@angular/router';

/** Reutiliza la instancia del componente entre rutas distintas con el mismo `data.reuseGrupo`;
 * el componente reacciona al nuevo `route.data` en vez de recrearse. */
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

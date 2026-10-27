import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { SesionService } from './sesion.service';

/** Sin sesión activa (sin permiso o sin backend) lleva a la página de no autorizado. Mientras se consulta la
 * sesión, ya descarga el componente de la ruta: el router lo pediría recién después de la guarda. */
export const sesionGuard: CanActivateFn = (route) => {
  const cargar = route.routeConfig?.loadComponent;
  if (cargar) {
    // Si falla, el router lo vuelve a intentar al activar la ruta y es él quien informa el error.
    Promise.resolve(cargar()).catch(() => undefined);
  }
  const router = inject(Router);
  return inject(SesionService).iniciar().pipe(
    map((estado) => (estado === 'activa' ? true : router.createUrlTree(['/no-autorizado'])))
  );
};

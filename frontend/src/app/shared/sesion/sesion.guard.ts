import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { SesionService } from './sesion.service';

/** Sin sesión activa (sin permiso o sin backend) lleva a la página de no autorizado. */
export const sesionGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(SesionService).iniciar().pipe(
    map((estado) => (estado === 'activa' ? true : router.createUrlTree(['/no-autorizado'])))
  );
};

import { Routes } from '@angular/router';
import { InicioComponent } from './features/inicio/inicio.component';

// Cada funcionalidad vive en `features/<nombre>/` (componente + `<nombre>-api.service.ts`) y se
// registra aquí. Para que dos rutas distintas compartan la misma instancia del componente al navegar
// entre ellas, darles el mismo `data.reuseGrupo` (ver GrupoRouteReuseStrategy).
export const routes: Routes = [
	{ path: '', pathMatch: 'full', redirectTo: 'inicio' },
	{ path: 'inicio', component: InicioComponent },
	{ path: '**', redirectTo: 'inicio' }
];

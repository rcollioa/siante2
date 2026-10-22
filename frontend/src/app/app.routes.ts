import { Routes } from '@angular/router';
import { InicioComponent } from './features/inicio/inicio.component';

// Cada funcionalidad vive en `features/<nombre>/` y se registra aquí.
// Rutas con el mismo `data.reuseGrupo` comparten instancia (ver GrupoRouteReuseStrategy).
export const routes: Routes = [
	{ path: '', pathMatch: 'full', redirectTo: 'inicio' },
	{ path: 'inicio', component: InicioComponent },
	{ path: '**', redirectTo: 'inicio' }
];

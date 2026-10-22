import { Routes } from '@angular/router';
import { sesionGuard } from './shared/sesion/sesion.guard';

// Cada funcionalidad vive en `features/<nombre>/` y se carga al entrar a su ruta (`loadComponent`).
// Rutas con el mismo `data.reuseGrupo` comparten instancia (ver GrupoRouteReuseStrategy).
export const routes: Routes = [
	{ path: '', pathMatch: 'full', redirectTo: 'mapa' },
	{
		path: 'mapa',
		canActivate: [sesionGuard],
		loadComponent: () => import('./features/mapa/mapa.component').then((m) => m.MapaComponent)
	},
	{
		path: 'no-autorizado',
		loadComponent: () => import('./features/no-autorizado/no-autorizado.component').then((m) => m.NoAutorizadoComponent)
	},
	{ path: '**', redirectTo: 'mapa' }
];

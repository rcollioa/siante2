import { Routes } from '@angular/router';
import { MapaComponent } from './features/mapa/mapa.component';

// Cada funcionalidad vive en `features/<nombre>/` y se registra aquí.
// Rutas con el mismo `data.reuseGrupo` comparten instancia (ver GrupoRouteReuseStrategy).
export const routes: Routes = [
	{ path: '', pathMatch: 'full', redirectTo: 'mapa' },
	{ path: 'mapa', component: MapaComponent },
	{ path: '**', redirectTo: 'mapa' }
];

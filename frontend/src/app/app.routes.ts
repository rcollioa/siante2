import { Routes } from '@angular/router';
import { MapaComponent } from './features/mapa/mapa.component';
import { NoAutorizadoComponent } from './features/no-autorizado/no-autorizado.component';
import { sesionGuard } from './shared/sesion/sesion.guard';

// Cada funcionalidad vive en `features/<nombre>/` y se registra aquí.
// Rutas con el mismo `data.reuseGrupo` comparten instancia (ver GrupoRouteReuseStrategy).
export const routes: Routes = [
	{ path: '', pathMatch: 'full', redirectTo: 'mapa' },
	{ path: 'mapa', component: MapaComponent, canActivate: [sesionGuard] },
	{ path: 'no-autorizado', component: NoAutorizadoComponent },
	{ path: '**', redirectTo: 'mapa' }
];

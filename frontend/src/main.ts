import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsCl from '@angular/common/locales/es-CL';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { AppComponent } from './app/app.component';
import { provideRouter, RouteReuseStrategy, withRouterConfig } from '@angular/router';
import { routes } from './app/app.routes';
import { GrupoRouteReuseStrategy } from './app/app.route-reuse-strategy';
import { pantallaCargaInterceptor } from './app/shared/pantalla-carga/pantalla-carga.interceptor';
import { tiempoLimiteInterceptor } from './app/shared/errores/tiempo-limite.interceptor';
import { reintentoInterceptor } from './app/shared/errores/reintento.interceptor';
import { refrescoCacheInterceptor } from './app/shared/cache/refresco-cache.interceptor';

// Números con formato chileno: 3.001 en vez de 3,001.
registerLocaleData(localeEsCl, 'es-CL');

bootstrapApplication(AppComponent, {
  providers: [
    { provide: LOCALE_ID, useValue: 'es-CL' },
    provideRouter(routes, withRouterConfig({ onSameUrlNavigation: 'reload' })),
    // Orden importa: pantallaCarga envuelve TODO (incluidos los reintentos) para no parpadear el
    // overlay en cada intento; reintento va antes que tiempoLimite para que cada intento tenga su
    // propio plazo completo, no uno compartido entre todos. refrescoCache va al final para que cada
    // reintento salga también con `?refrescar` aplicado.
    provideHttpClient(withInterceptors([
      pantallaCargaInterceptor, reintentoInterceptor, tiempoLimiteInterceptor, refrescoCacheInterceptor
    ])),
    { provide: RouteReuseStrategy, useClass: GrupoRouteReuseStrategy }
  ]
}).catch((err) => console.error(err));

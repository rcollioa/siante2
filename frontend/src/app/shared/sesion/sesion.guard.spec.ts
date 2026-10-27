import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Route, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { EstadoSesion, SesionService } from './sesion.service';
import { sesionGuard } from './sesion.guard';

describe('sesionGuard', () => {
  let estado: Subject<EstadoSesion>;

  beforeEach(() => {
    estado = new Subject<EstadoSesion>();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SesionService, useValue: { iniciar: () => estado } }]
    });
  });

  function activar(routeConfig: Route): Observable<boolean | UrlTree> {
    const ruta = { routeConfig } as ActivatedRouteSnapshot;
    return TestBed.runInInjectionContext(
      () => sesionGuard(ruta, {} as RouterStateSnapshot) as Observable<boolean | UrlTree>
    );
  }

  it('descarga el componente de la ruta sin esperar la respuesta de la sesión', () => {
    const cargar = vi.fn(() => Promise.resolve(class {}));

    activar({ path: 'mapa', loadComponent: cargar }).subscribe();

    expect(cargar).toHaveBeenCalledTimes(1);
  });

  it('con sesión activa deja pasar y sin permiso lleva a no autorizado', () => {
    let resultado: boolean | UrlTree | undefined;
    activar({ path: 'mapa' }).subscribe((r) => (resultado = r));
    estado.next('activa');
    expect(resultado).toBe(true);

    activar({ path: 'mapa' }).subscribe((r) => (resultado = r));
    estado.next('no-autorizado');
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/no-autorizado');
  });

  it('si la descarga del componente falla, no rompe la guarda', async () => {
    const cargar = vi.fn(() => Promise.reject(new Error('sin red')));
    let resultado: boolean | UrlTree | undefined;

    activar({ path: 'mapa', loadComponent: cargar }).subscribe((r) => (resultado = r));
    await Promise.resolve();
    estado.next('activa');

    expect(resultado).toBe(true);
  });
});

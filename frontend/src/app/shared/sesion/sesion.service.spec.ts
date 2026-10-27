import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EstadoSesion, SesionService } from './sesion.service';

describe('SesionService', () => {
  let sesion: SesionService;
  let http: HttpTestingController;
  const URL = '/siante2/api/usuarios/actual';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    sesion = TestBed.inject(SesionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function iniciar(): EstadoSesion | undefined {
    let estado: EstadoSesion | undefined;
    sesion.iniciar().subscribe((e) => (estado = e));
    return estado;
  }

  it('con usuario queda activa, guarda el nombre y no vuelve a consultar', () => {
    let estado: EstadoSesion | undefined;
    sesion.iniciar().subscribe((e) => (estado = e));
    http.expectOne(URL).flush({ id: 1, ivUser: 'jperez', nombre: 'Juan', apellido: 'Pérez', email: '', grupos: [] });

    expect(estado).toBe('activa');
    expect(sesion.nombreVisible()).toBe('Juan Pérez');
    iniciar();
    http.expectNone(URL);
  });

  it('un 401 deja la sesión como no autorizada', () => {
    let estado: EstadoSesion | undefined;
    sesion.iniciar().subscribe((e) => (estado = e));
    http.expectOne(URL).flush({ code: 'UNAUTHORIZED' }, { status: 401, statusText: 'Unauthorized' });

    expect(estado).toBe('no-autorizado');
  });

  it('al finalizar olvida al usuario y, si vuelve a ingresar, consulta de nuevo', () => {
    sesion.iniciar().subscribe();
    http.expectOne(URL).flush({ id: 1, ivUser: 'jperez', nombre: 'Juan', apellido: 'Pérez', email: '', grupos: [] });

    sesion.finalizar();
    expect(sesion.estado()).toBe('finalizada');
    expect(sesion.usuario()).toBeNull();

    iniciar();
    http.expectOne(URL);
  });

  it('un error de conexión permite reintentar con una consulta nueva', () => {
    sesion.iniciar().subscribe();
    http.expectOne(URL).flush(null, { status: 503, statusText: 'Service Unavailable' });
    expect(sesion.estado()).toBe('error');

    iniciar();
    http.expectOne(URL);
  });
});

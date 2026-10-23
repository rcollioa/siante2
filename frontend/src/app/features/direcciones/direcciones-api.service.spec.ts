import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DireccionesApiService } from './direcciones-api.service';

describe('DireccionesApiService', () => {
  let servicio: DireccionesApiService;
  let http: HttpTestingController;

  const direccion = { nombre: 'Teatinos 56, Santiago', latitud: -33.44, longitud: -70.65 };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(DireccionesApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envía el texto como parámetro y la misma búsqueda sale de la caché', () => {
    let recibidas: unknown;
    servicio.buscar('teatinos 56').subscribe((direcciones) => (recibidas = direcciones));

    http.expectOne('/siante2/api/direcciones?texto=teatinos%2056').flush({ data: [direccion] });
    expect(recibidas).toEqual([direccion]);

    servicio.buscar('teatinos 56').subscribe();
    http.expectNone('/siante2/api/direcciones?texto=teatinos%2056');
  });
});

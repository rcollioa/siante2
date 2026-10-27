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

  describe('sugerencias', () => {
    const santiago = { latitud: -33.44312, longitud: -70.65449, zoom: 12.4 };
    const sugerencias = '/siante2/api/direcciones/sugerencias';

    it('se piden a su propia ruta con la zona del mapa y se guardan aparte de la búsqueda completa', () => {
      servicio.sugerir('Teati', santiago).subscribe();
      http.expectOne(`${sugerencias}?texto=teati&latitud=-33.44&longitud=-70.65&zoom=12`).flush({ data: [direccion] });

      servicio.sugerir('teati ', santiago).subscribe();
      http.expectNone(() => true);

      servicio.buscar('teati').subscribe();
      http.expectOne('/siante2/api/direcciones?texto=teati').flush({ data: [] });
    });

    it('mover un poco el mapa no repite la consulta; otra zona sí', () => {
      servicio.sugerir('teati', santiago).subscribe();
      http.expectOne((r) => r.url === sugerencias).flush({ data: [direccion] });

      servicio.sugerir('teati', { latitud: -33.4429, longitud: -70.6501, zoom: 12 }).subscribe();
      http.expectNone(() => true);

      servicio.sugerir('teati', { latitud: -29.9, longitud: -71.25, zoom: 12 }).subscribe();
      http.expectOne(`${sugerencias}?texto=teati&latitud=-29.9&longitud=-71.25&zoom=12`).flush({ data: [] });
    });
  });

  it('envía el texto como parámetro y la misma búsqueda sale de la caché', () => {
    let recibidas: unknown;
    servicio.buscar('teatinos 56').subscribe((direcciones) => (recibidas = direcciones));

    http.expectOne('/siante2/api/direcciones?texto=teatinos%2056').flush({ data: [direccion] });
    expect(recibidas).toEqual([direccion]);

    servicio.buscar('teatinos 56').subscribe();
    http.expectNone('/siante2/api/direcciones?texto=teatinos%2056');
  });

  it('espacios y mayúsculas no cambian la búsqueda: se pide una sola vez', () => {
    servicio.buscar('  Teatinos   56 ').subscribe();
    http.expectOne('/siante2/api/direcciones?texto=teatinos%2056').flush({ data: [direccion] });

    servicio.buscar('teatinos 56').subscribe();
    http.expectNone('/siante2/api/direcciones?texto=teatinos%2056');
  });
});

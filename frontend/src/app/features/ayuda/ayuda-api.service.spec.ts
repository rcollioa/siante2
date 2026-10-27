import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AyudaApiService } from './ayuda-api.service';

describe('AyudaApiService', () => {
  let servicio: AyudaApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(AyudaApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('urlManual entrega la URL del archivo del manual de SIANTE', () => {
    let url: string | null | undefined;
    servicio.urlManual().subscribe((recibida) => (url = recibida));

    http.expectOne('/siante2/api/multimedia').flush({
      data: [
        { id: 2, descripcion: 'Gif Visualizar' },
        { id: 5, descripcion: 'Manual SIATE' }
      ]
    });

    expect(url).toBe('/siante2/api/multimedia/5/archivo');
  });

  it('urlManual entrega null si el manual no está en MULTIMEDIA', () => {
    let url: string | null | undefined;
    servicio.urlManual().subscribe((recibida) => (url = recibida));

    http.expectOne('/siante2/api/multimedia').flush({ data: [{ id: 2, descripcion: 'Gif Visualizar' }] });

    expect(url).toBeNull();
  });
});

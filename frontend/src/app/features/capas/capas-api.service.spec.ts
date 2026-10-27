import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CapasApiService } from './capas-api.service';

describe('CapasApiService', () => {
  let servicio: CapasApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(CapasApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista las capas del usuario sin guardarlas: cada vez pregunta, por si cambiaron en SIATE', () => {
    let recibidas: unknown;
    servicio.listarMias().subscribe((capas) => (recibidas = capas));
    const capa = { id: 1, nombre: 'Bodegas', descripcion: '', visibilidad: 'PRIVADA' };
    http.expectOne('/siante2/api/capas/mias').flush({ data: [capa] });
    expect(recibidas).toEqual([capa]);

    servicio.listarMias().subscribe();
    http.expectOne('/siante2/api/capas/mias').flush({ data: [] });
  });

  it('pide una capa por su ID', () => {
    servicio.ver(5).subscribe();
    http.expectOne('/siante2/api/capas/5').flush({});
  });
});

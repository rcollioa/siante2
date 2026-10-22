import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CategoriasApiService } from './categorias-api.service';

describe('CategoriasApiService', () => {
  let servicio: CategoriasApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(CategoriasApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide las categorías bajo el prefijo de la API y entrega el contenido de data', () => {
    let recibidas: unknown;
    servicio.listar().subscribe((categorias) => (recibidas = categorias));

    http.expectOne('/siante2/api/categorias').flush({ data: [{ id: 1, nombre: 'Salud', descripcion: '' }] });

    expect(recibidas).toEqual([{ id: 1, nombre: 'Salud', descripcion: '' }]);
  });

  it('la segunda consulta usa la caché y no vuelve a pedir al backend', () => {
    servicio.listar().subscribe();
    http.expectOne('/siante2/api/categorias').flush({ data: [] });

    servicio.listar().subscribe();
    http.expectNone('/siante2/api/categorias');
  });
});

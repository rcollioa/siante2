import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { reintentoInterceptor } from './reintento.interceptor';

describe('reintentoInterceptor', () => {
  const URL = '/siante2/api/direcciones';
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([reintentoInterceptor])), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
    vi.useRealTimers();
  });

  it('reintenta un 503 de la base y entrega la respuesta que llega después', () => {
    let respuesta: unknown;
    http.get(URL).subscribe((r) => (respuesta = r));

    backend.expectOne(URL).flush({ code: 'DATA_SOURCE_UNAVAILABLE' }, { status: 503, statusText: 'Service Unavailable' });
    vi.advanceTimersByTime(300);
    backend.expectOne(URL).flush({ data: [] });

    expect(respuesta).toEqual({ data: [] });
  });

  it('no reintenta cuando el servicio externo no respondió: el backend ya lo reintentó', () => {
    let fallo: unknown;
    http.get(URL).subscribe({ error: (e) => (fallo = e) });

    backend
      .expectOne(URL)
      .flush({ code: 'EXTERNAL_SERVICE_UNAVAILABLE' }, { status: 503, statusText: 'Service Unavailable' });
    vi.advanceTimersByTime(1000);

    backend.expectNone(URL);
    expect(fallo).toBeDefined();
  });
});

import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TimeoutError } from 'rxjs';
import { tiempoLimiteInterceptor } from './tiempo-limite.interceptor';

describe('tiempoLimiteInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  function pedir(url: string): { error: unknown } {
    const resultado: { error: unknown } = { error: null };
    http.post(url, null).subscribe({ error: (error: unknown) => (resultado.error = error) });
    return resultado;
  }

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([tiempoLimiteInterceptor])), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => vi.useRealTimers());

  it('una petición a la API que no responde se aborta a los 30 s', () => {
    const resultado = pedir('/siante2/api/capas');

    vi.advanceTimersByTime(29_999);
    expect(resultado.error).toBeNull();

    vi.advanceTimersByTime(1);
    expect(resultado.error).toBeInstanceOf(TimeoutError);
  });

  it('importar una capa tiene más plazo, porque inserta miles de filas', () => {
    const resultado = pedir('/siante2/api/capas/importar');

    vi.advanceTimersByTime(119_999);
    expect(resultado.error).toBeNull();

    vi.advanceTimersByTime(1);
    expect(resultado.error).toBeInstanceOf(TimeoutError);
  });

  it('fuera de la API no hay plazo', () => {
    const resultado = pedir('/assets/datos.json');

    vi.advanceTimersByTime(300_000);

    expect(resultado.error).toBeNull();
    backend.expectOne('/assets/datos.json').flush(null);
  });
});

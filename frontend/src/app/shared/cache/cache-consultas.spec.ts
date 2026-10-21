import { Observable, Subject, of } from 'rxjs';
import { CacheConsultas } from './cache-consultas';

describe('CacheConsultas', () => {
  it('una misma clave consulta una sola vez', () => {
    const cache = new CacheConsultas();
    const crear = vi.fn(() => of('dato'));

    cache.obtener('a', crear).subscribe();
    cache.obtener('a', crear).subscribe();

    expect(crear).toHaveBeenCalledTimes(1);
  });

  it('un error no se guarda: el siguiente intento vuelve a consultar', () => {
    const cache = new CacheConsultas();
    let peticiones = 0;
    // Cuenta suscripciones, no llamadas a `crear`: tras un error se reutiliza la misma consulta, pero
    // `shareReplay` se reinicia y vuelve a suscribirse a la fuente (en HTTP, una petición nueva).
    const fuente = new Observable<string>(suscriptor => {
      peticiones++;
      suscriptor.error(new Error('503'));
    });

    cache.obtener('a', () => fuente).subscribe({ error: () => undefined });
    cache.obtener('a', () => fuente).subscribe({ error: () => undefined });

    expect(peticiones).toBe(2);
  });

  it('dos suscriptores de una consulta en curso comparten la petición', () => {
    const cache = new CacheConsultas();
    const respuesta = new Subject<string>();
    const crear = vi.fn(() => respuesta.asObservable());
    const recibidos: string[] = [];

    cache.obtener('a', crear).subscribe(valor => recibidos.push(valor));
    cache.obtener('a', crear).subscribe(valor => recibidos.push(valor));
    respuesta.next('dato');

    expect(crear).toHaveBeenCalledTimes(1);
    expect(recibidos).toEqual(['dato', 'dato']);
  });

  it('al pasarse del máximo descarta la entrada menos usada', () => {
    const cache = new CacheConsultas({ maxEntradas: 2 });
    const crear = vi.fn(() => of('dato'));

    cache.obtener('a', crear).subscribe();
    cache.obtener('b', crear).subscribe();
    cache.obtener('a', crear).subscribe(); // 'a' pasa a ser la más reciente
    cache.obtener('c', crear).subscribe(); // descarta 'b'
    cache.obtener('a', crear).subscribe();
    cache.obtener('b', crear).subscribe();

    expect(crear).toHaveBeenCalledTimes(4);
  });

  it('una entrada vencida se vuelve a consultar', () => {
    vi.useFakeTimers();
    try {
      const cache = new CacheConsultas({ ttlMs: 1000 });
      const crear = vi.fn(() => of('dato'));

      cache.obtener('a', crear).subscribe();
      vi.advanceTimersByTime(1001);
      cache.obtener('a', crear).subscribe();

      expect(crear).toHaveBeenCalledTimes(2);
    } finally {
      vi.useRealTimers();
    }
  });
});

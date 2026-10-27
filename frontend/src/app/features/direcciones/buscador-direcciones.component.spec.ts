import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { MapaService, ZonaVisible } from '../mapa/mapa.service';
import { BuscadorDireccionesComponent, ESPERA_SUGERENCIAS_MS } from './buscador-direcciones.component';
import { Direccion, DireccionesApiService } from './direcciones-api.service';

describe('BuscadorDireccionesComponent', () => {
  const zona = { latitud: -33.44, longitud: -70.65, zoom: 12 };
  const mapa = { irA: vi.fn(), quitarMarcador: vi.fn(), zonaVisible: vi.fn(() => zona) };
  const teatinos = { nombre: 'Teatinos 56, Santiago', latitud: -33.44, longitud: -70.65 };
  const moneda = { nombre: 'Palacio de La Moneda, Santiago', latitud: -33.44, longitud: -70.654 };
  let buscar: ReturnType<typeof vi.fn<(texto: string) => Observable<Direccion[]>>>;
  let sugerir: ReturnType<typeof vi.fn<(texto: string, zona: ZonaVisible) => Observable<Direccion[]>>>;
  let fixture: ComponentFixture<BuscadorDireccionesComponent>;
  let elemento: HTMLElement;

  const campo = () => elemento.querySelector('input')!;
  const opciones = () => Array.from(elemento.querySelectorAll<HTMLElement>('li[role="option"]'));
  const listaVisible = () => !elemento.querySelector('ul')!.hidden;

  function crear(): void {
    TestBed.configureTestingModule({
      imports: [BuscadorDireccionesComponent],
      providers: [
        { provide: DireccionesApiService, useValue: { buscar, sugerir } },
        { provide: MapaService, useValue: mapa }
      ]
    });
    fixture = TestBed.createComponent(BuscadorDireccionesComponent);
    fixture.detectChanges();
    elemento = fixture.nativeElement;
  }

  function escribir(texto: string): void {
    campo().value = texto;
    campo().dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function esperarPausa(): void {
    vi.advanceTimersByTime(ESPERA_SUGERENCIAS_MS);
    fixture.detectChanges();
  }

  function tecla(key: string): void {
    campo().dispatchEvent(new KeyboardEvent('keydown', { key }));
    fixture.detectChanges();
  }

  function enviar(texto?: string): void {
    if (texto !== undefined) {
      escribir(texto);
    }
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    buscar = vi.fn();
    sugerir = vi.fn(() => of([teatinos, moneda]));
  });

  afterEach(() => vi.useRealTimers());

  describe('mientras se escribe', () => {
    it('sugiere después de una pausa, no en cada tecla, primero lo cercano a la zona del mapa', () => {
      crear();
      escribir('tea');
      escribir('teat');
      expect(sugerir).not.toHaveBeenCalled();

      esperarPausa();

      expect(sugerir).toHaveBeenCalledTimes(1);
      expect(sugerir).toHaveBeenCalledWith('teat', zona);
      expect(opciones().map((o) => o.textContent?.trim())).toEqual([teatinos.nombre, moneda.nombre]);
      expect(campo().getAttribute('aria-expanded')).toBe('true');
    });

    it('no sugiere con menos de 3 caracteres y cierra la lista', () => {
      crear();
      escribir('teat');
      esperarPausa();

      escribir('te');
      esperarPausa();

      expect(sugerir).toHaveBeenCalledTimes(1);
      expect(listaVisible()).toBe(false);
      expect(campo().getAttribute('aria-expanded')).toBe('false');
    });

    it('con las flechas resalta una sugerencia y Enter la elige y centra el mapa', () => {
      crear();
      escribir('teat');
      esperarPausa();

      tecla('ArrowDown');
      tecla('ArrowDown');
      expect(campo().getAttribute('aria-activedescendant')).toBe('direccion-opcion-1');
      expect(opciones()[1].getAttribute('aria-selected')).toBe('true');
      tecla('ArrowDown');
      expect(campo().getAttribute('aria-activedescendant')).toBe('direccion-opcion-0');

      enviar();

      expect(buscar).not.toHaveBeenCalled();
      expect(mapa.irA).toHaveBeenCalledWith(teatinos.longitud, teatinos.latitud);
      expect(campo().value).toBe(teatinos.nombre);
      expect(listaVisible()).toBe(false);
    });

    it('con el mouse elige una sugerencia', () => {
      crear();
      escribir('pala');
      esperarPausa();

      opciones()[1].dispatchEvent(new MouseEvent('mousedown', { cancelable: true }));
      fixture.detectChanges();

      expect(mapa.irA).toHaveBeenCalledWith(moneda.longitud, moneda.latitud);
    });

    it('si las sugerencias fallan, invita a buscar con Enter', () => {
      sugerir.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 503 })));
      crear();
      escribir('teat');
      esperarPausa();

      expect(elemento.textContent).toContain('Las sugerencias no están disponibles: presione Enter para buscar.');
    });

    it('una sugerencia pendiente no reemplaza la búsqueda completa', () => {
      buscar.mockReturnValue(of([teatinos]));
      crear();
      escribir('teatinos 56');
      enviar();
      esperarPausa();

      expect(sugerir).not.toHaveBeenCalled();
      expect(opciones()).toHaveLength(1);
    });

    it('al salir del campo se cierran las sugerencias', () => {
      crear();
      escribir('teat');
      esperarPausa();

      campo().dispatchEvent(new Event('blur'));
      fixture.detectChanges();

      expect(listaVisible()).toBe(false);
    });
  });

  describe('al confirmar sin elegir una sugerencia', () => {
    it('busca completo y al elegir un resultado centra el mapa en él', () => {
      buscar.mockReturnValue(of([teatinos]));
      crear();

      enviar('  teatinos 56 ');
      expect(buscar).toHaveBeenCalledWith('teatinos 56');

      opciones()[0].dispatchEvent(new MouseEvent('mousedown', { cancelable: true }));
      fixture.detectChanges();

      expect(mapa.irA).toHaveBeenCalledWith(-70.65, -33.44);
      expect(opciones()).toHaveLength(0);
      expect(campo().value).toBe('Teatinos 56, Santiago');
    });

    it('no consulta textos de menos de 3 caracteres', () => {
      crear();
      enviar('ab');

      expect(buscar).not.toHaveBeenCalled();
      expect(elemento.textContent).toContain('Escriba al menos 3 caracteres.');
    });

    it('avisa si no hay resultados o si el servicio no está disponible', () => {
      buscar.mockReturnValueOnce(of([])).mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 503 })));
      crear();

      enviar('xyzxyz');
      expect(elemento.textContent).toContain('No se encontraron direcciones.');
      enviar('santiago');
      expect(elemento.textContent).toContain('La búsqueda de direcciones no está disponible.');
    });
  });

  it('Escape cierra la lista y un segundo Escape limpia la búsqueda y quita el marcador', () => {
    crear();
    escribir('valparaiso');
    esperarPausa();

    tecla('Escape');
    expect(listaVisible()).toBe(false);
    expect(campo().value).toBe('valparaiso');

    tecla('Escape');
    expect(campo().value).toBe('');
    expect(mapa.quitarMarcador).toHaveBeenCalled();
  });
});

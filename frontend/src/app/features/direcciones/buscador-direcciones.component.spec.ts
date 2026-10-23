import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { MapaService } from '../mapa/mapa.service';
import { BuscadorDireccionesComponent } from './buscador-direcciones.component';
import { Direccion, DireccionesApiService } from './direcciones-api.service';

describe('BuscadorDireccionesComponent', () => {
  const mapa = { irA: vi.fn(), quitarMarcador: vi.fn() };
  let buscar: ReturnType<typeof vi.fn<(texto: string) => Observable<Direccion[]>>>;

  function crear(): ComponentFixture<BuscadorDireccionesComponent> {
    TestBed.configureTestingModule({
      imports: [BuscadorDireccionesComponent],
      providers: [
        { provide: DireccionesApiService, useValue: { buscar } },
        { provide: MapaService, useValue: mapa }
      ]
    });
    const fixture = TestBed.createComponent(BuscadorDireccionesComponent);
    fixture.detectChanges();
    return fixture;
  }

  function enviar(fixture: ComponentFixture<BuscadorDireccionesComponent>, texto: string): HTMLElement {
    const elemento: HTMLElement = fixture.nativeElement;
    const campo = elemento.querySelector('input')!;
    campo.value = texto;
    campo.dispatchEvent(new Event('input'));
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    return elemento;
  }

  beforeEach(() => {
    vi.clearAllMocks();
    buscar = vi.fn();
  });

  it('busca al enviar y al elegir un resultado centra el mapa en él', () => {
    buscar.mockReturnValue(of([{ nombre: 'Teatinos 56, Santiago', latitud: -33.44, longitud: -70.65 }]));
    const fixture = crear();

    const elemento = enviar(fixture, '  teatinos 56 ');
    expect(buscar).toHaveBeenCalledWith('teatinos 56');

    elemento.querySelector<HTMLButtonElement>('li button')!.click();
    fixture.detectChanges();

    expect(mapa.irA).toHaveBeenCalledWith(-70.65, -33.44);
    expect(elemento.querySelector('li')).toBeNull();
    expect(elemento.querySelector('input')!.value).toBe('Teatinos 56, Santiago');
  });

  it('no consulta textos de menos de 3 caracteres', () => {
    const elemento = enviar(crear(), 'ab');

    expect(buscar).not.toHaveBeenCalled();
    expect(elemento.textContent).toContain('Escriba al menos 3 caracteres.');
  });

  it('avisa si no hay resultados o si el servicio no está disponible', () => {
    buscar.mockReturnValueOnce(of([])).mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 503 })));
    const fixture = crear();

    expect(enviar(fixture, 'xyzxyz').textContent).toContain('No se encontraron direcciones.');
    expect(enviar(fixture, 'santiago').textContent).toContain('La búsqueda de direcciones no está disponible.');
  });

  it('Escape limpia la búsqueda y quita el marcador', () => {
    buscar.mockReturnValue(of([{ nombre: 'Valparaíso', latitud: -33.04, longitud: -71.62 }]));
    const fixture = crear();
    const elemento = enviar(fixture, 'valparaiso');

    elemento.querySelector('input')!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(elemento.querySelector('input')!.value).toBe('');
    expect(elemento.querySelector('li')).toBeNull();
    expect(mapa.quitarMarcador).toHaveBeenCalled();
  });
});

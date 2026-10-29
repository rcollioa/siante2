import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { MenuLateralService } from '../../../../../shared/menu-lateral/menu-lateral.service';
import { CapasAbiertas } from '../../../application/capas-abiertas';
import { CapasRepositorio } from '../../../application/port/out/capas.repositorio';
import { CapaCatalogo, SeccionCatalogo } from '../../../domain/capa';
import { CatalogoCapasComponent, MAXIMO_FILAS } from './catalogo-capas.component';

describe('CatalogoCapasComponent', () => {
  const capa = (id: number, nombre: string, cambios: Partial<CapaCatalogo> = {}): CapaCatalogo => ({
    id,
    nombre,
    descripcion: '',
    visibilidad: 'PUBLICA',
    categoria: 'Agua',
    dueno: 'Ana Pérez',
    favorita: false,
    ...cambios
  });
  let catalogo: ReturnType<typeof vi.fn<(seccion: SeccionCatalogo) => Observable<CapaCatalogo[]>>>;
  const capasAbiertas = { abrir: vi.fn(), abiertas: signal([]) };
  const menu = { alElegir: vi.fn() };
  let fixture: ComponentFixture<CatalogoCapasComponent>;
  let elemento: HTMLElement;

  const filas = () => Array.from(elemento.querySelectorAll('tbody tr'));
  const pestanas = () => Array.from(elemento.querySelectorAll<HTMLButtonElement>('[role="tab"]'));
  const texto = () => elemento.textContent?.replace(/\s+/g, ' ') ?? '';

  // jsdom no siempre implementa el diálogo modal: basta con marcarlo abierto o cerrado.
  beforeAll(() => {
    if (!HTMLDialogElement.prototype.showModal) {
      HTMLDialogElement.prototype.showModal = function (this: HTMLDialogElement) {
        this.open = true;
      };
      HTMLDialogElement.prototype.close = function (this: HTMLDialogElement) {
        this.open = false;
      };
    }
  });

  beforeEach(() => {
    vi.clearAllMocks();
    catalogo = vi.fn(() =>
      of([capa(1, 'Ríos', { favorita: true, descripcion: 'Red hídrica' }), capa(2, 'Hospitales', { categoria: 'Salud' })])
    );
    TestBed.configureTestingModule({
      imports: [CatalogoCapasComponent],
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        // Catalogo real con un doble del repositorio: la pantalla y su caso de uso juntos, sin HTTP.
        { provide: CapasRepositorio, useValue: { catalogo } },
        { provide: CapasAbiertas, useValue: capasAbiertas },
        { provide: MenuLateralService, useValue: menu }
      ]
    });
    fixture = TestBed.createComponent(CatalogoCapasComponent);
    fixture.detectChanges();
    elemento = fixture.nativeElement;
  });

  function abrir(seccion: SeccionCatalogo): void {
    fixture.componentInstance.abrir(seccion);
    fixture.detectChanges();
  }

  it('abre en la pestaña pedida con sus capas, categoría, dueño y si es favorita', () => {
    abrir('FORMALES');

    expect(elemento.querySelector('dialog')?.open).toBe(true);
    expect(catalogo).toHaveBeenCalledWith('FORMALES');
    expect(pestanas().map((p) => p.getAttribute('aria-selected'))).toEqual(['false', 'false', 'true']);
    expect(texto()).toContain('2 capas');
    expect(filas()[0].textContent).toContain('Ríos');
    expect(filas()[0].textContent).toContain('Red hídrica');
    expect(filas()[0].textContent).toContain('Sí');
    expect(filas()[1].textContent).toContain('Salud');
  });

  it('el buscador filtra sin importar tildes y dice cuántas coinciden', () => {
    abrir('PUBLICAS');
    const buscador = elemento.querySelector<HTMLInputElement>('input[type="search"]')!;

    buscador.value = 'rios';
    buscador.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(filas()).toHaveLength(1);
    expect(texto()).toContain('1 de 2 capas');
  });

  it('"Ver en el mapa" cierra el catálogo, vuelve al mapa y abre la capa', async () => {
    abrir('PUBLICAS');
    await TestBed.inject(Router).navigateByUrl('/ayuda');

    elemento.querySelector<HTMLButtonElement>('button[aria-label="Ver Ríos en el mapa"]')!.click();
    await fixture.whenStable();

    expect(elemento.querySelector('dialog')?.open).toBe(false);
    expect(TestBed.inject(Router).url).toBe('/mapa');
    expect(capasAbiertas.abrir).toHaveBeenCalledWith(1);
    expect(menu.alElegir).toHaveBeenCalled();
  });

  it('las flechas cambian de pestaña y la descargan', () => {
    abrir('PUBLICAS');

    pestanas()[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
    fixture.detectChanges();

    expect(catalogo).toHaveBeenLastCalledWith('FORMALES');
    expect(pestanas()[2].getAttribute('aria-selected')).toBe('true');
    expect(pestanas()[2].tabIndex).toBe(0);
    expect(pestanas()[0].tabIndex).toBe(-1);
  });

  it('con muchas capas dibuja las primeras e invita a buscar', () => {
    catalogo.mockReturnValue(of(Array.from({ length: MAXIMO_FILAS + 5 }, (_, i) => capa(i, `Capa ${i}`))));

    abrir('PUBLICAS');

    expect(filas()).toHaveLength(MAXIMO_FILAS);
    expect(texto()).toContain(`Se muestran las primeras ${MAXIMO_FILAS}.`);
  });

  it('una pestaña vacía lo dice', () => {
    catalogo.mockReturnValue(of([]));

    abrir('MIAS');

    expect(texto()).toContain('Esta pestaña no tiene capas.');
  });
});

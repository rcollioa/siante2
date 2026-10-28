import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { MenuLateralService } from '../../../../../shared/menu-lateral/menu-lateral.service';
import { CapaEnMapa, CapasAbiertas } from '../../../application/capas-abiertas';
import { CapasRepositorio } from '../../../application/port/out/capas.repositorio';
import { Capa, ResumenCapa } from '../../../domain/capa';
import { MisCapasComponent } from './mis-capas.component';

describe('MisCapasComponent', () => {
  const bodegas: ResumenCapa = { id: 1, nombre: 'Bodegas', descripcion: 'Región', visibilidad: 'PRIVADA' };
  const ide: ResumenCapa = { id: 2, nombre: 'Servicio IDE', descripcion: '', visibilidad: 'PUBLICA' };
  const enMapa = (resumen: ResumenCapa, visible = true): CapaEnMapa => ({
    capa: { ...resumen, propia: true, tipo: 'VECTORIAL', elementos: [] } as Capa,
    visible
  });
  let listarMias: ReturnType<typeof vi.fn<() => Observable<ResumenCapa[]>>>;
  const capasAbiertas = {
    abiertas: signal<CapaEnMapa[]>([]),
    abriendo: signal<ReadonlySet<number>>(new Set()),
    abrir: vi.fn(),
    cerrar: vi.fn(),
    alternarVisibilidad: vi.fn(),
    acercar: vi.fn()
  };
  const menu = { alElegir: vi.fn() };
  let fixture: ComponentFixture<MisCapasComponent>;
  let elemento: HTMLElement;

  const botones = () => Array.from(elemento.querySelectorAll<HTMLButtonElement>('button.capa'));
  const acciones = () => Array.from(elemento.querySelectorAll<HTMLButtonElement>('button.accion'));

  function crear(): void {
    TestBed.configureTestingModule({
      imports: [MisCapasComponent],
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        // MisCapas real con un doble del repositorio: la pantalla y su caso de uso juntos, sin HTTP.
        { provide: CapasRepositorio, useValue: { listarMias } },
        { provide: CapasAbiertas, useValue: capasAbiertas },
        { provide: MenuLateralService, useValue: menu }
      ]
    });
    fixture = TestBed.createComponent(MisCapasComponent);
    fixture.detectChanges();
    elemento = fixture.nativeElement;
  }

  beforeEach(() => {
    vi.clearAllMocks();
    capasAbiertas.abiertas.set([]);
    capasAbiertas.abriendo.set(new Set());
    listarMias = vi.fn(() => of([bodegas, ide]));
  });

  it('lista las capas con su visibilidad, sin acciones mientras están cerradas', () => {
    crear();

    const fila = (b: HTMLButtonElement) => [b.querySelector('.nombre')?.textContent, b.querySelector('.visibilidad')?.textContent];
    expect(botones().map(fila)).toEqual([
      ['Bodegas', 'Privada'],
      ['Servicio IDE', 'Pública']
    ]);
    expect(botones()[0].title).toBe('Región');
    expect(acciones()).toHaveLength(0);
  });

  it('al elegir una capa vuelve al mapa, la abre y cierra el menú en pantallas chicas', async () => {
    crear();
    await TestBed.inject(Router).navigateByUrl('/ayuda');

    botones()[0].click();
    await fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/mapa');
    expect(capasAbiertas.abrir).toHaveBeenCalledWith(1);
    expect(menu.alElegir).toHaveBeenCalled();
  });

  it('marca las capas abiertas, que pueden ser varias, y elegir una abierta la cierra', () => {
    crear();
    capasAbiertas.abiertas.set([enMapa(bodegas), enMapa(ide)]);
    fixture.detectChanges();

    expect(botones().map((b) => b.getAttribute('aria-pressed'))).toEqual(['true', 'true']);
    botones()[1].click();

    expect(capasAbiertas.cerrar).toHaveBeenCalledWith(2);
    expect(capasAbiertas.abrir).not.toHaveBeenCalled();
  });

  it('una capa abierta se puede ocultar y acercar', () => {
    crear();
    capasAbiertas.abiertas.set([enMapa(bodegas, false)]);
    fixture.detectChanges();

    const [mostrar, acercar] = acciones();
    expect(mostrar.getAttribute('aria-label')).toBe('Mostrar Bodegas en el mapa');
    expect(mostrar.getAttribute('aria-pressed')).toBe('false');
    expect(acercar.getAttribute('aria-label')).toBe('Acercar a Bodegas');

    mostrar.click();
    acercar.click();

    expect(capasAbiertas.alternarVisibilidad).toHaveBeenCalledWith(1);
    expect(capasAbiertas.acercar).toHaveBeenCalledWith(1);
  });

  it('marca la capa que se está descargando', () => {
    crear();
    capasAbiertas.abriendo.set(new Set([2]));
    fixture.detectChanges();

    expect(botones().map((b) => b.getAttribute('aria-busy'))).toEqual(['false', 'true']);
  });

  it('sin capas lo dice', () => {
    listarMias.mockReturnValue(of([]));
    crear();

    expect(elemento.textContent).toContain('Aún no tiene capas.');
  });

  it('si no se pudieron cargar permite reintentar', () => {
    listarMias.mockReturnValueOnce(throwError(() => new Error('sin conexión')));
    crear();
    expect(elemento.textContent).toContain('No se pudieron cargar sus capas.');

    elemento.querySelector<HTMLButtonElement>('button.reintentar')!.click();
    fixture.detectChanges();

    expect(botones()).toHaveLength(2);
  });
});

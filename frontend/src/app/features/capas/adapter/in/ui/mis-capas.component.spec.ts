import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { MenuLateralService } from '../../../../../shared/menu-lateral/menu-lateral.service';
import { CapaAbierta } from '../../../application/capa-abierta';
import { CapasRepositorio } from '../../../application/port/out/capas.repositorio';
import { ResumenCapa } from '../../../domain/capa';
import { MisCapasComponent } from './mis-capas.component';

describe('MisCapasComponent', () => {
  const bodegas: ResumenCapa = { id: 1, nombre: 'Bodegas', descripcion: 'Región', visibilidad: 'PRIVADA' };
  const ide: ResumenCapa = { id: 2, nombre: 'Servicio IDE', descripcion: '', visibilidad: 'PUBLICA' };
  let listarMias: ReturnType<typeof vi.fn<() => Observable<ResumenCapa[]>>>;
  const capaAbierta = {
    capa: signal<{ id: number } | null>(null),
    abriendo: signal<number | null>(null),
    abrir: vi.fn(),
    cerrar: vi.fn()
  };
  const menu = { alElegir: vi.fn() };
  let fixture: ComponentFixture<MisCapasComponent>;
  let elemento: HTMLElement;

  const botones = () => Array.from(elemento.querySelectorAll<HTMLButtonElement>('button.capa'));

  function crear(): void {
    TestBed.configureTestingModule({
      imports: [MisCapasComponent],
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        // MisCapas real con un doble del repositorio: la pantalla y su caso de uso juntos, sin HTTP.
        { provide: CapasRepositorio, useValue: { listarMias } },
        { provide: CapaAbierta, useValue: capaAbierta },
        { provide: MenuLateralService, useValue: menu }
      ]
    });
    fixture = TestBed.createComponent(MisCapasComponent);
    fixture.detectChanges();
    elemento = fixture.nativeElement;
  }

  beforeEach(() => {
    vi.clearAllMocks();
    capaAbierta.capa.set(null);
    listarMias = vi.fn(() => of([bodegas, ide]));
  });

  it('lista las capas con su visibilidad', () => {
    crear();

    const fila = (b: HTMLButtonElement) => [b.querySelector('.nombre')?.textContent, b.querySelector('.visibilidad')?.textContent];
    expect(botones().map(fila)).toEqual([
      ['Bodegas', 'Privada'],
      ['Servicio IDE', 'Pública']
    ]);
    expect(botones()[0].title).toBe('Región');
  });

  it('al elegir una capa vuelve al mapa, la abre y cierra el menú en pantallas chicas', async () => {
    crear();
    await TestBed.inject(Router).navigateByUrl('/ayuda');

    botones()[0].click();
    await fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/mapa');
    expect(capaAbierta.abrir).toHaveBeenCalledWith(1);
    expect(menu.alElegir).toHaveBeenCalled();
  });

  it('marca la capa abierta y elegirla de nuevo la cierra', () => {
    crear();
    capaAbierta.capa.set({ id: 2 });
    fixture.detectChanges();

    expect(botones().map((b) => b.getAttribute('aria-pressed'))).toEqual(['false', 'true']);
    botones()[1].click();

    expect(capaAbierta.cerrar).toHaveBeenCalled();
    expect(capaAbierta.abrir).not.toHaveBeenCalled();
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

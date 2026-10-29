import { signal, Type } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of } from 'rxjs';
import { MenuLateralService } from '../../../../../shared/menu-lateral/menu-lateral.service';
import { CapasAbiertas } from '../../../application/capas-abiertas';
import { CapasRepositorio } from '../../../application/port/out/capas.repositorio';
import { CapaCatalogo, ResumenCapa, SeccionCatalogo } from '../../../domain/capa';
import { CapasFormalesComponent, FavoritasComponent } from './favoritas.component';

describe('FavoritasComponent', () => {
  const rios: ResumenCapa = { id: 3, nombre: 'Ríos', descripcion: '', visibilidad: 'PUBLICA' };
  const hospitales: ResumenCapa = { id: 5, nombre: 'Hospitales', descripcion: '', visibilidad: 'FORMAL' };
  let listarFavoritas: ReturnType<typeof vi.fn<() => Observable<ResumenCapa[]>>>;
  let catalogo: ReturnType<typeof vi.fn<(seccion: SeccionCatalogo) => Observable<CapaCatalogo[]>>>;

  beforeAll(() => {
    if (!HTMLDialogElement.prototype.showModal) {
      HTMLDialogElement.prototype.showModal = function (this: HTMLDialogElement) {
        this.open = true;
      };
    }
  });

  function crear<T>(componente: Type<T>): ComponentFixture<T> {
    listarFavoritas = vi.fn(() => of([rios, hospitales]));
    catalogo = vi.fn(() => of([]));
    TestBed.configureTestingModule({
      imports: [componente],
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        { provide: CapasRepositorio, useValue: { listarFavoritas, catalogo } },
        { provide: CapasAbiertas, useValue: { abiertas: signal([]), abriendo: signal(new Set()) } },
        { provide: MenuLateralService, useValue: { alElegir: vi.fn() } }
      ]
    });
    const fixture = TestBed.createComponent(componente);
    fixture.detectChanges();
    return fixture;
  }

  const nombres = (elemento: HTMLElement) =>
    Array.from(elemento.querySelectorAll('button.capa .nombre')).map((nombre) => nombre.textContent);

  it('"Favoritos" lista las favoritas que no son formales y abre el catálogo en "Todas"', () => {
    const fixture = crear(FavoritasComponent);
    const elemento: HTMLElement = fixture.nativeElement;

    expect(nombres(elemento)).toEqual(['Ríos']);
    elemento.querySelector<HTMLButtonElement>('button.abrir-catalogo')!.click();
    expect(catalogo).toHaveBeenCalledWith('PUBLICAS');
  });

  it('"Capas formales" lista las formales elegidas y abre el catálogo en su pestaña', () => {
    const fixture = crear(CapasFormalesComponent);
    const elemento: HTMLElement = fixture.nativeElement;

    expect(nombres(elemento)).toEqual(['Hospitales']);
    expect(elemento.querySelector('button.abrir-catalogo')?.textContent).toContain('capas formales');
    elemento.querySelector<HTMLButtonElement>('button.abrir-catalogo')!.click();
    expect(catalogo).toHaveBeenCalledWith('FORMALES');
  });

  it('sin favoritas lo dice', () => {
    const fixture = crear(FavoritasComponent);
    listarFavoritas.mockReturnValue(of([hospitales]));
    fixture.componentInstance['favoritas'].cargar();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aún no tiene capas favoritas.');
  });
});

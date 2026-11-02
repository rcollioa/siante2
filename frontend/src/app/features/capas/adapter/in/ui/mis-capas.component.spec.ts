import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { MenuLateralService } from '../../../../../shared/menu-lateral/menu-lateral.service';
import { CategoriasApiService } from '../../../../categorias/categorias-api.service';
import { CapaEnMapa, CapasAbiertas } from '../../../application/capas-abiertas';
import { CapasRepositorio } from '../../../application/port/out/capas.repositorio';
import { Capa, CapaPropia, NuevaCapa, ResumenCapa } from '../../../domain/capa';
import { MisCapasComponent } from './mis-capas.component';

describe('MisCapasComponent', () => {
  const bodegas: CapaPropia = { id: 1, nombre: 'Bodegas', descripcion: 'Región', visibilidad: 'PRIVADA', categoriaId: 3 };
  const ide: CapaPropia = { id: 2, nombre: 'Servicio IDE', descripcion: '', visibilidad: 'PUBLICA', categoriaId: 3 };
  const enMapa = (resumen: ResumenCapa, visible = true): CapaEnMapa => ({
    capa: { ...resumen, propia: true, tipo: 'VECTORIAL', elementos: [], servicio: null } as Capa,
    visible
  });
  let listarMias: ReturnType<typeof vi.fn<() => Observable<CapaPropia[]>>>;
  const crearCapa = vi.fn<(nueva: NuevaCapa) => Observable<CapaPropia>>();
  const cambiarVisibilidad = vi.fn<(id: number, visibilidad: string) => Observable<void>>();
  const eliminar = vi.fn<(id: number) => Observable<void>>();
  const capasAbiertas = {
    abiertas: signal<CapaEnMapa[]>([]),
    abriendo: signal<ReadonlySet<number>>(new Set()),
    abrir: vi.fn(),
    cerrar: vi.fn(),
    actualizar: vi.fn(),
    alternarVisibilidad: vi.fn(),
    acercar: vi.fn()
  };
  const menu = { alElegir: vi.fn() };
  let fixture: ComponentFixture<MisCapasComponent>;
  let elemento: HTMLElement;

  const botones = () => Array.from(elemento.querySelectorAll<HTMLButtonElement>('button.capa'));
  const acciones = () => Array.from(elemento.querySelectorAll<HTMLButtonElement>('button.accion'));
  const gestionar = () => Array.from(elemento.querySelectorAll<HTMLButtonElement>('button.gestionar'));
  const opciones = () => Array.from(elemento.querySelectorAll<HTMLButtonElement>('.opciones button'));
  const opcion = (texto: string) => opciones().find((boton) => boton.textContent?.trim() === texto) as HTMLButtonElement;
  const confirmacion = () => elemento.querySelector('app-confirmacion dialog') as HTMLDialogElement;

  function crear(): void {
    TestBed.configureTestingModule({
      imports: [MisCapasComponent],
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        // MisCapas real con un doble del repositorio: la pantalla y su caso de uso juntos, sin HTTP.
        { provide: CapasRepositorio, useValue: { listarMias, crear: crearCapa, cambiarVisibilidad, eliminar } },
        { provide: CapasAbiertas, useValue: capasAbiertas },
        { provide: MenuLateralService, useValue: menu },
        { provide: CategoriasApiService, useValue: { listar: () => of([{ id: 3, nombre: 'Agua', descripcion: '' }]) } }
      ]
    });
    fixture = TestBed.createComponent(MisCapasComponent);
    fixture.detectChanges();
    elemento = fixture.nativeElement;
  }

  function desplegarOpciones(indice = 0): void {
    gestionar()[indice].click();
    fixture.detectChanges();
  }

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
    capasAbiertas.abiertas.set([]);
    capasAbiertas.abriendo.set(new Set());
    listarMias = vi.fn(() => of([bodegas, ide]));
    cambiarVisibilidad.mockReturnValue(of(undefined));
    eliminar.mockReturnValue(of(undefined));
  });

  describe('crear una capa', () => {
    const nueva: CapaPropia = { id: 50, nombre: 'Aduanas', descripcion: '', visibilidad: 'PRIVADA', categoriaId: 0 };

    function crearDesdeElDialogo(): void {
      (elemento.querySelector('button.crear') as HTMLButtonElement).click();
      fixture.detectChanges();
      const nombre = elemento.querySelector('#nombre-nueva-capa') as HTMLInputElement;
      nombre.value = 'Aduanas';
      nombre.dispatchEvent(new Event('input'));
      (elemento.querySelector('app-crear-capa form') as HTMLFormElement).dispatchEvent(new Event('submit', { cancelable: true }));
      fixture.detectChanges();
    }

    it('con el botón abre el diálogo "Nueva capa"', () => {
      crear();

      (elemento.querySelector('button.crear') as HTMLButtonElement).click();
      fixture.detectChanges();

      expect((elemento.querySelector('app-crear-capa dialog') as HTMLDialogElement).open).toBe(true);
    });

    it('también se puede crear la primera capa, con la lista vacía', () => {
      listarMias = vi.fn(() => of([]));
      crear();

      expect(elemento.textContent).toContain('Aún no tiene capas.');
      expect(elemento.querySelector('button.crear')).not.toBeNull();
    });

    it('la capa creada entra a la lista en orden y se abre en el mapa, como en SIATE', async () => {
      crearCapa.mockReturnValue(of(nueva));
      crear();
      await TestBed.inject(Router).navigateByUrl('/ayuda');

      crearDesdeElDialogo();
      await fixture.whenStable();

      expect(crearCapa).toHaveBeenCalledWith({ nombre: 'Aduanas', descripcion: '' });
      expect(botones().map((boton) => boton.querySelector('.nombre')?.textContent)).toEqual(['Aduanas', 'Bodegas', 'Servicio IDE']);
      expect(capasAbiertas.abrir).toHaveBeenCalledWith(50);
      expect(TestBed.inject(Router).url).toBe('/mapa');
      expect(menu.alElegir).toHaveBeenCalled();
    });

    it('si falla no la agrega ni abre nada', () => {
      crearCapa.mockReturnValue(throwError(() => new Error('sin conexión')));
      crear();

      crearDesdeElDialogo();

      expect(botones()).toHaveLength(2);
      expect(capasAbiertas.abrir).not.toHaveBeenCalled();
    });
  });

  describe('opciones de una capa propia', () => {
    it('se despliegan bajo la capa, con el botón que indica si están abiertas', () => {
      crear();
      expect(opciones()).toHaveLength(0);
      expect(gestionar()[0].getAttribute('aria-expanded')).toBe('false');
      expect(gestionar()[0].getAttribute('aria-label')).toBe('Opciones de Bodegas');

      desplegarOpciones();

      expect(gestionar()[0].getAttribute('aria-expanded')).toBe('true');
      expect(gestionar()[0].getAttribute('aria-controls')).toBe('opciones-capa-1');
      expect(opciones().map((boton) => boton.textContent?.trim())).toEqual(['Editar', 'Publicar', 'Eliminar']);

      desplegarOpciones();
      expect(opciones()).toHaveLength(0);
    });

    it('solo una capa a la vez, y una pública ofrece despublicar', () => {
      crear();
      desplegarOpciones(0);

      desplegarOpciones(1);

      expect(opciones().map((boton) => boton.textContent?.trim())).toEqual(['Editar', 'Despublicar', 'Eliminar']);
    });

    it('una capa formal no las ofrece', () => {
      listarMias = vi.fn(() => of([{ ...bodegas, visibilidad: 'FORMAL' as const }]));
      crear();

      expect(botones()).toHaveLength(1);
      expect(gestionar()).toHaveLength(0);
    });

    it('Editar abre el formulario con los datos de la capa', () => {
      crear();
      desplegarOpciones();

      opcion('Editar').click();
      fixture.detectChanges();

      expect((elemento.querySelector('app-editar-capa dialog') as HTMLDialogElement).open).toBe(true);
      expect((elemento.querySelector('#nombre-capa') as HTMLInputElement).value).toBe('Bodegas');
    });

    it('Publicar pregunta primero y, al responder Sí, publica y actualiza la fila', () => {
      crear();
      desplegarOpciones();

      opcion('Publicar').click();
      fixture.detectChanges();
      expect(confirmacion().open).toBe(true);
      expect(confirmacion().textContent).toContain('¿Desea publicar la capa?');
      expect(confirmacion().textContent).toContain('Bodegas');
      expect(cambiarVisibilidad).not.toHaveBeenCalled();

      (confirmacion().querySelector('button.si') as HTMLButtonElement).click();
      fixture.detectChanges();

      expect(cambiarVisibilidad).toHaveBeenCalledWith(1, 'PUBLICA');
      expect(confirmacion().open).toBe(false);
      expect(botones()[0].querySelector('.visibilidad')?.textContent).toBe('Pública');
      expect(opcion('Despublicar')).toBeDefined();
    });

    it('Despublicar deja la capa privada', () => {
      crear();
      desplegarOpciones(1);

      opcion('Despublicar').click();
      fixture.detectChanges();
      expect(confirmacion().textContent).toContain('¿Desea despublicar la capa?');

      (confirmacion().querySelector('button.si') as HTMLButtonElement).click();
      expect(cambiarVisibilidad).toHaveBeenCalledWith(2, 'PRIVADA');
    });

    it('Eliminar pregunta primero; con No no pasa nada', () => {
      crear();
      desplegarOpciones();

      opcion('Eliminar').click();
      fixture.detectChanges();
      expect(confirmacion().textContent).toContain('¿Confirma el borrado de la capa?');

      (confirmacion().querySelector('button.no') as HTMLButtonElement).click();
      fixture.detectChanges();

      expect(eliminar).not.toHaveBeenCalled();
      expect(botones()).toHaveLength(2);
    });

    it('Eliminar con Sí saca la capa de la lista y la cierra en el mapa', () => {
      crear();
      desplegarOpciones();

      opcion('Eliminar').click();
      fixture.detectChanges();
      (confirmacion().querySelector('button.si') as HTMLButtonElement).click();
      fixture.detectChanges();

      expect(eliminar).toHaveBeenCalledWith(1);
      expect(capasAbiertas.cerrar).toHaveBeenCalledWith(1);
      expect(botones().map((boton) => boton.querySelector('.nombre')?.textContent)).toEqual(['Servicio IDE']);
      expect(opciones()).toHaveLength(0);
    });

    it('si falla lo explica en la confirmación y la capa sigue en la lista', () => {
      eliminar.mockReturnValue(throwError(() => new Error('sin conexión')));
      crear();
      desplegarOpciones();

      opcion('Eliminar').click();
      fixture.detectChanges();
      (confirmacion().querySelector('button.si') as HTMLButtonElement).click();
      fixture.detectChanges();

      expect(confirmacion().open).toBe(true);
      expect(confirmacion().querySelector('[role="alert"]')?.textContent).toBe('No se pudo guardar el cambio. Intente nuevamente.');
      expect(botones()).toHaveLength(2);
    });
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

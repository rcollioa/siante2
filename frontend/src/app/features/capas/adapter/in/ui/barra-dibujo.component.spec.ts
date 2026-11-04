import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HerramientaDibujo } from '../../../../mapa/domain/mapa';
import { EdicionElementos, SalidaPedida } from '../../../application/edicion-elementos';
import { Capa, ElementoCapa } from '../../../domain/capa';
import { BarraDibujoComponent } from './barra-dibujo.component';

describe('BarraDibujoComponent', () => {
  const capa = { id: 5, nombre: 'Bodegas' } as Capa;
  const edicion = {
    capa: signal<Capa | null>(null),
    herramienta: signal<HerramientaDibujo | null>(null),
    seleccionado: signal<ElementoCapa | null>(null),
    hayCambios: signal(false),
    cantidadDeCambios: signal(0),
    guardando: signal(false),
    guardado: signal(false),
    error: signal(''),
    salidaPedida: signal<SalidaPedida | null>(null),
    usarHerramienta: vi.fn(),
    soltarHerramienta: vi.fn(),
    eliminarSeleccionado: vi.fn(),
    guardar: vi.fn(),
    terminar: vi.fn(),
    descartarYSalir: vi.fn()
  };
  let fixture: ComponentFixture<BarraDibujoComponent>;
  let elemento: HTMLElement;

  const boton = (nombre: string) => elemento.querySelector(`button[aria-label="${nombre}"]`) as HTMLButtonElement;
  const conTexto = (texto: string) =>
    Array.from(elemento.querySelectorAll('button')).find((b) => b.textContent?.includes(texto)) as HTMLButtonElement;
  const estado = () => elemento.querySelector('[role="status"]')?.textContent?.trim();

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
    edicion.capa.set(capa);
    edicion.herramienta.set(null);
    edicion.seleccionado.set(null);
    edicion.hayCambios.set(false);
    edicion.cantidadDeCambios.set(0);
    edicion.guardando.set(false);
    edicion.guardado.set(false);
    edicion.error.set('');
    edicion.salidaPedida.set(null);
    TestBed.configureTestingModule({ providers: [{ provide: EdicionElementos, useValue: edicion }] });
    fixture = TestBed.createComponent(BarraDibujoComponent);
    fixture.detectChanges();
    elemento = fixture.nativeElement;
  });

  it('sin una capa en edición no muestra nada', () => {
    edicion.capa.set(null);
    fixture.detectChanges();

    expect(elemento.querySelector('.barra')).toBeNull();
  });

  it('dice en qué capa se dibuja y ofrece las herramientas de SIATE', () => {
    expect(elemento.querySelector('h2')?.textContent).toContain('Bodegas');
    for (const nombre of ['Marcador', 'Línea', 'Cuadro', 'Polígono', 'Círculo']) {
      expect(boton(nombre)).not.toBeNull();
    }
  });

  it('elige una herramienta, la marca e indica cómo usarla', () => {
    boton('Polígono').click();
    expect(edicion.usarHerramienta).toHaveBeenCalledWith('Polygon');

    edicion.herramienta.set('Polygon');
    fixture.detectChanges();

    expect(boton('Polígono').getAttribute('aria-pressed')).toBe('true');
    expect(boton('Línea').getAttribute('aria-pressed')).toBe('false');
    expect(estado()).toContain('doble clic para cerrar el polígono');
  });

  it('Esc suelta la herramienta', () => {
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(edicion.soltarHerramienta).toHaveBeenCalled();
  });

  it('Eliminar solo se puede usar con un elemento seleccionado', () => {
    expect(boton('Eliminar el elemento seleccionado').disabled).toBe(true);

    edicion.seleccionado.set({ id: 1 } as ElementoCapa);
    fixture.detectChanges();
    boton('Eliminar el elemento seleccionado').click();

    expect(edicion.eliminarSeleccionado).toHaveBeenCalled();
  });

  it('Guardar solo se puede usar con cambios, y muestra cuántos hay', () => {
    expect(conTexto('Guardar').disabled).toBe(true);

    edicion.hayCambios.set(true);
    edicion.cantidadDeCambios.set(3);
    fixture.detectChanges();

    expect(conTexto('Guardar').textContent).toContain('(3)');
    conTexto('Guardar').click();
    expect(edicion.guardar).toHaveBeenCalled();
  });

  it('avisa que se guardó y explica un error', () => {
    edicion.guardado.set(true);
    edicion.error.set('No se pudieron guardar los cambios.');
    fixture.detectChanges();

    expect(estado()).toBe('Los datos se han guardado correctamente.');
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain('No se pudieron guardar');
  });

  it('Terminar pasa por la edición, que decide si hay que preguntar', () => {
    conTexto('Terminar').click();

    expect(edicion.terminar).toHaveBeenCalled();
  });

  it('con cambios sin guardar pregunta antes de descartarlos', () => {
    const dialogo = () => elemento.querySelector('app-confirmacion dialog') as HTMLDialogElement;
    edicion.salidaPedida.set({ despues: vi.fn() });
    fixture.detectChanges();
    TestBed.tick();

    expect(dialogo().open).toBe(true);
    expect(dialogo().textContent).toContain('Hay cambios sin guardar');
    expect(dialogo().textContent).toContain('Bodegas');
    expect(edicion.descartarYSalir).not.toHaveBeenCalled();

    (elemento.querySelector('app-confirmacion .si') as HTMLButtonElement).click();

    expect(edicion.descartarYSalir).toHaveBeenCalled();
    expect(dialogo().open).toBe(false);
  });

  it('al cerrar la ventana avisa solo si hay cambios sin guardar', () => {
    const cerrar = () => {
      const evento = new Event('beforeunload', { cancelable: true });
      window.dispatchEvent(evento);
      return evento.defaultPrevented;
    };

    expect(cerrar()).toBe(false);

    edicion.hayCambios.set(true);
    expect(cerrar()).toBe(true);
  });
});

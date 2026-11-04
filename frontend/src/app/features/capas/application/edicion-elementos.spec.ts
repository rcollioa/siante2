import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { MapaPort } from '../../mapa/application/port/out/mapa.port';
import { ElementoMapa, OyenteEdicion } from '../../mapa/domain/mapa';
import { CambiosElementos } from '../domain/cambios-elementos';
import { CambiosRechazados, Capa, CapaNoEncontrada, ElementoCapa } from '../domain/capa';
import { CapaEnMapa, CapasAbiertas, ElementoElegido } from './capas-abiertas';
import { EdicionElementos } from './edicion-elementos';
import { CapasRepositorio } from './port/out/capas.repositorio';

describe('EdicionElementos', () => {
  const forma = { geometria: { type: 'Point', coordinates: [-70.65, -33.44] }, radio: 0 };
  const otraForma = { geometria: { type: 'Point', coordinates: [-71, -34] }, radio: 0 };
  const guardado: ElementoCapa = {
    ...forma,
    id: 3,
    nombre: 'Bodega',
    descripcion: '',
    estilo: { color: '', grosor: 0, relleno: '', icono: '' },
    metadatos: []
  };
  const capa = (cambios: Partial<Capa> = {}): Capa => ({
    id: 5,
    nombre: 'Bodegas',
    descripcion: '',
    visibilidad: 'PRIVADA',
    propia: true,
    editable: true,
    tipo: 'VECTORIAL',
    elementos: [guardado],
    servicio: null,
    ...cambios
  });
  const guardarElementos = vi.fn<(id: number, cambios: CambiosElementos) => Observable<number[]>>();
  const mapa = {
    midiendo: signal(false),
    editarCapa: vi.fn<(id: number, oyente: OyenteEdicion) => void>(),
    terminarEdicion: vi.fn(),
    usarHerramienta: vi.fn(),
    agregarElemento: vi.fn(),
    reemplazarElemento: vi.fn(),
    quitarElemento: vi.fn(),
    mostrarCapa: vi.fn()
  };
  const abiertas = signal<CapaEnMapa[]>([]);
  const elegido = signal<ElementoElegido | null>(null);
  const capasAbiertas = {
    abiertas,
    elegido,
    abrir: vi.fn((id: number, alAbrirse?: (capa: Capa) => void) => {
      const abierta = capa({ id });
      abiertas.update((lista) => [...lista, { capa: abierta, visible: true }]);
      alAbrirse?.(abierta);
    }),
    actualizar: vi.fn((id: number, datos: Partial<Capa>) =>
      abiertas.update((lista) => lista.map((a) => (a.capa.id === id ? { ...a, capa: { ...a.capa, ...datos } } : a)))
    ),
    alternarVisibilidad: vi.fn(),
    avisarError: vi.fn()
  };
  let edicion: EdicionElementos;

  const oyente = () => mapa.editarCapa.mock.lastCall![1];
  const enviados = () => guardarElementos.mock.lastCall![1];
  /** Lo que hace el mapa al recibir un clic: el elemento queda elegido. */
  const elegir = (elemento: ElementoMapa) => elegido.set({ capa: capa(), elemento: elemento as ElementoCapa });

  beforeEach(() => {
    vi.clearAllMocks();
    mapa.midiendo.set(false);
    abiertas.set([]);
    elegido.set(null);
    guardarElementos.mockReturnValue(of([10]));
    TestBed.configureTestingModule({
      providers: [
        { provide: CapasRepositorio, useValue: { guardarElementos } },
        { provide: MapaPort, useValue: mapa },
        { provide: CapasAbiertas, useValue: capasAbiertas }
      ]
    });
    edicion = TestBed.inject(EdicionElementos);
  });

  describe('al empezar', () => {
    it('abre la capa y la edita en el mapa', () => {
      edicion.iniciar(5);

      expect(capasAbiertas.abrir).toHaveBeenCalledWith(5, expect.any(Function));
      expect(mapa.editarCapa).toHaveBeenCalledWith(5, expect.anything());
      expect(edicion.capa()?.id).toBe(5);
      expect(edicion.hayCambios()).toBe(false);
    });

    it('en una capa que no se puede dibujar lo avisa y no entra a editar', () => {
      capasAbiertas.abrir.mockImplementationOnce((_id, alAbrirse) => alAbrirse?.(capa({ editable: false })));

      edicion.iniciar(5);

      expect(mapa.editarCapa).not.toHaveBeenCalled();
      expect(capasAbiertas.avisarError).toHaveBeenCalled();
      expect(edicion.capa()).toBeNull();
    });

    it('si ya la está editando no la abre de nuevo, y si estaba oculta la muestra', () => {
      edicion.iniciar(5);
      abiertas.update((lista) => lista.map((a) => ({ ...a, visible: false })));
      capasAbiertas.abrir.mockClear();

      edicion.iniciar(5);

      expect(capasAbiertas.abrir).not.toHaveBeenCalled();
      expect(capasAbiertas.alternarVisibilidad).toHaveBeenCalledWith(5);
    });

    it('pasar a otra capa sin cambios pendientes termina la anterior', () => {
      edicion.iniciar(5);

      edicion.iniciar(6);

      expect(mapa.terminarEdicion).toHaveBeenCalled();
      expect(edicion.capa()?.id).toBe(6);
    });
  });

  describe('herramientas', () => {
    beforeEach(() => edicion.iniciar(5));

    it('elige una herramienta y al repetirla la suelta, como en SIATE', () => {
      edicion.usarHerramienta('Polygon');
      expect(edicion.herramienta()).toBe('Polygon');
      expect(mapa.usarHerramienta).toHaveBeenLastCalledWith('Polygon');

      edicion.usarHerramienta('Polygon');
      expect(edicion.herramienta()).toBeNull();
      expect(mapa.usarHerramienta).toHaveBeenLastCalledWith(null);
    });

    it('pasa directo de una herramienta a otra', () => {
      edicion.usarHerramienta('Polygon');
      edicion.usarHerramienta('Circle');

      expect(edicion.herramienta()).toBe('Circle');
    });

    it('activar la regla en el mapa deja la barra sin herramienta elegida', () => {
      edicion.usarHerramienta('Polygon');

      mapa.midiendo.set(true);
      TestBed.tick();

      expect(edicion.herramienta()).toBeNull();
    });

    it('soltar sin herramienta no molesta al mapa', () => {
      edicion.soltarHerramienta();

      expect(mapa.usarHerramienta).not.toHaveBeenCalled();
    });

    it('elegir una herramienta muestra la capa si estaba oculta: dibujar en ella no se vería', () => {
      abiertas.update((lista) => lista.map((a) => ({ ...a, visible: false })));

      edicion.usarHerramienta('Point');

      expect(capasAbiertas.alternarVisibilidad).toHaveBeenCalledWith(5);
    });
  });

  describe('cambios pendientes', () => {
    beforeEach(() => edicion.iniciar(5));

    it('lo dibujado se agrega al mapa con un ID provisional y queda pendiente', () => {
      oyente().alDibujar(forma);

      const [capaId, nuevo] = mapa.agregarElemento.mock.lastCall as [number, ElementoCapa];
      expect(capaId).toBe(5);
      expect(nuevo.id).toBeLessThan(0);
      expect(nuevo.geometria).toEqual(forma.geometria);
      expect(edicion.cantidadDeCambios()).toBe(1);
      expect(guardarElementos).not.toHaveBeenCalled();
    });

    it('si ocultó la capa mientras dibujaba, la muestra: lo dibujado no se vería', () => {
      abiertas.update((lista) => lista.map((a) => ({ ...a, visible: false })));

      oyente().alDibujar(forma);

      expect(capasAbiertas.alternarVisibilidad).toHaveBeenCalledWith(5);
    });

    it('cada elemento dibujado tiene su propio ID provisional', () => {
      oyente().alDibujar(forma);
      oyente().alDibujar(forma);

      const ids = mapa.agregarElemento.mock.calls.map(([, nuevo]) => nuevo.id);
      expect(new Set(ids).size).toBe(2);
    });

    it('mover un elemento guardado lo deja entre los modificados y actualiza el mapa', () => {
      oyente().alCambiarForma(guardado, otraForma);

      expect(mapa.reemplazarElemento).toHaveBeenCalledWith(5, guardado, { ...guardado, ...otraForma });
      expect(edicion.cantidadDeCambios()).toBe(1);
    });

    it('cambiar sus datos conserva todo lo demás', () => {
      edicion.cambiarDatos(guardado, { nombre: 'Central' });

      expect(mapa.reemplazarElemento).toHaveBeenCalledWith(5, guardado, { ...guardado, nombre: 'Central' });
    });

    it('solo se edita lo elegido en la capa en edición', () => {
      elegido.set({ capa: capa({ id: 9 }), elemento: guardado });
      expect(edicion.seleccionado()).toBeNull();

      elegir(guardado);
      expect(edicion.seleccionado()).toBe(guardado);
    });

    it('eliminar lo quita del mapa y queda pendiente', () => {
      elegir(guardado);

      edicion.eliminarSeleccionado();

      expect(mapa.quitarElemento).toHaveBeenCalledWith(5, guardado);
      expect(edicion.cantidadDeCambios()).toBe(1);
    });

    it('sin nada elegido eliminar no hace nada', () => {
      edicion.eliminarSeleccionado();

      expect(mapa.quitarElemento).not.toHaveBeenCalled();
      expect(edicion.hayCambios()).toBe(false);
    });

    it('si la capa se cierra por otro lado, ya no hay nada pendiente', () => {
      oyente().alDibujar(forma);

      abiertas.set([]);

      expect(edicion.capa()).toBeNull();
      expect(edicion.hayCambios()).toBe(false);
    });
  });

  describe('guardar', () => {
    beforeEach(() => edicion.iniciar(5));

    it('envía todo junto, y el mapa y la capa abierta quedan con el ID que asignó la base', () => {
      oyente().alDibujar(forma);
      const provisional = mapa.agregarElemento.mock.lastCall![1] as ElementoCapa;

      edicion.guardar();

      expect(guardarElementos).toHaveBeenCalledOnce();
      expect(enviados().nuevos).toEqual([provisional]);
      expect(mapa.reemplazarElemento).toHaveBeenCalledWith(5, provisional, { ...provisional, id: 10 });
      expect(capasAbiertas.actualizar).toHaveBeenCalledWith(5, { elementos: [guardado, { ...provisional, id: 10 }] });
      expect(edicion.hayCambios()).toBe(false);
      expect(edicion.guardado()).toBe(true);
    });

    it('envía los datos limpios y el mapa queda sin lo que se limpió', () => {
      edicion.cambiarDatos(guardado, { nombre: '  Central ' });
      const cambiado = mapa.reemplazarElemento.mock.lastCall![2] as ElementoCapa;

      edicion.guardar();

      expect(enviados().modificados[0].nombre).toBe('Central');
      expect(mapa.reemplazarElemento).toHaveBeenLastCalledWith(5, cambiado, { ...guardado, nombre: 'Central' });
    });

    it('sin cambios no escribe', () => {
      edicion.guardar();

      expect(guardarElementos).not.toHaveBeenCalled();
    });

    it('mientras guarda no envía dos veces', () => {
      const respuesta = new Subject<number[]>();
      guardarElementos.mockReturnValue(respuesta);
      oyente().alDibujar(forma);

      edicion.guardar();
      expect(edicion.guardando()).toBe(true);
      edicion.guardar();

      expect(guardarElementos).toHaveBeenCalledOnce();
      respuesta.next([10]);
      expect(edicion.guardando()).toBe(false);
    });

    it('si falla lo explica y los cambios siguen pendientes para reintentar', () => {
      guardarElementos.mockReturnValueOnce(throwError(() => new Error('sin conexión')));
      oyente().alDibujar(forma);

      edicion.guardar();

      expect(edicion.error()).toBe('No se pudieron guardar los cambios. Intente nuevamente.');
      expect(edicion.cantidadDeCambios()).toBe(1);
      expect(edicion.guardado()).toBe(false);

      edicion.guardar();
      expect(edicion.error()).toBe('');
      expect(edicion.hayCambios()).toBe(false);
    });

    it('muestra el motivo que da el backend y distingue una capa que ya no existe', () => {
      oyente().alDibujar(forma);

      guardarElementos.mockReturnValueOnce(throwError(() => new CambiosRechazados('El color no es válido.')));
      edicion.guardar();
      expect(edicion.error()).toBe('El color no es válido.');

      guardarElementos.mockReturnValueOnce(throwError(() => new CapaNoEncontrada(5)));
      edicion.guardar();
      expect(edicion.error()).toBe('La capa ya no existe o no tiene permiso para modificarla.');
    });

    it('un cambio nuevo borra el aviso de que se guardó', () => {
      oyente().alDibujar(forma);
      edicion.guardar();

      oyente().alDibujar(forma);

      expect(edicion.guardado()).toBe(false);
    });
  });

  describe('al terminar', () => {
    beforeEach(() => edicion.iniciar(5));

    it('sin cambios deja de editar y sigue con lo que se pidió', () => {
      const despues = vi.fn();

      edicion.terminar(despues);

      expect(mapa.terminarEdicion).toHaveBeenCalled();
      expect(edicion.capa()).toBeNull();
      expect(despues).toHaveBeenCalled();
      expect(edicion.salidaPedida()).toBeNull();
    });

    it('con cambios no hace nada hasta que se confirme', () => {
      const despues = vi.fn();
      oyente().alDibujar(forma);
      mapa.terminarEdicion.mockClear();

      edicion.terminar(despues);

      expect(edicion.salidaPedida()).not.toBeNull();
      expect(despues).not.toHaveBeenCalled();
      expect(mapa.terminarEdicion).not.toHaveBeenCalled();
      expect(edicion.capa()?.id).toBe(5);
    });

    it('al descartar vuelve a mostrar lo guardado y sigue con lo que se pidió', () => {
      const despues = vi.fn();
      oyente().alDibujar(forma);
      edicion.terminar(despues);

      edicion.descartarYSalir();

      expect(mapa.mostrarCapa).toHaveBeenCalledWith(5, [guardado]);
      expect(mapa.terminarEdicion).toHaveBeenCalled();
      expect(despues).toHaveBeenCalled();
      expect(edicion.salidaPedida()).toBeNull();
      expect(edicion.capa()).toBeNull();
    });

    it('pasar a otra capa con cambios también pregunta primero', () => {
      oyente().alDibujar(forma);

      edicion.iniciar(6);

      expect(capasAbiertas.abrir).not.toHaveBeenCalledWith(6, expect.anything());
      edicion.descartarYSalir();
      expect(edicion.capa()?.id).toBe(6);
      expect(edicion.hayCambios()).toBe(false);
    });

    it('al dejar de editar suelta la herramienta', () => {
      edicion.usarHerramienta('Point');

      edicion.terminar();

      expect(edicion.herramienta()).toBeNull();
    });
  });
});

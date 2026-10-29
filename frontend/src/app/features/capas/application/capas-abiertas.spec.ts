import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { MapaPort } from '../../mapa/application/port/out/mapa.port';
import { ElementoSeleccionado } from '../../mapa/domain/mapa';
import { Capa, CapaNoEncontrada, ElementoCapa, ServidorDeMapasNoDisponible } from '../domain/capa';
import { CapasAbiertas } from './capas-abiertas';
import { CapasRepositorio } from './port/out/capas.repositorio';

describe('CapasAbiertas', () => {
  const elemento: ElementoCapa = {
    id: 0,
    geometria: { type: 'Point', coordinates: [-70.65, -33.44] },
    radio: 0,
    nombre: 'Bodega',
    descripcion: '',
    estilo: { color: '', grosor: 0, relleno: '', icono: '' },
    metadatos: []
  };
  const capa = (id: number, cambios: Partial<Capa> = {}): Capa => ({
    id,
    nombre: `Capa ${id}`,
    descripcion: '',
    visibilidad: 'PRIVADA',
    propia: true,
    tipo: 'VECTORIAL',
    elementos: [elemento],
    servicio: null,
    ...cambios
  });
  let ver: ReturnType<typeof vi.fn<(id: number) => Observable<Capa>>>;
  // Dobles en memoria de los puertos: sin HTTP ni OpenLayers.
  const mapa = {
    mostrarCapa: vi.fn(),
    mostrarCapaWms: vi.fn(),
    quitarCapa: vi.fn(),
    cambiarVisibilidad: vi.fn(),
    acercarA: vi.fn(),
    deseleccionar: vi.fn(),
    quitarMarcador: vi.fn(),
    elementoSeleccionado: signal<ElementoSeleccionado | null>(null),
    capasSinRespuesta: signal<ReadonlySet<number>>(new Set())
  };
  let servicio: CapasAbiertas;
  const ids = () => servicio.abiertas().map(({ capa }) => capa.id);

  beforeEach(() => {
    vi.clearAllMocks();
    mapa.elementoSeleccionado.set(null);
    mapa.capasSinRespuesta.set(new Set());
    ver = vi.fn((id: number) => of(capa(id)));
    TestBed.configureTestingModule({
      providers: [
        { provide: CapasRepositorio, useValue: { ver } },
        { provide: MapaPort, useValue: mapa }
      ]
    });
    servicio = TestBed.inject(CapasAbiertas);
  });

  it('abre varias capas sobrepuestas y acerca el mapa a cada una al abrirla', () => {
    servicio.abrir(1);
    servicio.abrir(2);

    expect(ids()).toEqual([1, 2]);
    expect(mapa.mostrarCapa).toHaveBeenCalledWith(1, [elemento]);
    expect(mapa.acercarA).toHaveBeenLastCalledWith(2);
    expect(mapa.quitarMarcador).toHaveBeenCalled();
  });

  it('abrir una capa ya abierta no la descarga de nuevo: la muestra y la acerca', () => {
    servicio.abrir(1);
    servicio.alternarVisibilidad(1);

    servicio.abrir(1);

    expect(ver).toHaveBeenCalledTimes(1);
    expect(servicio.abiertas()[0].visible).toBe(true);
    expect(mapa.acercarA).toHaveBeenCalledTimes(2);
  });

  it('indica qué capas se están descargando y no pide dos veces la misma', () => {
    const respuesta = new Subject<Capa>();
    ver.mockReturnValue(respuesta);

    servicio.abrir(1);
    servicio.abrir(1);
    expect(servicio.abriendo().has(1)).toBe(true);
    expect(ver).toHaveBeenCalledTimes(1);

    respuesta.next(capa(1));
    expect(servicio.abriendo().size).toBe(0);
  });

  it('una capa por URL se abre sin elementos', () => {
    ver.mockReturnValue(of(capa(1, { tipo: 'URL', elementos: [] })));

    servicio.abrir(1);

    expect(mapa.mostrarCapa).toHaveBeenCalledWith(1, []);
  });

  it('una capa WMS se dibuja con las imágenes de su servidor, pedidas al backend, y se acerca a su extensión', () => {
    ver.mockReturnValue(of(capa(7, {
      tipo: 'URL',
      elementos: [],
      servicio: {
        tipo: 'WMS',
        urlMapa: '/siante2/api/capas/7/wms',
        capas: [
          { nombre: 'rios', titulo: 'Ríos', extension: { oeste: -76, sur: -56, este: -66, norte: -17 } },
          { nombre: 'lagos', titulo: 'Lagos', extension: { oeste: -73, sur: -42, este: -70, norte: -39 } }
        ]
      }
    })));

    servicio.abrir(7);

    expect(mapa.mostrarCapa).not.toHaveBeenCalled();
    expect(mapa.mostrarCapaWms).toHaveBeenCalledWith(7, {
      url: '/siante2/api/capas/7/wms',
      capas: ['rios', 'lagos'],
      extension: { oeste: -76, sur: -56, este: -66, norte: -17 }
    });
    expect(mapa.acercarA).toHaveBeenCalledWith(7);
  });

  it('dice qué capas WMS abiertas no reciben imágenes de su servidor', () => {
    servicio.abrir(1);
    servicio.abrir(2);

    mapa.capasSinRespuesta.set(new Set([2]));

    expect(servicio.sinRespuesta().map(({ id }) => id)).toEqual([2]);
  });

  it('si el servidor de mapas de la capa no responde lo dice', () => {
    ver.mockReturnValue(throwError(() => new ServidorDeMapasNoDisponible(7)));

    servicio.abrir(7);

    expect(servicio.error()).toBe('El servidor de mapas de la capa no responde. Intente más tarde.');
  });

  it('oculta y vuelve a mostrar una capa sin cerrarla', () => {
    servicio.abrir(1);

    servicio.alternarVisibilidad(1);
    expect(servicio.abiertas()[0].visible).toBe(false);
    expect(mapa.cambiarVisibilidad).toHaveBeenLastCalledWith(1, false);

    servicio.alternarVisibilidad(1);
    expect(mapa.cambiarVisibilidad).toHaveBeenLastCalledWith(1, true);
  });

  it('acercarse a una capa oculta también la muestra', () => {
    servicio.abrir(1);
    servicio.alternarVisibilidad(1);
    mapa.acercarA.mockClear();

    servicio.acercar(1);

    expect(servicio.abiertas()[0].visible).toBe(true);
    expect(mapa.acercarA).toHaveBeenCalledWith(1);
  });

  it('cerrar quita solo esa capa del mapa', () => {
    servicio.abrir(1);
    servicio.abrir(2);

    servicio.cerrar(1);

    expect(ids()).toEqual([2]);
    expect(mapa.quitarCapa).toHaveBeenCalledWith(1);
  });

  it('el elemento en que se hizo clic se muestra con su capa', () => {
    servicio.abrir(1);
    servicio.abrir(2);

    mapa.elementoSeleccionado.set({ capa: 2, elemento });

    expect(servicio.elegido()?.capa.nombre).toBe('Capa 2');
    expect(servicio.elegido()?.elemento).toBe(elemento);
  });

  it('si una capa ya no existe lo avisa y deja abiertas las demás', () => {
    servicio.abrir(1);
    ver.mockReturnValue(throwError(() => new CapaNoEncontrada(2)));

    servicio.abrir(2);

    expect(servicio.error()).toBe('La capa ya no existe o no tiene permiso para verla.');
    expect(ids()).toEqual([1]);

    servicio.descartarError();
    expect(servicio.error()).toBe('');
  });

  it('otros errores invitan a intentar de nuevo', () => {
    ver.mockReturnValue(throwError(() => new Error('sin conexión')));

    servicio.abrir(1);

    expect(servicio.error()).toBe('No se pudo abrir la capa. Intente nuevamente.');
    expect(servicio.abriendo().size).toBe(0);
  });
});

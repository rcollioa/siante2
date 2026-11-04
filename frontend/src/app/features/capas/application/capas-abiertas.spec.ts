import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { MapaPort } from '../../mapa/application/port/out/mapa.port';
import { ElementoSeleccionado, PuntoWms } from '../../mapa/domain/mapa';
import { Capa, CapaNoEncontrada, ElementoCapa, ElementoDeServicio, ServidorDeMapasNoDisponible } from '../domain/capa';
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
    editable: true,
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
    mostrarCapaWmts: vi.fn(),
    quitarCapa: vi.fn(),
    cambiarVisibilidad: vi.fn(),
    acercarA: vi.fn(),
    deseleccionar: vi.fn(),
    quitarMarcador: vi.fn(),
    elementoSeleccionado: signal<ElementoSeleccionado | null>(null),
    consultaWms: signal<readonly PuntoWms[]>([]),
    capasSinRespuesta: signal<ReadonlySet<number>>(new Set())
  };
  let consultarPunto: ReturnType<typeof vi.fn<(punto: PuntoWms) => Observable<ElementoDeServicio[]>>>;
  let servicio: CapasAbiertas;
  const ids = () => servicio.abiertas().map(({ capa }) => capa.id);

  beforeEach(() => {
    vi.clearAllMocks();
    mapa.elementoSeleccionado.set(null);
    mapa.capasSinRespuesta.set(new Set());
    mapa.consultaWms.set([]);
    ver = vi.fn((id: number) => of(capa(id)));
    consultarPunto = vi.fn(() => of([]));
    TestBed.configureTestingModule({
      providers: [
        { provide: CapasRepositorio, useValue: { ver, consultarPunto } },
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
        ],
        disponible: true,
        grilla: null
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

  it('una capa WMTS se dibuja con los mosaicos de su servidor, pedidos al backend con su grilla', () => {
    const grilla = {
      crs: 'EPSG:3857',
      matrices: [{ id: '0', resolucion: 156543.03, origenX: -2e7, origenY: 2e7, anchoMosaico: 256, altoMosaico: 256, columnas: 1, filas: 1 }]
    };
    ver.mockReturnValue(of(capa(9, {
      tipo: 'URL',
      elementos: [],
      servicio: {
        tipo: 'WMTS',
        urlMapa: '/siante2/api/capas/9/wmts',
        capas: [{ nombre: 'orto', titulo: 'Ortofoto', extension: null }],
        disponible: true,
        grilla
      }
    })));

    servicio.abrir(9);

    expect(mapa.mostrarCapa).not.toHaveBeenCalled();
    expect(mapa.mostrarCapaWmts).toHaveBeenCalledWith(9, { url: '/siante2/api/capas/9/wmts', capas: ['orto'], extension: null, grilla });
  });

  it('una capa WMS cuyo servidor no respondió se abre vacía y se avisa, como en SIATE', () => {
    ver.mockReturnValue(of(capa(7, {
      tipo: 'URL',
      elementos: [],
      servicio: { tipo: 'WMS', urlMapa: '/siante2/api/capas/7/wms', capas: [], disponible: false, grilla: null }
    })));

    servicio.abrir(7);

    expect(servicio.error()).toBe('');
    expect(ids()).toEqual([7]);
    expect(mapa.mostrarCapaWms).not.toHaveBeenCalled();
    expect(mapa.mostrarCapa).toHaveBeenCalledWith(7, []);
    expect(servicio.sinRespuesta().map(({ id }) => id)).toEqual([7]);
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

  it('una capa editada mientras está abierta muestra sus datos nuevos y conserva sus elementos', () => {
    servicio.abrir(1);
    servicio.abrir(2);
    ver.mockClear();

    servicio.actualizar(1, { nombre: 'Bodegas norte', visibilidad: 'PUBLICA' });

    const [primera, segunda] = servicio.abiertas();
    expect(primera.capa).toEqual(capa(1, { nombre: 'Bodegas norte', visibilidad: 'PUBLICA' }));
    expect(primera.visible).toBe(true);
    expect(segunda.capa).toEqual(capa(2));
    expect(ver).not.toHaveBeenCalled();
    expect(mapa.quitarCapa).not.toHaveBeenCalled();
  });

  it('otros errores invitan a intentar de nuevo', () => {
    ver.mockReturnValue(throwError(() => new Error('sin conexión')));

    servicio.abrir(1);

    expect(servicio.error()).toBe('No se pudo abrir la capa. Intente nuevamente.');
    expect(servicio.abriendo().size).toBe(0);
  });

  describe('consulta de un punto en capas WMS', () => {
    const punto = (id: number): PuntoWms => ({
      capa: id,
      capas: ['rios'],
      bbox: '0,0,10,10',
      ancho: 256,
      alto: 256,
      crs: 'EPSG:3857',
      version: '1.3.0',
      i: 1,
      j: 2
    });
    const rios: ElementoDeServicio = { capa: 'Ríos', datos: [{ nombre: 'NOMBRE', valor: 'Maipo' }] };

    beforeEach(() => {
      servicio.abrir(7);
      servicio.abrir(8);
      servicio.abrir(9);
    });

    it('sin un clic sobre capas WMS no hay nada consultado', () => {
      expect(servicio.consultado()).toBeNull();
    });

    it('muestra solo las capas con algo en el punto, y las que no respondieron', () => {
      consultarPunto.mockImplementation(({ capa: id }) =>
        id === 7 ? of([rios]) : id === 8 ? of([]) : throwError(() => new ServidorDeMapasNoDisponible(id))
      );

      mapa.consultaWms.set([punto(7), punto(8), punto(9)]);
      TestBed.tick();

      expect(consultarPunto).toHaveBeenCalledTimes(3);
      const consultado = servicio.consultado()!;
      expect(consultado.consultando).toBe(false);
      expect(consultado.resultados.map(({ capa, elementos }) => [capa.id, elementos])).toEqual([[7, [rios]]]);
      expect(consultado.sinRespuesta.map(({ id }) => id)).toEqual([9]);
    });

    it('mientras los servidores responden lo dice, y otro clic deja la consulta anterior', () => {
      const lenta = new Subject<ElementoDeServicio[]>();
      consultarPunto.mockReturnValueOnce(lenta).mockReturnValue(of([rios]));

      mapa.consultaWms.set([punto(7)]);
      TestBed.tick();
      expect(servicio.consultado()).toEqual({ consultando: true, resultados: [], sinRespuesta: [] });

      mapa.consultaWms.set([punto(7)]);
      TestBed.tick();
      expect(lenta.observed).toBe(false);
      expect(servicio.consultado()!.resultados).toHaveLength(1);
    });

    it('una capa cerrada deja de aparecer en lo consultado', () => {
      consultarPunto.mockReturnValue(of([rios]));
      mapa.consultaWms.set([punto(7), punto(8)]);
      TestBed.tick();

      servicio.cerrar(7);

      expect(servicio.consultado()!.resultados.map(({ capa }) => capa.id)).toEqual([8]);
    });
  });
});

import LayerGroup from 'ol/layer/Group';
import TileLayer from 'ol/layer/Tile';
import TileWMS from 'ol/source/TileWMS';
import VectorLayer from 'ol/layer/Vector';
import { toLonLat } from 'ol/proj';
import TileSource from 'ol/source/Tile';
import VectorSource from 'ol/source/Vector';
import { ElementoMapa } from '../../../domain/mapa';
import { ELEMENTO } from './elementos-openlayers';
import { MapaOpenLayers } from './mapa-openlayers';

describe('MapaOpenLayers', () => {
  let servicio: MapaOpenLayers;

  // Las dos primeras capas son las base; después, el grupo con las capas abiertas, el marcador de direcciones
  // y la posición del usuario.
  const capas = () => servicio.mapa.getLayers().getArray();
  const visibles = () => capas().slice(0, 2).map((capa) => capa.getVisible());
  const abiertas = () => (capas()[2] as LayerGroup).getLayers().getArray() as VectorLayer<VectorSource>[];
  const elementos = (indice: number) => abiertas()[indice].getSource()!.getFeatures();
  const marcadores = () => (capas()[3] as VectorLayer<VectorSource>).getSource()!.getFeatures();
  const ubicaciones = () => (capas()[4] as VectorLayer<VectorSource>).getSource()!.getFeatures();

  beforeEach(() => {
    // jsdom no trae ResizeObserver y OpenLayers lo usa al crear el mapa.
    vi.stubGlobal('ResizeObserver', class { observe = vi.fn(); unobserve = vi.fn(); disconnect = vi.fn(); });
    localStorage.clear();
    servicio = new MapaOpenLayers();
  });

  afterEach(() => vi.unstubAllGlobals());

  it('parte con el mapa como capa base y el satelital oculto', () => {
    expect(servicio.capaBase()).toBe('mapa');
    expect(visibles()).toEqual([true, false]);
  });

  it('al cambiar de capa base deja visible solo la elegida', () => {
    servicio.usarCapaBase('satelital');
    expect(servicio.capaBase()).toBe('satelital');
    expect(visibles()).toEqual([false, true]);

    servicio.usarCapaBase('mapa');
    expect(visibles()).toEqual([true, false]);
  });

  it('recuerda la capa base elegida en el siguiente ingreso', () => {
    servicio.usarCapaBase('satelital');

    const siguienteIngreso = new MapaOpenLayers();

    expect(siguienteIngreso.capaBase()).toBe('satelital');
    expect(siguienteIngreso.mapa.getLayers().getArray().slice(0, 2).map((capa) => capa.getVisible())).toEqual([false, true]);
  });

  it('sin almacenamiento disponible parte con el mapa y no falla', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('bloqueado', 'SecurityError');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('bloqueado', 'SecurityError');
    });

    const sinAlmacenamiento = new MapaOpenLayers();
    sinAlmacenamiento.usarCapaBase('satelital');

    expect(sinAlmacenamiento.capaBase()).toBe('satelital');
    vi.restoreAllMocks();
  });

  describe('aviso de mapas base', () => {
    const fuenteBase = () => (capas()[0] as TileLayer<TileSource>).getSource()!;

    it('avisa cuando fallan 4 teselas seguidas', () => {
      for (let i = 0; i < 3; i++) {
        fuenteBase().dispatchEvent('tileloaderror');
      }
      expect(servicio.sinMapaBase()).toBe(false);

      fuenteBase().dispatchEvent('tileloaderror');
      expect(servicio.sinMapaBase()).toBe(true);

      servicio.descartarAvisoMapaBase();
      expect(servicio.sinMapaBase()).toBe(false);
    });

    it('una tesela que carga reinicia la cuenta y quita el aviso', () => {
      for (let i = 0; i < 4; i++) {
        fuenteBase().dispatchEvent('tileloaderror');
      }
      fuenteBase().dispatchEvent('tileloadend');
      expect(servicio.sinMapaBase()).toBe(false);

      for (let i = 0; i < 3; i++) {
        fuenteBase().dispatchEvent('tileloaderror');
      }
      expect(servicio.sinMapaBase()).toBe(false);
    });
  });

  it('irA centra el mapa en la dirección y deja un solo marcador', () => {
    // Movimiento reducido: sin animación, el centro cambia de inmediato.
    vi.stubGlobal('matchMedia', () => ({ matches: true }));
    servicio.irA(-70.65, -33.44);
    servicio.irA(-71.62, -33.04);

    expect(marcadores()).toHaveLength(1);
    const [lon, lat] = toLonLat(servicio.mapa.getView().getCenter()!);
    expect(lon).toBeCloseTo(-71.62);
    expect(lat).toBeCloseTo(-33.04);

    servicio.quitarMarcador();
    expect(marcadores()).toHaveLength(0);
  });

  describe('capas abiertas', () => {
    const punto = (lon: number, lat: number): ElementoMapa => ({
      geometria: { type: 'Point', coordinates: [lon, lat] },
      radio: 0,
      estilo: { color: '', grosor: 0, relleno: '', icono: '' }
    });

    beforeEach(() => vi.stubGlobal('matchMedia', () => ({ matches: true })));

    it('muestra cada capa en su propia capa del mapa, la última encima', () => {
      const bodega = punto(-70.65, -33.44);
      servicio.mostrarCapa(1, [bodega]);
      servicio.mostrarCapa(2, [punto(-71.6, -33.0), punto(-71.5, -32.9)]);

      expect(abiertas()).toHaveLength(2);
      expect(elementos(1)).toHaveLength(2);
      // Al hacer clic se entrega el mismo elemento que se mostró, con su capa.
      expect(elementos(0)[0].get(ELEMENTO)).toBe(bodega);
      expect(elementos(0)[0].get('capa')).toBe(1);
    });

    it('mostrar de nuevo una capa reemplaza sus elementos, sin agregar otra', () => {
      servicio.mostrarCapa(1, [punto(-70.65, -33.44)]);
      servicio.mostrarCapa(1, [punto(-71.6, -33.0), punto(-71.5, -32.9)]);

      expect(abiertas()).toHaveLength(1);
      expect(elementos(0)).toHaveLength(2);
    });

    it('acerca el mapa a los elementos de una capa; sin tamaño, al montarse', () => {
      servicio.mostrarCapa(1, [punto(-71.6, -33.0), punto(-71.5, -32.9)]);

      servicio.acercarA(1);
      servicio.mapa.setSize([800, 600]);

      const [lon, lat] = toLonLat(servicio.mapa.getView().getCenter()!);
      expect(lon).toBeCloseTo(-71.55);
      expect(lat).toBeCloseTo(-32.95, 1);
    });

    it('una capa sin elementos no mueve el mapa', () => {
      servicio.mostrarCapa(1, []);
      servicio.mapa.setSize([800, 600]);

      servicio.acercarA(1);

      expect(servicio.mapa.getView().getZoom()).toBe(4);
    });

    it('una capa WMS pide sus mosaicos al backend con sus capas y se acerca a la extensión del servidor', () => {
      servicio.mostrarCapaWms(7, {
        url: '/siante2/api/capas/7/wms',
        capas: ['rios', 'lagos'],
        extension: { oeste: -71.6, sur: -33.1, este: -71.5, norte: -33.0 }
      });
      const capa = abiertas()[0] as unknown as TileLayer<TileWMS>;

      expect(capa).toBeInstanceOf(TileLayer);
      expect(capa.getSource()!.getUrls()).toEqual(['/siante2/api/capas/7/wms']);
      expect(capa.getSource()!.getParams()).toMatchObject({ LAYERS: 'rios,lagos', TILED: true });

      servicio.mapa.setSize([800, 600]);
      servicio.acercarA(7);
      const [lon, lat] = toLonLat(servicio.mapa.getView().getCenter()!);
      expect(lon).toBeCloseTo(-71.55);
      expect(lat).toBeCloseTo(-33.05, 1);
    });

    it('marca una capa WMS sin respuesta cuando un mosaico falla, y la desmarca cuando uno llega', () => {
      servicio.mostrarCapaWms(7, { url: '/siante2/api/capas/7/wms', capas: ['rios'], extension: null });
      const fuente = (abiertas()[0] as unknown as TileLayer<TileWMS>).getSource()!;

      fuente.dispatchEvent('tileloaderror');
      expect(servicio.capasSinRespuesta().has(7)).toBe(true);

      fuente.dispatchEvent('tileloadend');
      expect(servicio.capasSinRespuesta().has(7)).toBe(false);

      fuente.dispatchEvent('tileloaderror');
      servicio.quitarCapa(7);
      expect(servicio.capasSinRespuesta().size).toBe(0);
    });

    it('una capa WMS sin extensión no mueve el mapa al acercarse', () => {
      servicio.mostrarCapaWms(7, { url: '/siante2/api/capas/7/wms', capas: ['rios'], extension: null });
      servicio.mapa.setSize([800, 600]);

      servicio.acercarA(7);

      expect(servicio.mapa.getView().getZoom()).toBe(4);
    });

    it('un clic fuera de los elementos consulta el punto a las capas WMS visibles, la de más arriba primero', () => {
      servicio.mostrarCapaWms(7, { url: '/siante2/api/capas/7/wms', capas: ['rios'], extension: null });
      servicio.mostrarCapaWms(8, { url: '/siante2/api/capas/8/wms', capas: ['ide:lagos', 'ide:canales'], extension: null });
      servicio.mostrarCapaWms(9, { url: '/siante2/api/capas/9/wms', capas: ['oculta'], extension: null });
      servicio.cambiarVisibilidad(9, false);
      // Sin dibujar, OpenLayers no encuentra elementos en el píxel: el clic cae sobre las capas WMS.
      servicio.mapa.dispatchEvent({ type: 'singleclick', pixel: [10, 10], coordinate: [-7_850_000, -3_950_000] } as never);

      const puntos = servicio.consultaWms();
      expect(puntos.map(({ capa }) => capa)).toEqual([8, 7]);
      expect(puntos[0]).toMatchObject({ capas: ['ide:lagos', 'ide:canales'], crs: 'EPSG:3857', version: '1.3.0', ancho: 256, alto: 256 });
      expect(puntos[0].bbox.split(',')).toHaveLength(4);
      expect(puntos[0].i).toBeGreaterThanOrEqual(0);
      expect(puntos[0].i).toBeLessThan(256);

      servicio.cambiarVisibilidad(8, false);
      expect(servicio.consultaWms().map(({ capa }) => capa)).toEqual([7]);
      servicio.deseleccionar();
      expect(servicio.consultaWms()).toEqual([]);
    });

    it('ocultar o quitar una capa deselecciona su elemento, pero no el de otra', () => {
      const bodega = punto(-70.65, -33.44);
      servicio.mostrarCapa(1, [bodega]);
      servicio.mostrarCapa(2, [punto(-71.6, -33.0)]);
      servicio.elementoSeleccionado.set({ capa: 1, elemento: bodega });

      servicio.cambiarVisibilidad(2, false);
      expect(servicio.elementoSeleccionado()).not.toBeNull();
      expect(abiertas()[1].getVisible()).toBe(false);

      servicio.cambiarVisibilidad(1, false);
      expect(servicio.elementoSeleccionado()).toBeNull();

      servicio.elementoSeleccionado.set({ capa: 1, elemento: bodega });
      servicio.quitarCapa(1);
      expect(servicio.elementoSeleccionado()).toBeNull();
      expect(abiertas()).toHaveLength(1);

      servicio.deseleccionar();
      expect(servicio.elementoSeleccionado()).toBeNull();
    });
  });

  it('entrega la zona que se está mirando en EPSG:4326', () => {
    expect(servicio.zonaVisible().zoom).toBe(4);

    vi.stubGlobal('matchMedia', () => ({ matches: true }));
    servicio.irA(-71.25, -29.9);

    const zona = servicio.zonaVisible();
    expect(zona.longitud).toBeCloseTo(-71.25);
    expect(zona.latitud).toBeCloseTo(-29.9);
    expect(zona.zoom).toBe(17);
  });

  describe('ubicarUsuario', () => {
    type Exito = (posicion: { coords: { longitude: number; latitude: number } }) => void;
    type Fallo = (error: { code: number }) => void;
    let getCurrentPosition: ReturnType<typeof vi.fn<(exito: Exito, fallo: Fallo) => void>>;

    // jsdom no trae geolocalización: se simula y se responde a mano.
    beforeEach(() => {
      getCurrentPosition = vi.fn();
      Object.defineProperty(navigator, 'geolocation', { value: { getCurrentPosition }, configurable: true });
    });

    afterEach(() => Reflect.deleteProperty(navigator, 'geolocation'));

    const responder = (longitud: number, latitud: number) =>
      getCurrentPosition.mock.calls[0][0]({ coords: { longitude: longitud, latitude: latitud } });
    const fallar = (code: number) => getCurrentPosition.mock.calls[0][1]({ code });
    const centro = () => toLonLat(servicio.mapa.getView().getCenter()!);

    it('centra el mapa en la posición del usuario con zoom 12 y la marca', () => {
      servicio.ubicarUsuario();
      responder(-70.6, -33.4);

      expect(ubicaciones()).toHaveLength(1);
      expect(centro()[0]).toBeCloseTo(-70.6);
      expect(centro()[1]).toBeCloseTo(-33.4);
      expect(servicio.mapa.getView().getZoom()).toBe(12);
    });

    it('en pantallas chicas parte con zoom 7, como SIATE', () => {
      vi.stubGlobal('matchMedia', (consulta: string) => ({ matches: consulta === '(max-width: 48rem)' }));
      servicio.ubicarUsuario();
      responder(-70.6, -33.4);

      expect(servicio.mapa.getView().getZoom()).toBe(7);
    });

    it('pide la posición una sola vez', () => {
      servicio.ubicarUsuario();
      servicio.ubicarUsuario();

      expect(getCurrentPosition).toHaveBeenCalledTimes(1);
    });

    it('si el usuario ya buscó una dirección, marca su posición sin mover el mapa', () => {
      vi.stubGlobal('matchMedia', () => ({ matches: true }));
      servicio.ubicarUsuario();
      servicio.irA(-71.62, -33.04);
      responder(-70.6, -33.4);

      expect(ubicaciones()).toHaveLength(1);
      expect(centro()[0]).toBeCloseTo(-71.62);
      expect(centro()[1]).toBeCloseTo(-33.04);
    });

    it('avisa si no se pudo obtener la posición', () => {
      servicio.ubicarUsuario();
      fallar(3);

      expect(servicio.avisoUbicacion()).toBe('no-disponible');

      servicio.descartarAvisoUbicacion();
      expect(servicio.avisoUbicacion()).toBeNull();
    });

    it('no avisa si el usuario negó el permiso', () => {
      servicio.ubicarUsuario();
      fallar(1);

      expect(servicio.avisoUbicacion()).toBeNull();
    });

    it('sin geolocalización en el navegador no hace nada', () => {
      Reflect.deleteProperty(navigator, 'geolocation');
      servicio.ubicarUsuario();

      expect(ubicaciones()).toHaveLength(0);
      expect(servicio.avisoUbicacion()).toBeNull();
    });

    describe('botón Mi ubicación', () => {
      it('vuelve a la posición ya conocida sin pedirla de nuevo', () => {
        vi.stubGlobal('matchMedia', () => ({ matches: true }));
        servicio.ubicarUsuario();
        responder(-70.6, -33.4);
        servicio.irA(-71.62, -33.04);

        servicio.irAMiUbicacion();

        expect(getCurrentPosition).toHaveBeenCalledTimes(1);
        expect(centro()[0]).toBeCloseTo(-70.6);
        expect(centro()[1]).toBeCloseTo(-33.4);
      });

      it('si no se conocía, la pide, la marca y centra el mapa', () => {
        vi.stubGlobal('matchMedia', () => ({ matches: true }));
        servicio.irAMiUbicacion();
        expect(servicio.ubicando()).toBe(true);

        responder(-70.6, -33.4);

        expect(servicio.ubicando()).toBe(false);
        expect(ubicaciones()).toHaveLength(1);
        expect(centro()[0]).toBeCloseTo(-70.6);
      });

      it('si falló al abrir, la vuelve a pedir y quita el aviso al obtenerla', () => {
        servicio.ubicarUsuario();
        fallar(3);
        expect(servicio.avisoUbicacion()).toBe('no-disponible');

        servicio.irAMiUbicacion();
        getCurrentPosition.mock.calls[1][0]({ coords: { longitude: -70.6, latitude: -33.4 } });

        expect(servicio.avisoUbicacion()).toBeNull();
      });

      it('con el permiso negado explica cómo habilitarlo', () => {
        servicio.irAMiUbicacion();
        fallar(1);

        expect(servicio.ubicando()).toBe(false);
        expect(servicio.avisoUbicacion()).toBe('sin-permiso');
      });

      it('sin geolocalización en el navegador avisa que no está disponible', () => {
        Reflect.deleteProperty(navigator, 'geolocation');
        servicio.irAMiUbicacion();

        expect(servicio.avisoUbicacion()).toBe('no-disponible');
      });
    });
  });
});

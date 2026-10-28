import TileLayer from 'ol/layer/Tile';
import VectorLayer from 'ol/layer/Vector';
import { toLonLat } from 'ol/proj';
import TileSource from 'ol/source/Tile';
import VectorSource from 'ol/source/Vector';
import { ElementoMapa } from '../../../domain/mapa';
import { ELEMENTO } from './elementos-openlayers';
import { MapaOpenLayers } from './mapa-openlayers';

describe('MapaOpenLayers', () => {
  let servicio: MapaOpenLayers;

  // Las dos primeras capas son las base; después, los elementos de la capa abierta, el marcador de direcciones
  // y la posición del usuario.
  const capas = () => servicio.mapa.getLayers().getArray();
  const visibles = () => capas().slice(0, 2).map((capa) => capa.getVisible());
  const elementos = () => (capas()[2] as VectorLayer<VectorSource>).getSource()!.getFeatures();
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

  it('muestra los elementos de una capa en lugar de los anteriores y acerca el mapa a ellos', () => {
    vi.stubGlobal('matchMedia', () => ({ matches: true }));
    const punto = (lon: number, lat: number): ElementoMapa => ({
      geometria: { type: 'Point', coordinates: [lon, lat] },
      radio: 0,
      estilo: { color: '', grosor: 0, relleno: '', icono: '' }
    });
    const bodega = punto(-70.65, -33.44);
    servicio.mostrarElementos([bodega]);
    // Al hacer clic se entrega el mismo elemento que se mostró.
    expect(elementos()[0].get(ELEMENTO)).toBe(bodega);
    servicio.elementoSeleccionado.set(bodega);

    // Sin tamaño (aún no se monta) no se ajusta; al tenerlo, sí, y solo a la última capa abierta.
    servicio.mostrarElementos([punto(-71.6, -33.0), punto(-71.5, -32.9)]);
    servicio.mapa.setSize([800, 600]);

    expect(elementos()).toHaveLength(2);
    expect(servicio.elementoSeleccionado()).toBeNull();
    const [lon, lat] = toLonLat(servicio.mapa.getView().getCenter()!);
    expect(lon).toBeCloseTo(-71.55);
    expect(lat).toBeCloseTo(-32.95, 1);

    servicio.quitarElementos();
    expect(elementos()).toHaveLength(0);
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

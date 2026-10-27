import VectorLayer from 'ol/layer/Vector';
import { toLonLat } from 'ol/proj';
import VectorSource from 'ol/source/Vector';
import { MapaService } from './mapa.service';

describe('MapaService', () => {
  let servicio: MapaService;

  // Las dos primeras capas son las base; después, el marcador de direcciones y la posición del usuario.
  const capas = () => servicio.mapa.getLayers().getArray();
  const visibles = () => capas().slice(0, 2).map((capa) => capa.getVisible());
  const marcadores = () => (capas()[2] as VectorLayer<VectorSource>).getSource()!.getFeatures();
  const ubicaciones = () => (capas()[3] as VectorLayer<VectorSource>).getSource()!.getFeatures();

  beforeEach(() => {
    // jsdom no trae ResizeObserver y OpenLayers lo usa al crear el mapa.
    vi.stubGlobal('ResizeObserver', class { observe = vi.fn(); unobserve = vi.fn(); disconnect = vi.fn(); });
    servicio = new MapaService();
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

      expect(servicio.sinUbicacion()).toBe(true);
    });

    it('no avisa si el usuario negó el permiso', () => {
      servicio.ubicarUsuario();
      fallar(1);

      expect(servicio.sinUbicacion()).toBe(false);
    });

    it('sin geolocalización en el navegador no hace nada', () => {
      Reflect.deleteProperty(navigator, 'geolocation');
      servicio.ubicarUsuario();

      expect(ubicaciones()).toHaveLength(0);
      expect(servicio.sinUbicacion()).toBe(false);
    });
  });
});

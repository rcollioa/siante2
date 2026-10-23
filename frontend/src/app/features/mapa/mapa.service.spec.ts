import VectorLayer from 'ol/layer/Vector';
import { toLonLat } from 'ol/proj';
import VectorSource from 'ol/source/Vector';
import { MapaService } from './mapa.service';

describe('MapaService', () => {
  let servicio: MapaService;

  // Las dos primeras capas son las base; la última, el marcador de direcciones.
  const capas = () => servicio.mapa.getLayers().getArray();
  const visibles = () => capas().slice(0, 2).map((capa) => capa.getVisible());
  const marcadores = () => (capas()[2] as VectorLayer<VectorSource>).getSource()!.getFeatures();

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
});

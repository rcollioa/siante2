import { MapaService } from './mapa.service';

describe('MapaService', () => {
  let servicio: MapaService;

  const visibles = () => servicio.mapa.getLayers().getArray().map((capa) => capa.getVisible());

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
});

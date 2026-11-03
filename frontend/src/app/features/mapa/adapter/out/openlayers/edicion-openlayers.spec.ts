import Collection from 'ol/Collection';
import Feature from 'ol/Feature';
import Circle from 'ol/geom/Circle';
import LineString from 'ol/geom/LineString';
import Point from 'ol/geom/Point';
import Polygon from 'ol/geom/Polygon';
import DoubleClickZoom from 'ol/interaction/DoubleClickZoom';
import Draw, { DrawEvent } from 'ol/interaction/Draw';
import Interaction from 'ol/interaction/Interaction';
import Modify, { ModifyEvent } from 'ol/interaction/Modify';
import Snap from 'ol/interaction/Snap';
import Translate, { TranslateEvent } from 'ol/interaction/Translate';
import VectorLayer from 'ol/layer/Vector';
import { fromLonLat, toLonLat } from 'ol/proj';
import VectorSource from 'ol/source/Vector';
import { Style } from 'ol/style';
import { ElementoMapa, OyenteEdicion } from '../../../domain/mapa';
import { ELEMENTO } from './elementos-openlayers';
import { MapaOpenLayers } from './mapa-openlayers';

describe('Edición en OpenLayers', () => {
  const sinEstilo = { color: '', grosor: 0, relleno: '', icono: '' };
  const punto: ElementoMapa = { geometria: { type: 'Point', coordinates: [-70.65, -33.44] }, radio: 0, estilo: sinEstilo };
  const linea: ElementoMapa = {
    geometria: { type: 'LineString', coordinates: [[-70.6, -33.4], [-70.5, -33.3]] },
    radio: 0,
    estilo: sinEstilo
  };
  const oyente = { alDibujar: vi.fn<OyenteEdicion['alDibujar']>(), alCambiarForma: vi.fn<OyenteEdicion['alCambiarForma']>() };
  let servicio: MapaOpenLayers;

  const interacciones = () => servicio.mapa.getInteractions().getArray();
  const de = <T extends Interaction>(tipo: new (...args: never[]) => T) => interacciones().filter((i) => i instanceof tipo) as T[];
  const zoomConDobleClic = () => de(DoubleClickZoom)[0].getActive();
  const fuente = (capa: number) =>
    (servicio.mapa.getLayers().getArray()[2] as import('ol/layer/Group').default)
      .getLayers()
      .getArray()
      .find((c) => c.get('capa') === capa) as VectorLayer<VectorSource>;
  const features = (capa: number) => fuente(capa).getSource()!.getFeatures();
  const featureDe = (capa: number, elemento: ElementoMapa) => features(capa).find((f) => f.get(ELEMENTO) === elemento) as Feature;
  /** Un clic en el mapa, sin pasar por el canvas de OpenLayers, que jsdom no dibuja. */
  function clicEn(feature: Feature | undefined): void {
    vi.spyOn(servicio.mapa, 'forEachFeatureAtPixel').mockReturnValue(feature as never);
    servicio.mapa.dispatchEvent({ type: 'singleclick', pixel: [0, 0], coordinate: [0, 0] } as never);
  }

  beforeEach(() => {
    // jsdom no trae ResizeObserver y OpenLayers lo usa al crear el mapa.
    vi.stubGlobal('ResizeObserver', class { observe = vi.fn(); unobserve = vi.fn(); disconnect = vi.fn(); });
    localStorage.clear();
    vi.clearAllMocks();
    servicio = new MapaOpenLayers();
    servicio.mostrarCapa(1, [punto, linea]);
    servicio.mostrarCapa(2, [{ ...punto }]);
  });

  afterEach(() => vi.unstubAllGlobals());

  describe('al editar una capa', () => {
    it('agrega las herramientas para mover y cambiar la forma, y al terminar las quita', () => {
      servicio.editarCapa(1, oyente);
      expect(de(Translate)).toHaveLength(1);
      expect(de(Modify)).toHaveLength(1);
      expect(de(Snap)).toHaveLength(1);

      servicio.terminarEdicion();
      expect(de(Translate)).toHaveLength(0);
      expect(de(Modify)).toHaveLength(0);
      expect(de(Snap)).toHaveLength(0);
    });

    it('Snap queda al final: tiene que ajustar la coordenada antes que las demás la usen', () => {
      servicio.editarCapa(1, oyente);
      expect(interacciones().at(-1)).toBeInstanceOf(Snap);

      servicio.usarHerramienta('Polygon');
      expect(interacciones().at(-1)).toBeInstanceOf(Snap);
      expect(interacciones().at(-2)).toBeInstanceOf(Draw);
    });

    it('una capa que no existe o no es vectorial no se edita', () => {
      servicio.editarCapa(99, oyente);

      expect(de(Modify)).toHaveLength(0);
    });

    it('editar otra capa termina la anterior', () => {
      servicio.editarCapa(1, oyente);
      servicio.editarCapa(2, oyente);

      expect(de(Modify)).toHaveLength(1);
    });

    it('cerrar la capa termina su edición', () => {
      servicio.editarCapa(1, oyente);

      servicio.quitarCapa(1);

      expect(de(Modify)).toHaveLength(0);
    });
  });

  describe('selección', () => {
    beforeEach(() => servicio.editarCapa(1, oyente));

    it('lo elegido en la capa en edición se resalta, sin perder su estilo', () => {
      const feature = featureDe(1, linea);

      clicEn(feature);

      expect(servicio.elementoSeleccionado()).toEqual({ capa: 1, elemento: linea });
      expect((feature.getStyle() as Style[]).length).toBeGreaterThan(1);
    });

    it('al elegir otro, el anterior vuelve a su estilo', () => {
      const anterior = featureDe(1, linea);
      clicEn(anterior);

      clicEn(featureDe(1, punto));

      expect(Array.isArray(anterior.getStyle())).toBe(false);
    });

    it('un elemento de otra capa se puede mirar, pero no se resalta', () => {
      const ajeno = features(2)[0];

      clicEn(ajeno);

      expect(servicio.elementoSeleccionado()?.capa).toBe(2);
      expect(Array.isArray(ajeno.getStyle())).toBe(false);
    });

    it('un clic fuera deselecciona', () => {
      clicEn(featureDe(1, linea));

      clicEn(undefined);

      expect(servicio.elementoSeleccionado()).toBeNull();
    });

    it('al terminar la edición no queda nada resaltado ni seleccionado', () => {
      const feature = featureDe(1, linea);
      clicEn(feature);

      servicio.terminarEdicion();

      expect(servicio.elementoSeleccionado()).toBeNull();
      expect(Array.isArray(feature.getStyle())).toBe(false);
    });
  });

  describe('dibujar', () => {
    beforeEach(() => servicio.editarCapa(1, oyente));

    it('la herramienta agrega un Draw, y null lo quita', () => {
      servicio.usarHerramienta('Point');
      expect(de(Draw)).toHaveLength(1);

      servicio.usarHerramienta('LineString');
      expect(de(Draw)).toHaveLength(1);

      servicio.usarHerramienta(null);
      expect(de(Draw)).toHaveLength(0);
    });

    it('mientras se dibuja, el doble clic no acerca el mapa', () => {
      servicio.usarHerramienta('Polygon');
      expect(zoomConDobleClic()).toBe(false);

      servicio.usarHerramienta(null);
      expect(zoomConDobleClic()).toBe(true);
    });

    it('mientras se dibuja, un clic no selecciona', () => {
      servicio.usarHerramienta('Point');

      clicEn(featureDe(1, linea));

      expect(servicio.elementoSeleccionado()).toBeNull();
    });

    it('avisa la forma dibujada en grados', () => {
      servicio.usarHerramienta('Point');

      de(Draw)[0].dispatchEvent(new DrawEvent('drawend' as never, new Feature(new Point(fromLonLat([-70.65, -33.44])))));

      const { geometria, radio } = oyente.alDibujar.mock.lastCall![0];
      expect(radio).toBe(0);
      expect(geometria.type).toBe('Point');
      expect((geometria.coordinates as number[])[0]).toBeCloseTo(-70.65);
      expect((geometria.coordinates as number[])[1]).toBeCloseTo(-33.44);
    });

    it('un círculo es un punto con radio, y uno sin tamaño se ignora', () => {
      servicio.usarHerramienta('Circle');
      const dibujo = de(Draw)[0];

      dibujo.dispatchEvent(new DrawEvent('drawend' as never, new Feature(new Circle(fromLonLat([-70.65, -33.44]), 123.456))));
      const forma = oyente.alDibujar.mock.lastCall![0];
      expect(forma.geometria.type).toBe('Point');
      expect(forma.radio).toBe(123.46);

      oyente.alDibujar.mockClear();
      dibujo.dispatchEvent(new DrawEvent('drawend' as never, new Feature(new Circle(fromLonLat([-70.65, -33.44]), 0))));
      expect(oyente.alDibujar).not.toHaveBeenCalled();
    });

    it('un cuadro es un polígono cerrado, y uno sin área se ignora', () => {
      const [x, y] = fromLonLat([-70.65, -33.44]);
      servicio.usarHerramienta('Box');
      const dibujo = de(Draw)[0];

      dibujo.dispatchEvent(
        new DrawEvent('drawend' as never, new Feature(new Polygon([[[x, y], [x + 100, y], [x + 100, y + 100], [x, y + 100], [x, y]]])))
      );
      const { geometria } = oyente.alDibujar.mock.lastCall![0];
      const [anillo] = geometria.coordinates as number[][][];
      expect(geometria.type).toBe('Polygon');
      expect(anillo[0]).toEqual(anillo[4]);

      oyente.alDibujar.mockClear();
      dibujo.dispatchEvent(new DrawEvent('drawend' as never, new Feature(new Polygon([[[x, y], [x, y], [x, y], [x, y]]]))));
      expect(oyente.alDibujar).not.toHaveBeenCalled();
    });
  });

  describe('mover y cambiar la forma', () => {
    beforeEach(() => servicio.editarCapa(1, oyente));

    it('solo se puede modificar lo seleccionado, y no mientras se dibuja', () => {
      const feature = featureDe(1, linea);
      const modificables = () => (de(Translate)[0] as unknown as { features_: Collection<Feature> }).features_.getArray();
      expect(modificables()).toEqual([]);

      clicEn(feature);
      expect(modificables()).toEqual([feature]);

      servicio.usarHerramienta('Point');
      expect(modificables()).toEqual([]);

      servicio.usarHerramienta(null);
      expect(modificables()).toEqual([feature]);
    });

    it('avisa la forma nueva al terminar de modificar', () => {
      const feature = featureDe(1, linea);
      const nueva = [fromLonLat([-71, -34]), fromLonLat([-72, -35])];
      (feature.getGeometry() as LineString).setCoordinates(nueva);

      de(Modify)[0].dispatchEvent(new ModifyEvent('modifyend' as never, new Collection([feature]), {} as never));

      const [elemento, forma] = oyente.alCambiarForma.mock.lastCall!;
      expect(elemento).toBe(linea);
      expect((forma.geometria.coordinates as number[][])[0][0]).toBeCloseTo(-71);
    });

    it('avisa al trasladar, pero no si fue un clic sin mover', () => {
      const feature = featureDe(1, linea);
      const trasladar = (inicio: number[], fin: number[]) =>
        de(Translate)[0].dispatchEvent(
          new TranslateEvent('translateend' as never, new Collection([feature]), fin, inicio, {} as never)
        );

      trasladar([0, 0], [0, 0]);
      expect(oyente.alCambiarForma).not.toHaveBeenCalled();

      trasladar([0, 0], [10, 10]);
      expect(oyente.alCambiarForma).toHaveBeenCalledOnce();
    });
  });

  describe('elementos', () => {
    beforeEach(() => servicio.editarCapa(1, oyente));

    it('agregar lo muestra en su capa y lo deja seleccionado', () => {
      const nuevo: ElementoMapa = { ...punto };

      servicio.agregarElemento(1, nuevo);

      expect(featureDe(1, nuevo)).toBeDefined();
      expect(featureDe(1, nuevo).get('capa')).toBe(1);
      expect(servicio.elementoSeleccionado()).toEqual({ capa: 1, elemento: nuevo });
      const [longitud] = toLonLat((featureDe(1, nuevo).getGeometry() as Point).getCoordinates());
      expect(longitud).toBeCloseTo(-70.65);
    });

    it('reemplazar conserva la forma del mapa, y mantiene la selección', () => {
      const feature = featureDe(1, linea);
      clicEn(feature);
      const movida = [fromLonLat([-71, -34]), fromLonLat([-72, -35])];
      (feature.getGeometry() as LineString).setCoordinates(movida);
      const nuevo: ElementoMapa = { ...linea, estilo: { ...sinEstilo, color: '#ff0000' } };

      servicio.reemplazarElemento(1, linea, nuevo);

      expect(feature.get(ELEMENTO)).toBe(nuevo);
      expect((feature.getGeometry() as LineString).getCoordinates()).toEqual(movida);
      expect(servicio.elementoSeleccionado()?.elemento).toBe(nuevo);
      expect((feature.getStyle() as Style[])[0].getStroke()?.getColor()).toBe('#ff0000');
    });

    it('reemplazar uno que no está seleccionado no cambia la selección', () => {
      clicEn(featureDe(1, punto));

      servicio.reemplazarElemento(1, linea, { ...linea });

      expect(servicio.elementoSeleccionado()?.elemento).toBe(punto);
    });

    it('quitar lo saca del mapa y lo deselecciona', () => {
      clicEn(featureDe(1, linea));

      servicio.quitarElemento(1, linea);

      expect(featureDe(1, linea)).toBeUndefined();
      expect(servicio.elementoSeleccionado()).toBeNull();
    });

    it('quitar otro no cambia la selección', () => {
      clicEn(featureDe(1, punto));

      servicio.quitarElemento(1, linea);

      expect(servicio.elementoSeleccionado()?.elemento).toBe(punto);
    });

    it('un elemento que no está en el mapa se ignora', () => {
      expect(() => servicio.reemplazarElemento(1, { ...punto }, punto)).not.toThrow();
      expect(() => servicio.quitarElemento(1, { ...punto })).not.toThrow();
    });
  });
});

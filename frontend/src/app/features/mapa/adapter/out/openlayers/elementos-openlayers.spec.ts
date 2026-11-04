import Circle from 'ol/geom/Circle';
import Point from 'ol/geom/Point';
import Polygon from 'ol/geom/Polygon';
import { toLonLat } from 'ol/proj';
import { Icon, Style } from 'ol/style';
import { ElementoMapa } from '../../../domain/mapa';
import { aFeatures, ELEMENTO } from './elementos-openlayers';

describe('aFeatures', () => {
  const sinEstilo = { color: '', grosor: 0, relleno: '', icono: '' };
  const elemento = (cambios: Partial<ElementoMapa>): ElementoMapa => ({
    geometria: { type: 'Point', coordinates: [-70.65, -33.44] },
    radio: 0,
    estilo: sinEstilo,
    ...cambios
  });
  const estilo = (indice: number, elementos: ElementoMapa[]) => aFeatures(elementos)[indice].getStyle() as Style;

  it('lleva cada elemento al mapa en EPSG:3857 y guarda el elemento para mostrar su información', () => {
    const marcador = elemento({});
    const [feature] = aFeatures([marcador]);

    const [lon, lat] = toLonLat((feature.getGeometry() as Point).getCoordinates());
    expect(lon).toBeCloseTo(-70.65);
    expect(lat).toBeCloseTo(-33.44);
    expect(feature.get(ELEMENTO)).toBe(marcador);
  });

  it('un círculo de SIATE vuelve a ser un círculo con su radio', () => {
    const [feature] = aFeatures([elemento({ radio: 500 })]);

    expect(feature.getGeometry()).toBeInstanceOf(Circle);
    expect((feature.getGeometry() as Circle).getRadius()).toBe(500);
  });

  it('usa el estilo guardado y, si falta, el de la aplicación', () => {
    const poligono = {
      type: 'Polygon',
      coordinates: [[[-70, -33], [-70.1, -33], [-70.1, -33.1], [-70, -33]]]
    };
    const elementos = [
      elemento({ geometria: poligono, estilo: { color: '#ff0000', grosor: 4, relleno: 'rgba(0, 0, 0, 0.5)', icono: '' } }),
      elemento({ geometria: poligono })
    ];

    expect(aFeatures(elementos)[0].getGeometry()).toBeInstanceOf(Polygon);
    expect(estilo(0, elementos).getStroke()?.getColor()).toBe('#ff0000');
    expect(estilo(0, elementos).getStroke()?.getWidth()).toBe(4);
    expect(estilo(0, elementos).getFill()?.getColor()).toBe('rgba(0, 0, 0, 0.5)');
    expect(estilo(1, elementos).getStroke()?.getColor()).toBe('#71aecb');
    expect(estilo(1, elementos).getStroke()?.getWidth()).toBe(2);
    expect(estilo(1, elementos).getFill()?.getColor()).toBe('rgba(230, 248, 253, 0.35)');
  });

  it('una línea sin estilo se dibuja con el trazo de SIATE', () => {
    const linea = { type: 'LineString', coordinates: [[-70, -33], [-70.1, -33]] };

    expect(estilo(0, [elemento({ geometria: linea })]).getStroke()?.getColor()).toBe('#71aecb');
    expect(estilo(0, [elemento({ geometria: linea, estilo: { ...sinEstilo, color: '#ff0000' } })]).getStroke()?.getColor()).toBe('#ff0000');
  });

  it('un punto es el pin de SIATE, salvo que traiga un ícono incrustado', () => {
    const icono = (guardado: string) => (estilo(0, [elemento({ estilo: { ...sinEstilo, icono: guardado } })]).getImage() as Icon).getSrc();

    expect(icono('')).toBe('assets/img/marker.svg');
    expect(icono('./assets/img/marker.svg')).toBe('assets/img/marker.svg');
    expect(icono('https://otro.cl/icono.png')).toBe('assets/img/marker.svg');
    expect(icono('data:image/svg+xml;base64,PHN2Zy8+')).toBe('data:image/svg+xml;base64,PHN2Zy8+');
  });

  it('omite los elementos que no se pueden dibujar', () => {
    const roto = elemento({ geometria: { type: 'Desconocido', coordinates: [] } });

    expect(aFeatures([roto, elemento({})])).toHaveLength(1);
  });
});

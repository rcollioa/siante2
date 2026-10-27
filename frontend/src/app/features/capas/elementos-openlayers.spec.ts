import Circle from 'ol/geom/Circle';
import Point from 'ol/geom/Point';
import Polygon from 'ol/geom/Polygon';
import { toLonLat } from 'ol/proj';
import { Circle as Punto, Icon, Style } from 'ol/style';
import { ElementoCapa } from './capas-api.service';
import { aFeatures, ELEMENTO } from './elementos-openlayers';

describe('aFeatures', () => {
  const sinEstilo = { color: '', grosor: 0, relleno: '', icono: '' };
  const elemento = (cambios: Partial<ElementoCapa>): ElementoCapa => ({
    id: 0,
    geometria: { type: 'Point', coordinates: [-70.65, -33.44], geometries: [] },
    radio: 0,
    nombre: '',
    descripcion: '',
    estilo: sinEstilo,
    metadatos: [],
    ...cambios
  });
  const estilo = (indice: number, elementos: ElementoCapa[]) => aFeatures(elementos)[indice].getStyle() as Style;

  it('lleva cada elemento al mapa en EPSG:3857 y guarda el elemento para mostrar su información', () => {
    const marcador = elemento({ nombre: 'Contraloría' });
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
      coordinates: [[[-70, -33], [-70.1, -33], [-70.1, -33.1], [-70, -33]]],
      geometries: []
    };
    const elementos = [
      elemento({ geometria: poligono, estilo: { color: '#ff0000', grosor: 4, relleno: 'rgba(0, 0, 0, 0.5)', icono: '' } }),
      elemento({ geometria: poligono })
    ];

    expect(aFeatures(elementos)[0].getGeometry()).toBeInstanceOf(Polygon);
    expect(estilo(0, elementos).getStroke()?.getColor()).toBe('#ff0000');
    expect(estilo(0, elementos).getStroke()?.getWidth()).toBe(4);
    expect(estilo(0, elementos).getFill()?.getColor()).toBe('rgba(0, 0, 0, 0.5)');
    expect(estilo(1, elementos).getStroke()?.getColor()).toBe('#4c6fed');
    expect(estilo(1, elementos).getStroke()?.getWidth()).toBe(2);
  });

  it('usa los íconos incrustados, pero no los de otros servidores, que la CSP bloquearía', () => {
    const elementos = [
      elemento({ estilo: { ...sinEstilo, icono: 'data:image/svg+xml;base64,PHN2Zy8+' } }),
      elemento({ estilo: { ...sinEstilo, icono: 'https://otro.cl/icono.png' } })
    ];

    expect(estilo(0, elementos).getImage()).toBeInstanceOf(Icon);
    expect(estilo(1, elementos).getImage()).toBeInstanceOf(Punto);
  });

  it('omite los elementos que no se pueden dibujar', () => {
    const roto = elemento({ geometria: { type: 'Desconocido', coordinates: [], geometries: [] } });

    expect(aFeatures([roto, elemento({})])).toHaveLength(1);
  });
});

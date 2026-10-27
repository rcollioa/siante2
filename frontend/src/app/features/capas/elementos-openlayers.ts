import Feature from 'ol/Feature';
import Circle from 'ol/geom/Circle';
import Geometry from 'ol/geom/Geometry';
import LineString from 'ol/geom/LineString';
import MultiLineString from 'ol/geom/MultiLineString';
import MultiPoint from 'ol/geom/MultiPoint';
import MultiPolygon from 'ol/geom/MultiPolygon';
import Point from 'ol/geom/Point';
import Polygon from 'ol/geom/Polygon';
import { fromLonLat } from 'ol/proj';
import { Circle as Punto, Fill, Icon, Stroke, Style } from 'ol/style';
import { ElementoCapa } from './capas-api.service';

/** Propiedad de cada Feature con su elemento, para mostrar su información al seleccionarlo. */
export const ELEMENTO = 'elemento';

// El canvas no lee variables CSS: mismos valores que --color-primary y --color-surface.
const COLOR = '#4c6fed';
const RELLENO = 'rgba(76, 111, 237, 0.2)';
const BLANCO = '#ffffff';

/** Un elemento que no se puede dibujar se omite: el resto de la capa se muestra igual. */
export function aFeatures(elementos: ElementoCapa[]): Feature[] {
  return elementos.flatMap((elemento) => {
    try {
      const feature = new Feature(geometria(elemento));
      feature.setStyle(estilo(elemento));
      feature.set(ELEMENTO, elemento);
      return [feature];
    } catch {
      return [];
    }
  });
}

/** GeoJSON (EPSG:4326) a la proyección del mapa, sin ol/format/GeoJSON, que pediría los tipos de geojson. */
function geometria({ geometria, radio }: ElementoCapa): Geometry {
  const coordenadas = geometria.coordinates as never;
  // Los círculos de SIATE: centro en EPSG:4326 y radio en unidades del mapa (EPSG:3857).
  if (radio > 0 && geometria.type === 'Point') {
    return new Circle(fromLonLat(coordenadas), radio);
  }
  return enGrados(geometria.type, coordenadas).transform('EPSG:4326', 'EPSG:3857');
}

function enGrados(tipo: string, coordenadas: never): Geometry {
  switch (tipo) {
    case 'Point':
      return new Point(coordenadas);
    case 'LineString':
      return new LineString(coordenadas);
    case 'Polygon':
      return new Polygon(coordenadas);
    case 'MultiPoint':
      return new MultiPoint(coordenadas);
    case 'MultiLineString':
      return new MultiLineString(coordenadas);
    case 'MultiPolygon':
      return new MultiPolygon(coordenadas);
    default:
      throw new Error(`Geometría no soportada: ${tipo}`);
  }
}

/** El estilo que guardó SIATE; lo que no definió, con los colores de la aplicación. */
function estilo({ geometria, radio, estilo: guardado }: ElementoCapa): Style {
  const color = guardado.color || COLOR;
  const esPunto = radio === 0 && (geometria.type === 'Point' || geometria.type === 'MultiPoint');
  if (esPunto) {
    // Solo íconos incrustados: la CSP bloquea imágenes de otros servidores (SIATE guardaba algunas así).
    const imagen = guardado.icono.startsWith('data:image/')
      ? new Icon({ src: guardado.icono, anchor: [0.5, 1] })
      : new Punto({ radius: 7, fill: new Fill({ color }), stroke: new Stroke({ color: BLANCO, width: 2 }) });
    return new Style({ image: imagen });
  }
  return new Style({
    stroke: new Stroke({ color, width: guardado.grosor || 2 }),
    fill: new Fill({ color: guardado.relleno || RELLENO })
  });
}

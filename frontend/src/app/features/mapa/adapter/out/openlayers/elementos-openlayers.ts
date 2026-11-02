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
import { Fill, Icon, Stroke, Style } from 'ol/style';
import { ElementoMapa } from '../../../domain/mapa';

/** Propiedad de cada Feature con su elemento, para entregarlo tal cual al seleccionarlo. */
export const ELEMENTO = 'elemento';

// Los de SIATE para lo que no definió su estilo (getDefaultStyle en su map.service).
const COLOR = '#71aecb';
const RELLENO = 'rgba(230, 248, 253, 0.35)';
/** El marcador de SIATE. Su punta está cerca del centro de la imagen: se ancla al centro, como en SIATE. */
const PIN = 'assets/img/marker.svg';

/** Un elemento que no se puede dibujar se omite: el resto de la capa se muestra igual. */
export function aFeatures(elementos: readonly ElementoMapa[]): Feature[] {
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
function geometria({ geometria, radio }: ElementoMapa): Geometry {
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
function estilo({ geometria, radio, estilo: guardado }: ElementoMapa): Style {
  const esPunto = radio === 0 && (geometria.type === 'Point' || geometria.type === 'MultiPoint');
  if (esPunto) {
    // SIATE guarda "./assets/img/marker.svg" o un ícono incrustado; los de otros servidores los bloquearía la CSP.
    return new Style({ image: new Icon({ src: guardado.icono.startsWith('data:image/') ? guardado.icono : PIN }) });
  }
  return new Style({
    stroke: new Stroke({ color: guardado.color || COLOR, width: guardado.grosor || 2 }),
    fill: new Fill({ color: guardado.relleno || RELLENO })
  });
}

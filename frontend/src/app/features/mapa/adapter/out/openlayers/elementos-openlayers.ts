import Feature from 'ol/Feature';
import Circle from 'ol/geom/Circle';
import Geometry from 'ol/geom/Geometry';
import LineString from 'ol/geom/LineString';
import MultiLineString from 'ol/geom/MultiLineString';
import MultiPoint from 'ol/geom/MultiPoint';
import MultiPolygon from 'ol/geom/MultiPolygon';
import Point from 'ol/geom/Point';
import Polygon from 'ol/geom/Polygon';
import SimpleGeometry from 'ol/geom/SimpleGeometry';
import { fromLonLat, toLonLat } from 'ol/proj';
import { Circle as CirculoDeEstilo, Fill, Icon, Stroke, Style } from 'ol/style';
import { COLOR_POR_DEFECTO, ElementoMapa, FormaElemento, GROSOR_POR_DEFECTO, RELLENO_POR_DEFECTO } from '../../../domain/mapa';

/** Propiedad de cada Feature con su elemento, para entregarlo tal cual al seleccionarlo. */
export const ELEMENTO = 'elemento';

/** El marcador de SIATE. Su punta está cerca del centro de la imagen: se ancla al centro, como en SIATE. */
const PIN = 'assets/img/marker.svg';

// El canvas no lee variables CSS: el mismo naranjo de la posición del usuario, que se distingue sobre el mapa y el satélite.
const COLOR_RESALTE = '#ed7d31';
const RESALTE = new Style({
  image: new CirculoDeEstilo({ radius: 14, stroke: new Stroke({ color: COLOR_RESALTE, width: 3 }) }),
  stroke: new Stroke({ color: COLOR_RESALTE, width: 3, lineDash: [8, 6] })
});
/** Los vértices que se pueden arrastrar: sin ellos, nada indica que se puede cambiar la forma. */
const VERTICES = new Style({
  image: new CirculoDeEstilo({ radius: 5, fill: new Fill({ color: '#ffffff' }), stroke: new Stroke({ color: COLOR_RESALTE, width: 2 }) }),
  geometry: (feature) => {
    const forma = feature.getGeometry();
    return forma instanceof SimpleGeometry && !(forma instanceof Circle) && !(forma instanceof Point)
      ? new MultiPoint(forma.getFlatCoordinates() as never, forma.getLayout())
      : undefined;
  }
});

/** Un elemento que no se puede dibujar se omite: el resto de la capa se muestra igual. */
export function aFeatures(elementos: readonly ElementoMapa[]): Feature[] {
  return elementos.flatMap((elemento) => {
    try {
      const feature = new Feature(geometria(elemento));
      feature.setStyle(estiloDe(elemento));
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
export function estiloDe({ geometria, radio, estilo: guardado }: ElementoMapa): Style {
  const esPunto = radio === 0 && (geometria.type === 'Point' || geometria.type === 'MultiPoint');
  if (esPunto) {
    // SIATE guarda "./assets/img/marker.svg" o un ícono incrustado; los de otros servidores los bloquearía la CSP.
    return new Style({ image: new Icon({ src: guardado.icono.startsWith('data:image/') ? guardado.icono : PIN }) });
  }
  return new Style({
    stroke: new Stroke({ color: guardado.color || COLOR_POR_DEFECTO, width: guardado.grosor || GROSOR_POR_DEFECTO }),
    fill: new Fill({ color: guardado.relleno || RELLENO_POR_DEFECTO })
  });
}

/** El elemento seleccionado en la capa en edición, sobre su estilo: se sigue viendo cómo queda. */
export function estiloResaltado(elemento: ElementoMapa): Style[] {
  return [estiloDe(elemento), RESALTE, VERTICES];
}

/** Lo que muestra el mapa, de vuelta a GeoJSON (EPSG:4326). Null si no tiene tamaño: un círculo de radio 0 sería un punto
 * al leerlo, y un cuadro sin área no sirve. El radio con dos decimales, como lo guarda SIATE. */
export function formaDe(forma: Geometry): FormaElemento | null {
  if (forma instanceof Circle) {
    const radio = Math.round(forma.getRadius() * 100) / 100;
    return radio > 0 ? { geometria: { type: 'Point', coordinates: toLonLat(forma.getCenter()) }, radio } : null;
  }
  if (forma instanceof Polygon && forma.getArea() === 0) {
    return null;
  }
  const enGrados = forma.clone().transform('EPSG:3857', 'EPSG:4326') as SimpleGeometry;
  return { geometria: { type: forma.getType(), coordinates: enGrados.getCoordinates() ?? [] }, radio: 0 };
}

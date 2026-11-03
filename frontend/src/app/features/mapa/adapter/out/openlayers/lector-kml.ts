import Feature from 'ol/Feature';
import { asString, Color } from 'ol/color';
import KML from 'ol/format/KML';
import Geometry from 'ol/geom/Geometry';
import GeometryCollection from 'ol/geom/GeometryCollection';
import SimpleGeometry from 'ol/geom/SimpleGeometry';
import { Style } from 'ol/style';
import { ElementoLeido, EstiloElemento, Geometria, sinAltura } from '../../../domain/mapa';

/** Lo que OpenLayers entrega como propiedad de un Placemark y no es un dato para mostrar. */
const SIN_INTERES = new Set(['geometry', 'name', 'description', 'Shape', 'styleUrl', 'visibility', 'open', 'displayName']);
/** Lo que OpenLayers usa si el KML no define un estilo. Opaco: un polígono taparía el mapa. */
const BLANCO_POR_DEFECTO = 'rgba(255,255,255,1)';

/** En grados y en dos dimensiones. Si no se puede leer, vacío: OpenLayers no distingue un XML dañado de uno sin elementos. */
export function leerKml(texto: string): ElementoLeido[] {
  const lector = new KML({ showPointNames: false });
  const features = lector.readFeatures(sinEsquema(texto), { dataProjection: 'EPSG:4326', featureProjection: 'EPSG:4326' });
  return features.flatMap(aElementos);
}

/** Muchos KML declaran `xsi:schemaLocation` sin el espacio de nombres `xsi`, y el XML queda inválido (`replceXSI` en SIATE). */
function sinEsquema(texto: string): string {
  return texto.replace(/\s+xsi:schemaLocation\s*=\s*("[^"]*"|'[^']*')/g, '');
}

function aElementos(feature: Feature): ElementoLeido[] {
  const geometria = feature.getGeometry();
  const nombre = feature.get('name');
  const descripcion = feature.get('description');
  const datos = {
    nombre: typeof nombre === 'string' ? nombre : '',
    descripcion: typeof descripcion === 'string' ? descripcion : '',
    metadatos: metadatosDe(feature),
    estilo: estiloDe(feature)
  };
  return (geometria ? geometriasDe(geometria) : [{ type: '', coordinates: [] }]).map((forma) => ({ ...datos, geometria: forma, radio: 0 }));
}

/** Un MultiGeometry de varios tipos llega como colección: un elemento por cada parte, ya que SIATE no guarda colecciones. */
function geometriasDe(geometria: Geometry): Geometria[] {
  if (geometria instanceof GeometryCollection) {
    return geometria.getGeometriesArray().flatMap(geometriasDe);
  }
  return geometria instanceof SimpleGeometry
    ? [{ type: geometria.getType(), coordinates: sinAltura(geometria.getCoordinates() ?? []) }]
    : [];
}

/** Los de `ExtendedData`, con el atributo `name`: OpenLayers arrastra el `displayName` de un `Data` a los siguientes,
 * que llegan como objeto con su valor. */
function metadatosDe(feature: Feature): ElementoLeido['metadatos'] {
  return Object.entries(feature.getProperties())
    .filter(([nombre]) => !SIN_INTERES.has(nombre))
    .map(([nombre, valor]) => {
      const conTitulo = typeof valor === 'object' && valor !== null ? (valor as { value?: unknown }) : null;
      return { nombre, valor: String((conTitulo ? conTitulo.value : valor) ?? '') };
    });
}

/** Sin el ícono: los de un KML están en otros servidores, y la CSP los bloquearía. */
function estiloDe(feature: Feature): EstiloElemento {
  const [estilo] = estilosDe(feature);
  const color = colorDe(estilo?.getStroke()?.getColor());
  return {
    color,
    // Sin color propio, el grosor también es el de OpenLayers.
    grosor: color ? (estilo?.getStroke()?.getWidth() ?? 0) : 0,
    relleno: colorDe(estilo?.getFill()?.getColor()),
    icono: ''
  };
}

function estilosDe(feature: Feature): Style[] {
  const resultado = feature.getStyleFunction()?.(feature, 1);
  if (Array.isArray(resultado)) {
    return resultado;
  }
  return resultado ? [resultado] : [];
}

/** Un color puede ser también un degradado o un patrón, que un KML nunca trae. */
function colorDe(color: unknown): string {
  if (typeof color !== 'string' && !Array.isArray(color)) {
    return '';
  }
  const texto = asString(color as Color | string);
  return texto === BLANCO_POR_DEFECTO ? '' : texto;
}

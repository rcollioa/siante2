import { ElementoLeido } from '../../mapa/domain/mapa';
import { ArchivoInvalido, decodificar, leerGeoJson, nombreSugerido, origenDe, revisar, tieneCarpetas } from './archivo-geografico';

describe('archivo geográfico', () => {
  const coleccion = (...features: unknown[]) => JSON.stringify({ type: 'FeatureCollection', features });
  const punto = { type: 'Point', coordinates: [-70.65, -33.44] };
  const feature = (geometry: unknown, properties: unknown = {}, extra: object = {}) => ({ type: 'Feature', geometry, properties, ...extra });

  describe('tipo de archivo', () => {
    it('reconoce las extensiones, sin importar mayúsculas', () => {
      expect(origenDe('Bodegas.KML')).toBe('KML');
      expect(origenDe('bodegas.json')).toBe('GEOJSON');
      expect(origenDe('bodegas.geojson')).toBe('GEOJSON');
      expect(origenDe('bodegas.shp')).toBeNull();
      expect(origenDe('kml')).toBeNull();
    });

    it('propone el nombre del archivo, sin extensión y sin pasar el largo', () => {
      expect(nombreSugerido('Bodegas norte.v2.kml', 100)).toBe('Bodegas norte.v2');
      expect(nombreSugerido('Bodegas.kml', 3)).toBe('Bod');
    });

    it('detecta un KML con varias carpetas', () => {
      expect(tieneCarpetas('<Folder><Placemark/></Folder>')).toBe(false);
      expect(tieneCarpetas('<Folder id="a"/><Folder>')).toBe(true);
      expect(tieneCarpetas('<FolderX/><FolderY/>')).toBe(false);
    });
  });

  describe('decodificar', () => {
    it('lee UTF-8', () => {
      expect(decodificar(new TextEncoder().encode('Ñuñoa').buffer as ArrayBuffer)).toBe('Ñuñoa');
    });

    it('si no es UTF-8, lo lee como Windows-1252', () => {
      expect(decodificar(new Uint8Array([0xd1, 0x75, 0xf1, 0x6f, 0x61]).buffer)).toBe('Ñuñoa');
    });
  });

  describe('leerGeoJson', () => {
    it('lee una colección, un elemento solo, una geometría suelta y un arreglo', () => {
      expect(leerGeoJson(coleccion(feature(punto)))).toHaveLength(1);
      expect(leerGeoJson(JSON.stringify(feature(punto)))).toHaveLength(1);
      expect(leerGeoJson(JSON.stringify(punto))).toHaveLength(1);
      expect(leerGeoJson(JSON.stringify([feature(punto), feature(punto)]))).toHaveLength(2);
    });

    it('rechaza lo que no es GeoJSON', () => {
      expect(() => leerGeoJson('no es json')).toThrow(new ArchivoInvalido('El archivo no tiene un formato válido.'));
      expect(() => leerGeoJson('3')).toThrow(ArchivoInvalido);
      expect(() => leerGeoJson('{"a":1}')).toThrow(ArchivoInvalido);
    });

    it('avisa si el archivo declara otro sistema de coordenadas', () => {
      const utm = JSON.stringify({ type: 'FeatureCollection', crs: { type: 'name', properties: { name: 'urn:ogc:def:crs:EPSG::32719' } }, features: [] });

      expect(() => leerGeoJson(utm)).toThrow('urn:ogc:def:crs:EPSG::32719');
    });

    it('acepta los nombres de WGS84', () => {
      for (const name of ['urn:ogc:def:crs:OGC:1.3:CRS84', 'EPSG:4326']) {
        expect(leerGeoJson(JSON.stringify({ type: 'FeatureCollection', crs: { properties: { name } }, features: [feature(punto)] }))).toHaveLength(1);
      }
    });

    it('en un GeoJSON cualquiera, sus propiedades son los datos del elemento', () => {
      const [leido] = leerGeoJson(coleccion(feature(punto, { NOMBRE: 'Bodega', descripcion: 'Sur', Región: 'RM', Superficie: 120, vacio: null, datos: { a: 1 } })));

      expect(leido.nombre).toBe('Bodega');
      expect(leido.descripcion).toBe('Sur');
      expect(leido.metadatos).toEqual([
        { nombre: 'Región', valor: 'RM' },
        { nombre: 'Superficie', valor: '120' },
        { nombre: 'vacio', valor: '' },
        { nombre: 'datos', valor: '{"a":1}' }
      ]);
    });

    it('usa la convención de estilo de geojson.io, sin mostrarla como datos', () => {
      const [leido] = leerGeoJson(coleccion(feature(punto, { stroke: '#ff0000', 'stroke-width': 3, fill: '#00ff00', 'fill-opacity': 0.25, 'marker-color': '#000' })));

      expect(leido.estilo).toEqual({ color: '#ff0000', grosor: 3, relleno: 'rgba(0,255,0,0.25)', icono: '' });
      expect(leido.metadatos).toEqual([]);
    });

    it('un dato que no es un color no se usa como estilo', () => {
      const [leido] = leerGeoJson(coleccion(feature(punto, { stroke: 'rojo', 'stroke-width': -1 })));

      expect(leido.estilo).toEqual({ color: '', grosor: 0, relleno: '', icono: '' });
    });

    it('lee lo que guarda SIATE: el estilo en properties y los datos en metadata', () => {
      const [leido] = leerGeoJson(
        coleccion(
          feature(punto, { color: [255, 0, 0, 1], width: 4, fill: 'rgba(1,2,3,0.5)', image: 'x.svg' }, {
            metadata: { name: 'Bodega', description: 'Sur', metadata: [{ name: 'Fuente', description: 'Terreno' }] }
          })
        )
      );

      expect(leido.nombre).toBe('Bodega');
      expect(leido.descripcion).toBe('Sur');
      expect(leido.metadatos).toEqual([{ nombre: 'Fuente', valor: 'Terreno' }]);
      expect(leido.estilo).toEqual({ color: 'rgba(255, 0, 0, 1)', grosor: 4, relleno: 'rgba(1,2,3,0.5)', icono: '' });
    });

    it('un círculo de SIATE sigue siendo un círculo', () => {
      const [leido] = leerGeoJson(coleccion(feature({ type: 'Circle', center: [-70, -33], radius: 500 })));

      expect(leido.geometria).toEqual({ type: 'Point', coordinates: [-70, -33] });
      expect(leido.radio).toBe(500);
    });

    it('una colección de geometrías da un elemento por cada una, con los mismos datos', () => {
      const leidos = leerGeoJson(coleccion(feature({ type: 'GeometryCollection', geometries: [punto, punto] }, { name: 'Doble' })));

      expect(leidos.map(({ nombre }) => nombre)).toEqual(['Doble', 'Doble']);
    });

    it('un elemento sin geometría se entrega vacío, para contarlo entre los omitidos', () => {
      expect(leerGeoJson(coleccion(feature(null), 3)).map(({ geometria }) => geometria.type)).toEqual(['', '']);
    });
  });

  describe('revisar', () => {
    const leido = (type: string, coordinates: unknown[], radio = 0): ElementoLeido => ({
      geometria: { type, coordinates },
      radio,
      nombre: '',
      descripcion: '',
      estilo: { color: '', grosor: 0, relleno: '', icono: '' },
      metadatos: []
    });
    const uno = (elemento: ElementoLeido) => revisar([elemento], 'GEOJSON', false).elementos[0];
    const omite = (elemento: ElementoLeido) => {
      const { omitidos } = revisar([leido('Point', [1, 1]), elemento], 'GEOJSON', false);
      expect(omitidos).toBe(1);
    };

    it('acepta cada tipo de geometría', () => {
      const anillo = [[0, 0], [1, 0], [1, 1], [0, 0]];
      const { elementos, omitidos } = revisar(
        [
          leido('Point', [1, 2]),
          leido('LineString', [[0, 0], [1, 1]]),
          leido('Polygon', [anillo]),
          leido('MultiPoint', [[0, 0], [1, 1]]),
          leido('MultiLineString', [[[0, 0], [1, 1]]]),
          leido('MultiPolygon', [[anillo]])
        ],
        'KML',
        true
      );

      expect(elementos).toHaveLength(6);
      expect(omitidos).toBe(0);
    });

    it('entrega IDs provisionales negativos, el origen y si tenía carpetas', () => {
      const archivo = revisar([leido('Point', [1, 2]), leido('Point', [3, 4])], 'KML', true);

      expect(archivo.elementos.map(({ id }) => id)).toEqual([-1, -2]);
      expect(archivo.origen).toBe('KML');
      expect(archivo.enCarpetas).toBe(true);
    });

    it('quita la altura en cualquier nivel', () => {
      expect(uno(leido('Point', [1, 2, 300])).geometria.coordinates).toEqual([1, 2]);
      expect(uno(leido('LineString', [[0, 0, 9], [1, 1, 9]])).geometria.coordinates).toEqual([[0, 0], [1, 1]]);
    });

    it('cierra un polígono que no viene cerrado', () => {
      expect(uno(leido('Polygon', [[[0, 0], [1, 0], [1, 1]]])).geometria.coordinates).toEqual([[[0, 0], [1, 0], [1, 1], [0, 0]]]);
    });

    it('omite lo que Oracle Spatial no aceptaría', () => {
      omite(leido('Point', [500, 2]));
      omite(leido('Point', [1, 95]));
      omite(leido('Point', [1]));
      omite(leido('Point', ['a', 2]));
      omite(leido('LineString', [[0, 0]]));
      omite(leido('Polygon', [[[0, 0], [1, 1], [0, 0]]]));
      omite(leido('Polygon', []));
      omite(leido('MultiPoint', []));
      omite(leido('MultiLineString', [[[0, 0]]]));
      omite(leido('MultiPolygon', [3]));
      omite(leido('GeometryCollection', []));
      omite(leido('', []));
      omite(leido('Point', [1, 2], -5));
      omite(leido('LineString', [[0, 0], [1, 1]], 5));
    });

    it('recorta y limpia los textos', () => {
      const limpio = uno({
        ...leido('Point', [1, 2]),
        nombre: '  Bodega ',
        descripcion: 'x'.repeat(70000),
        metadatos: [
          { nombre: ' Fuente ', valor: ' Terreno ' },
          { nombre: '  ', valor: 'sin nombre' }
        ]
      });

      expect(limpio.nombre).toBe('Bodega');
      expect(limpio.descripcion).toHaveLength(65536);
      expect(limpio.metadatos).toEqual([{ nombre: 'Fuente', valor: 'Terreno' }]);
    });

    it('sin elementos dice que no tiene geometrías', () => {
      expect(() => revisar([], 'KML', false)).toThrow(new ArchivoInvalido('El archivo no tiene geometrías.'));
    });

    it('si ninguno es válido, explica qué coordenadas espera', () => {
      expect(() => revisar([leido('Point', [350000, 6300000])], 'GEOJSON', false)).toThrow('WGS84');
    });

    it('rechaza más elementos que el máximo', () => {
      const demasiados = Array.from({ length: 10001 }, () => leido('Point', [1, 2]));

      expect(() => revisar(demasiados, 'GEOJSON', false)).toThrow('El archivo tiene 10001 elementos, y el máximo es 10000.');
    });

    it('rechaza un contenido demasiado grande para enviarlo', () => {
      const enorme = { ...leido('Point', [1, 2]), metadatos: [{ nombre: 'a', valor: 'x'.repeat(65536) }] };

      expect(() => revisar(Array.from({ length: 400 }, () => enorme), 'GEOJSON', false)).toThrow('demasiado grande');
    });
  });
});

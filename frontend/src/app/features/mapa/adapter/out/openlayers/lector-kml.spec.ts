import { leerKml } from './lector-kml';

describe('leerKml', () => {
  const kml = (contenido: string, atributos = '') => `<?xml version="1.0" encoding="UTF-8"?>
    <kml xmlns="http://www.opengis.net/kml/2.2"${atributos}><Document>${contenido}</Document></kml>`;
  const placemark = (geometria: string, resto = '') => `<Placemark>${resto}${geometria}</Placemark>`;
  const punto = '<Point><coordinates>-70.65,-33.44,120</coordinates></Point>';
  const linea = '<LineString><coordinates>-70.6,-33.4,0 -70.5,-33.3,0</coordinates></LineString>';
  const poligono = `<Polygon><outerBoundaryIs><LinearRing>
      <coordinates>-70.7,-33.5,0 -70.6,-33.5,0 -70.6,-33.4,0 -70.7,-33.5,0</coordinates>
    </LinearRing></outerBoundaryIs></Polygon>`;

  it('lee cada geometría en grados y sin altura', () => {
    const leidos = leerKml(kml(placemark(punto) + placemark(linea) + placemark(poligono)));

    expect(leidos.map(({ geometria }) => geometria.type)).toEqual(['Point', 'LineString', 'Polygon']);
    expect(leidos[0].geometria.coordinates).toEqual([-70.65, -33.44]);
    expect(leidos[1].geometria.coordinates).toEqual([[-70.6, -33.4], [-70.5, -33.3]]);
    expect(leidos[2].geometria.coordinates).toEqual([[[-70.7, -33.5], [-70.6, -33.5], [-70.6, -33.4], [-70.7, -33.5]]]);
    expect(leidos.every(({ radio }) => radio === 0)).toBe(true);
  });

  it('lee el nombre, la descripción y los datos, sin lo que no interesa', () => {
    const [leido] = leerKml(
      kml(
        placemark(
          punto,
          `<name>Bodega</name><description>Junto al río</description><visibility>1</visibility><styleUrl>#x</styleUrl>
           <ExtendedData>
             <Data name="region"><displayName>Región</displayName><value>RM</value></Data>
             <Data name="superficie"><value>120</value></Data>
           </ExtendedData>`
        )
      )
    );

    expect(leido.nombre).toBe('Bodega');
    expect(leido.descripcion).toBe('Junto al río');
    expect(leido.metadatos).toEqual([
      { nombre: 'region', valor: 'RM' },
      { nombre: 'superficie', valor: '120' }
    ]);
  });

  it('sin nombre ni descripción quedan vacíos', () => {
    const [leido] = leerKml(kml(placemark(punto)));

    expect(leido.nombre).toBe('');
    expect(leido.descripcion).toBe('');
  });

  it('usa el color de la línea y del fondo que define el KML', () => {
    const [leido] = leerKml(
      kml(`<Style id="a"><LineStyle><color>ff0000ff</color><width>3</width></LineStyle><PolyStyle><color>8000ff00</color></PolyStyle></Style>` +
        placemark(poligono, '<styleUrl>#a</styleUrl>'))
    );

    expect(leido.estilo.color).toBe('rgba(255,0,0,1)');
    expect(leido.estilo.grosor).toBe(3);
    expect(leido.estilo.relleno).toMatch(/^rgba\(0,255,0,0\.50\d*\)$/);
  });

  it('sin estilo definido usa el de la aplicación: el blanco opaco de OpenLayers taparía el mapa', () => {
    const [leido] = leerKml(kml(placemark(poligono)));

    expect(leido.estilo.color).toBe('');
    expect(leido.estilo.grosor).toBe(0);
    expect(leido.estilo.relleno).toBe('');
  });

  it('nunca toma el ícono: los de un KML están en otros servidores', () => {
    const [leido] = leerKml(
      kml(`<Style id="a"><IconStyle><Icon><href>https://maps.example.com/pin.png</href></Icon></IconStyle></Style>` +
        placemark(punto, '<styleUrl>#a</styleUrl>'))
    );

    expect(leido.estilo.icono).toBe('');
  });

  it('un MultiGeometry del mismo tipo es una sola geometría múltiple', () => {
    const leidos = leerKml(kml(placemark(`<MultiGeometry>${punto}${punto}</MultiGeometry>`)));

    expect(leidos.map(({ geometria }) => geometria.type)).toEqual(['MultiPoint']);
  });

  it('un MultiGeometry de varios tipos da un elemento por cada parte, con los mismos datos', () => {
    const leidos = leerKml(kml(placemark(`<MultiGeometry>${punto}${linea}</MultiGeometry>`, '<name>Doble</name>')));

    expect(leidos.map(({ geometria }) => geometria.type)).toEqual(['Point', 'LineString']);
    expect(leidos.map(({ nombre }) => nombre)).toEqual(['Doble', 'Doble']);
  });

  it('lee los elementos de todas las carpetas', () => {
    const leidos = leerKml(kml(`<Folder>${placemark(punto)}</Folder><Folder>${placemark(linea)}</Folder>`));

    expect(leidos).toHaveLength(2);
  });

  it('un Placemark sin geometría se entrega vacío, para contarlo entre los omitidos', () => {
    expect(leerKml(kml('<Placemark><name>Solo nombre</name></Placemark>')).map(({ geometria }) => geometria.type)).toEqual(['']);
  });

  it('acepta un KML que declara xsi:schemaLocation sin definir xsi', () => {
    const leidos = leerKml(kml(placemark(punto), ' xsi:schemaLocation="http://www.opengis.net/kml/2.2 http://schemas.opengis.net/kml/2.2.0/ogckml22.xsd"'));

    expect(leidos).toHaveLength(1);
  });

  it('un archivo que no es KML no tiene elementos', () => {
    expect(leerKml('esto no es xml')).toEqual([]);
    expect(leerKml('<html><body></body></html>')).toEqual([]);
  });
});

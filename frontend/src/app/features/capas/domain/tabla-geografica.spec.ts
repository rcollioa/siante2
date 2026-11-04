import { ArchivoInvalido } from './archivo-geografico';
import { armarTabla, columnasSugeridas, convertirTabla, leerCsv, TablaLeida, tipoSugerido } from './tabla-geografica';

describe('tabla geográfica', () => {
  const tabla = (titulos: string[], ...filas: string[][]): TablaLeida => ({ origen: 'CSV', titulos, filas });

  describe('leerCsv', () => {
    it('usa el separador de la primera línea: Excel en español usa punto y coma', () => {
      expect(leerCsv('a;b\n1,5;2')).toEqual([['a', 'b'], ['1,5', '2']]);
      expect(leerCsv('a,b\n1;5,2')).toEqual([['a', 'b'], ['1;5', '2']]);
      expect(leerCsv('a\tb\n1\t2')).toEqual([['a', 'b'], ['1', '2']]);
    });

    it('respeta comillas con separadores, saltos y comillas dobles, sin BOM ni fin de línea de Windows', () => {
      expect(leerCsv('\uFEFFa,b\r\n"x, ""y""\nz",2\r\n')).toEqual([['a', 'b'], ['x, "y"\nz', '2'], ['']]);
    });
  });

  describe('armarTabla', () => {
    it('separa los títulos, omite filas vacías y completa las cortas', () => {
      expect(armarTabla('CSV', [[' a ', 'b'], ['', ' '], ['1']])).toEqual({ origen: 'CSV', titulos: ['a', 'b'], filas: [['1', '']] });
    });

    it('sin datos no se puede importar', () => {
      expect(() => armarTabla('XLSX', [['a', 'b']])).toThrow(ArchivoInvalido);
    });
  });

  describe('sugerencias', () => {
    it('reconoce las columnas por sus títulos', () => {
      const conLatitud = tabla(['Nombre', 'LONGITUD', 'Latitud']);

      expect(tipoSugerido(conLatitud)).toBe('GEODESICAS');
      expect(columnasSugeridas(conLatitud, 'GEODESICAS')).toEqual([2, 1]);
    });

    it('reconoce UTM y cuadrícula militar', () => {
      expect(tipoSugerido(tabla(['Zona', 'Este', 'Norte']))).toBe('UTM');
      expect(tipoSugerido(tabla(['Sitio', 'MGRS']))).toBe('MGRS');
    });

    it('lo que no reconoce queda para que lo elija el usuario', () => {
      const sinPistas = tabla(['A', 'B']);

      expect(tipoSugerido(sinPistas)).toBe('GEODESICAS');
      expect(columnasSugeridas(sinPistas, 'GEODESICAS')).toEqual([-1, -1]);
    });
  });

  describe('convertirTabla', () => {
    const datos = tabla(['Sitio', 'Lat', 'Lon', 'Tipo'], ['Bodega', '-33,44', '-70.65', 'A'], ['Mala', 'x', '-70', 'B']);

    it('cada fila es un marcador, con nombre y las demás columnas como metadatos, y avisa las omitidas', () => {
      const leido = convertirTabla(datos, 'GEODESICAS', [1, 2]);

      expect(leido.origen).toBe('CSV');
      expect(leido.omitidos).toBe(1);
      expect(leido.elementos).toHaveLength(1);
      expect(leido.elementos[0]).toMatchObject({
        geometria: { type: 'Point', coordinates: [-70.65, -33.44] },
        radio: 0,
        nombre: 'Bodega'
      });
      expect(leido.elementos[0].metadatos).toEqual([
        { nombre: 'Lat', valor: '-33,44' },
        { nombre: 'Lon', valor: '-70.65' },
        { nombre: 'Tipo', valor: 'A' }
      ]);
    });

    it('prefiere la columna titulada nombre, aunque no sea la primera', () => {
      const leido = convertirTabla(tabla(['Tipo', 'Nombre', 'Lat', 'Lon'], ['A', 'Bodega', '-33', '-70']), 'GEODESICAS', [2, 3]);

      expect(leido.elementos[0].nombre).toBe('Bodega');
    });

    it('fuera del mundo no es una ubicación válida, como una latitud y longitud invertidas', () => {
      expect(convertirTabla(tabla(['N', 'Lat', 'Lon'], ['A', '-120', '-33'], ['B', '-33', '-70']), 'GEODESICAS', [1, 2]).omitidos).toBe(1);
    });

    it('sin ninguna fila válida lo informa', () => {
      expect(() => convertirTabla(tabla(['N', 'Lat', 'Lon'], ['A', 'x', 'y']), 'GEODESICAS', [1, 2])).toThrow(ArchivoInvalido);
    });

    it('sin columna elegida, ninguna fila tiene ubicación', () => {
      expect(() => convertirTabla(tabla(['N', 'Lat', 'Lon'], ['A', '-33', '-70']), 'GEODESICAS', [-1, 2])).toThrow(ArchivoInvalido);
    });

    it('convierte cada tipo de ubicación', () => {
      const de = (titulos: string[], fila: string[], tipo: Parameters<typeof convertirTabla>[1], columnas: number[]) =>
        convertirTabla(tabla(titulos, fila), tipo, columnas).elementos[0].geometria.coordinates as number[];

      expect(de(['N', 'Z', 'E', 'S'], ['A', '56H', '334873', '6252266'], 'UTM', [1, 2, 3])[1]).toBeCloseTo(-33.8568, 3);
      expect(de(['N', 'Lat', 'Lon'], ['A', '33 30 0 S', '70 15 0 O'], 'GMS', [1, 2])).toEqual([-70.25, -33.5]);
      expect(de(['N', 'Lat', 'Lon'], ['A', '33 30 S', '70 15 O'], 'GMD', [1, 2])).toEqual([-70.25, -33.5]);
      expect(de(['N', 'M'], ['A', '19HCC4662598715'], 'MGRS', [1])[0]).toBeCloseTo(-70.65, 3);
    });
  });
});

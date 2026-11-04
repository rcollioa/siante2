import { TestBed } from '@angular/core/testing';
import { MapaPort } from '../../mapa/application/port/out/mapa.port';
import { ElementoLeido } from '../../mapa/domain/mapa';
import { ArchivoInvalido } from '../domain/archivo-geografico';
import { LectorExcel } from './port/out/lector-excel.port';
import { LectorArchivos } from './lector-archivos';

describe('LectorArchivos', () => {
  const leerKml = vi.fn<(texto: string) => Promise<ElementoLeido[]>>();
  const leerExcel = vi.fn<(contenido: ArrayBuffer) => Promise<string[][]>>();
  const punto: ElementoLeido = {
    geometria: { type: 'Point', coordinates: [-70.65, -33.44] },
    radio: 0,
    nombre: 'Bodega',
    descripcion: '',
    estilo: { color: '', grosor: 0, relleno: '', icono: '' },
    metadatos: []
  };
  let lector: LectorArchivos;

  /** jsdom no implementa `arrayBuffer` en todas sus versiones: se simula solo lo que usa el lector. */
  const archivo = (nombre: string, contenido: string, tamano = contenido.length) =>
    ({ name: nombre, size: tamano, arrayBuffer: async () => new TextEncoder().encode(contenido).buffer }) as unknown as File;

  beforeEach(() => {
    leerKml.mockReset();
    leerKml.mockResolvedValue([punto]);
    leerExcel.mockReset();
    TestBed.configureTestingModule({
      providers: [
        { provide: MapaPort, useValue: { leerKml } },
        { provide: LectorExcel, useValue: { leer: leerExcel } }
      ]
    });
    lector = TestBed.inject(LectorArchivos);
  });

  describe('tablas', () => {
    it('lee un CSV sin pasar por la librería de Excel', async () => {
      const tabla = await lector.leerTabla(archivo('sitios.CSV', 'Nombre;Lat;Lon\nBodega;-33;-70'));

      expect(tabla).toEqual({ origen: 'CSV', titulos: ['Nombre', 'Lat', 'Lon'], filas: [['Bodega', '-33', '-70']] });
      expect(leerExcel).not.toHaveBeenCalled();
    });

    it('un Excel lo lee su adaptador', async () => {
      leerExcel.mockResolvedValue([['Nombre', 'Lat'], ['Bodega', '-33']]);

      const tabla = await lector.leerTabla(archivo('sitios.xlsx', 'contenido'));

      expect(tabla.origen).toBe('XLSX');
      expect(tabla.filas).toEqual([['Bodega', '-33']]);
    });

    it('un Excel dañado se informa como archivo inválido', async () => {
      leerExcel.mockRejectedValue(new Error('zip inválido'));

      await expect(lector.leerTabla(archivo('sitios.xlsx', 'x'))).rejects.toThrow('El archivo Excel no se pudo leer.');
    });

    it('un Excel sin datos dice qué le falta', async () => {
      leerExcel.mockResolvedValue([['Nombre']]);

      await expect(lector.leerTabla(archivo('sitios.xlsx', 'x'))).rejects.toThrow('fila de títulos');
    });

    it('rechaza un archivo demasiado grande sin leerlo', async () => {
      await expect(lector.leerTabla(archivo('sitios.csv', 'a', 31 * 1024 * 1024))).rejects.toThrow('30 MB');
    });
  });

  it('a un .xls antiguo le indica que lo guarde como .xlsx', async () => {
    await expect(lector.leer(archivo('sitios.xls', 'x'))).rejects.toThrow('guárdelo como .xlsx');
  });

  it('lee un GeoJSON sin pasar por el mapa', async () => {
    const leido = await lector.leer(archivo('bodegas.geojson', '{"type":"Point","coordinates":[-70,-33,5]}'));

    expect(leido.origen).toBe('GEOJSON');
    expect(leido.elementos[0].geometria.coordinates).toEqual([-70, -33]);
    expect(leerKml).not.toHaveBeenCalled();
  });

  it('un KML lo lee la librería del mapa y avisa si tiene carpetas', async () => {
    const kml = '<kml><Folder></Folder><Folder></Folder></kml>';

    const leido = await lector.leer(archivo('bodegas.KML', kml));

    expect(leerKml).toHaveBeenCalledWith(kml);
    expect(leido.origen).toBe('KML');
    expect(leido.enCarpetas).toBe(true);
    expect(leido.elementos[0].nombre).toBe('Bodega');
  });

  it('un KML que no se pudo leer no tiene geometrías', async () => {
    leerKml.mockResolvedValue([]);

    await expect(lector.leer(archivo('dañado.kml', 'nada'))).rejects.toThrow(new ArchivoInvalido('El archivo no tiene geometrías.'));
  });

  it('rechaza otro formato sin leerlo', async () => {
    const leerContenido = vi.fn();

    await expect(lector.leer({ name: 'bodegas.shp', size: 1, arrayBuffer: leerContenido } as unknown as File)).rejects.toThrow(
      'El formato del archivo no está disponible'
    );
    expect(leerContenido).not.toHaveBeenCalled();
  });

  it('rechaza un archivo de más de 30 MB sin leerlo', async () => {
    const leerContenido = vi.fn();

    await expect(lector.leer({ name: 'bodegas.kml', size: 30 * 1024 * 1024 + 1, arrayBuffer: leerContenido } as unknown as File)).rejects.toThrow(
      'El archivo excede el tamaño máximo de 30 MB.'
    );
    expect(leerContenido).not.toHaveBeenCalled();
  });
});

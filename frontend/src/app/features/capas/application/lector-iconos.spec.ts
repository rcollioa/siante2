import { IconoInvalido } from '../domain/icono';
import { LectorIconos } from './lector-iconos';

describe('LectorIconos', () => {
  const lector = new LectorIconos();
  let medidas: { naturalWidth: number; naturalHeight: number } | null;

  /** jsdom no decodifica imágenes: el doble responde con las medidas de cada prueba, o con un error. */
  class ImagenDePrueba {
    naturalWidth = 0;
    naturalHeight = 0;
    onload: () => void = () => undefined;
    onerror: () => void = () => undefined;
    set src(_: string) {
      queueMicrotask(() => {
        if (medidas) {
          Object.assign(this, medidas);
          this.onload();
        } else {
          this.onerror();
        }
      });
    }
  }

  const archivo = (nombre: string, contenido = 'abc') =>
    ({ name: nombre, arrayBuffer: async () => new TextEncoder().encode(contenido).buffer }) as unknown as File;

  beforeEach(() => {
    medidas = { naturalWidth: 32, naturalHeight: 32 };
    vi.stubGlobal('Image', ImagenDePrueba);
  });

  afterEach(() => vi.unstubAllGlobals());

  it('entrega un PNG como URL data: con el nombre del archivo', async () => {
    await expect(lector.leer(archivo('bodega.PNG'))).resolves.toEqual({
      archivo: 'bodega.PNG',
      imagen: 'data:image/png;base64,YWJj'
    });
  });

  it('el tipo de un SVG sale de su extensión', async () => {
    const { imagen } = await lector.leer(archivo('bodega.svg'));

    expect(imagen.startsWith('data:image/svg+xml;base64,')).toBe(true);
  });

  it('acepta el tamaño máximo de SIATE', async () => {
    medidas = { naturalWidth: 66, naturalHeight: 88 };

    await expect(lector.leer(archivo('a.png'))).resolves.toBeDefined();
  });

  it.each([
    [67, 10],
    [10, 89]
  ])('rechaza %i x %i: basta con exceder una medida', async (naturalWidth, naturalHeight) => {
    medidas = { naturalWidth, naturalHeight };

    await expect(lector.leer(archivo('a.png'))).rejects.toThrow('no deben superar el ancho 66 y el alto 88');
  });

  it('rechaza otros formatos sin leerlos', async () => {
    await expect(lector.leer(archivo('a.jpg'))).rejects.toThrow('permitidos son SVG y PNG');
    await expect(lector.leer(archivo('sin-extension'))).rejects.toBeInstanceOf(IconoInvalido);
  });

  it('rechaza un archivo que no cabe como ícono', async () => {
    await expect(lector.leer(archivo('a.png', 'a'.repeat(24000)))).rejects.toThrow('demasiado grande');
  });

  it('rechaza una imagen que el navegador no puede leer', async () => {
    medidas = null;

    await expect(lector.leer(archivo('a.png'))).rejects.toThrow('problemas de lectura');
  });
});

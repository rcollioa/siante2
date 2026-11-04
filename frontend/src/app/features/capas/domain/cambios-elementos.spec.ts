import {
  aHexadecimal,
  aplicar,
  cantidadDe,
  conCambio,
  conNuevo,
  elementoNuevo,
  limpios,
  rellenoDe,
  sinElemento,
  SIN_CAMBIOS
} from './cambios-elementos';
import { ElementoCapa } from './capa';

describe('cambios de elementos', () => {
  const forma = { geometria: { type: 'Point', coordinates: [-70.65, -33.44] }, radio: 0 };
  const elemento = (id: number, nombre = ''): ElementoCapa => ({ ...elementoNuevo(id, forma), nombre });

  it('un elemento nuevo parte sin datos, con el estilo de la aplicación', () => {
    expect(elementoNuevo(-1, forma)).toEqual({
      ...forma,
      id: -1,
      nombre: '',
      descripcion: '',
      estilo: { color: '', grosor: 0, relleno: '', icono: '' },
      metadatos: []
    });
  });

  it('cambiar uno nuevo lo reemplaza entre los nuevos', () => {
    const cambios = conCambio(conNuevo(SIN_CAMBIOS, elemento(-1)), elemento(-1, 'Bodega'));

    expect(cambios.nuevos).toEqual([elemento(-1, 'Bodega')]);
    expect(cambios.modificados).toEqual([]);
  });

  it('cambiar dos veces uno guardado lo deja una sola vez, con lo último', () => {
    const cambios = conCambio(conCambio(SIN_CAMBIOS, elemento(3, 'A')), elemento(3, 'B'));

    expect(cambios.modificados).toEqual([elemento(3, 'B')]);
    expect(cantidadDe(cambios)).toBe(1);
  });

  it('borrar uno nuevo no deja rastro', () => {
    expect(sinElemento(conNuevo(SIN_CAMBIOS, elemento(-1)), elemento(-1))).toEqual(SIN_CAMBIOS);
  });

  it('borrar uno guardado descarta lo que se le había cambiado', () => {
    const cambios = sinElemento(conCambio(SIN_CAMBIOS, elemento(3, 'A')), elemento(3));

    expect(cambios.modificados).toEqual([]);
    expect(cambios.eliminados).toEqual([3]);
  });

  it('al enviar quita los espacios a los lados y los metadatos sin nombre', () => {
    const conEspacios: ElementoCapa = {
      ...elemento(3, ' Bodega '),
      descripcion: ' Sur ',
      metadatos: [
        { nombre: ' Fuente ', valor: ' Terreno ' },
        { nombre: '  ', valor: 'sin nombre' }
      ]
    };

    const [limpio] = limpios(conCambio(SIN_CAMBIOS, conEspacios)).modificados;

    expect(limpio.nombre).toBe('Bodega');
    expect(limpio.descripcion).toBe('Sur');
    expect(limpio.metadatos).toEqual([{ nombre: 'Fuente', valor: 'Terreno' }]);
  });

  it('aplicar deja la capa como quedó guardada, con los ID de la base', () => {
    const cambios = { nuevos: [elemento(-1, 'Nuevo')], modificados: [elemento(2, 'Cambiado')], eliminados: [1] };

    const elementos = aplicar([elemento(1, 'Borrado'), elemento(2, 'Original'), elemento(4, 'Igual')], cambios, [9]);

    expect(elementos.map(({ id, nombre }) => [id, nombre])).toEqual([
      [2, 'Cambiado'],
      [4, 'Igual'],
      [9, 'Nuevo']
    ]);
  });
});

describe('colores', () => {
  it('convierte a #rrggbb los que guarda SIATE', () => {
    expect(aHexadecimal('#71AECB')).toBe('#71aecb');
    expect(aHexadecimal('#FFF')).toBe('#ffffff');
    expect(aHexadecimal('rgba(230, 248, 253, 0.35)')).toBe('#e6f8fd');
    expect(aHexadecimal('rgb(1,2,3)')).toBe('#010203');
  });

  it('un color que no se puede convertir queda vacío', () => {
    expect(aHexadecimal('red')).toBe('');
  });

  it('el fondo elegido queda semitransparente, como en SIATE', () => {
    expect(rellenoDe('#ff0080')).toBe('rgba(255,0,128,0.5)');
  });
});

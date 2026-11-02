import { EdicionCapa, erroresDeEdicion, ResumenCapa, sePuedeGestionar, Visibilidad } from './capa';

describe('reglas de edición de una capa', () => {
  const valida: EdicionCapa = { nombre: 'Bodegas', descripcion: '', categoriaId: 3, visibilidad: 'PRIVADA' };
  const errores = (cambios: Partial<EdicionCapa>) => erroresDeEdicion({ ...valida, ...cambios });

  it('una edición válida no tiene errores', () => {
    expect(errores({})).toEqual({ nombre: '', descripcion: '', categoria: '' });
  });

  it('exige un nombre de 4 a 100 caracteres, sin contar los espacios a los lados', () => {
    expect(errores({ nombre: '' }).nombre).toBe('Nombre requerido.');
    expect(errores({ nombre: '     ' }).nombre).toBe('Nombre requerido.');
    expect(errores({ nombre: ' abc ' }).nombre).toBe('El nombre debe tener al menos 4 caracteres.');
    expect(errores({ nombre: 'abcd' }).nombre).toBe('');
    expect(errores({ nombre: 'a'.repeat(100) }).nombre).toBe('');
    expect(errores({ nombre: 'a'.repeat(101) }).nombre).toBe('El nombre no puede tener más de 100 caracteres.');
  });

  it('limita la descripción a 200 caracteres', () => {
    expect(errores({ descripcion: 'a'.repeat(200) }).descripcion).toBe('');
    expect(errores({ descripcion: 'a'.repeat(201) }).descripcion).toBe('La descripción no puede tener más de 200 caracteres.');
  });

  it('exige una categoría', () => {
    expect(errores({ categoriaId: 0 }).categoria).toBe('Categoría requerida.');
  });

  it('solo se gestionan las capas privadas o públicas', () => {
    const capa = (visibilidad: Visibilidad): ResumenCapa => ({ id: 1, nombre: '', descripcion: '', visibilidad });

    expect(sePuedeGestionar(capa('PRIVADA'))).toBe(true);
    expect(sePuedeGestionar(capa('PUBLICA'))).toBe(true);
    expect(sePuedeGestionar(capa('FORMAL'))).toBe(false);
    expect(sePuedeGestionar(capa('AREA_DE_DIBUJO'))).toBe(false);
  });
});

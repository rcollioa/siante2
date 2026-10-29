import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { CapaCatalogo, SeccionCatalogo } from '../domain/capa';
import { Catalogo } from './catalogo';
import { CapasRepositorio } from './port/out/capas.repositorio';

describe('Catalogo', () => {
  const capa = (id: number, nombre: string, cambios: Partial<CapaCatalogo> = {}): CapaCatalogo => ({
    id,
    nombre,
    descripcion: '',
    visibilidad: 'PUBLICA',
    categoria: 'Agua',
    dueno: 'Ana Pérez',
    favorita: false,
    ...cambios
  });
  let catalogo: ReturnType<typeof vi.fn<(seccion: SeccionCatalogo) => Observable<CapaCatalogo[]>>>;
  let servicio: Catalogo;

  beforeEach(() => {
    catalogo = vi.fn(() => of([capa(1, 'Ríos'), capa(2, 'Hospitales', { categoria: 'Salud', dueno: 'SIANTE' })]));
    TestBed.configureTestingModule({ providers: [{ provide: CapasRepositorio, useValue: { catalogo } }] });
    servicio = TestBed.inject(Catalogo);
  });

  it('abre en la pestaña pedida y la descarga', () => {
    servicio.abrir('FORMALES');

    expect(catalogo).toHaveBeenCalledWith('FORMALES');
    expect(servicio.seccion()).toBe('FORMALES');
    expect(servicio.estado()).toBe('listo');
    expect(servicio.total()).toBe(2);
  });

  it('busca por nombre, categoría o dueño sin importar tildes ni mayúsculas', () => {
    servicio.abrir('PUBLICAS');

    servicio.buscar('RIOS');
    expect(servicio.capas().map(({ nombre }) => nombre)).toEqual(['Ríos']);

    servicio.buscar('salud');
    expect(servicio.capas().map(({ nombre }) => nombre)).toEqual(['Hospitales']);

    servicio.buscar('siante');
    expect(servicio.capas().map(({ nombre }) => nombre)).toEqual(['Hospitales']);
    expect(servicio.total()).toBe(2);
  });

  it('cambiar de pestaña mantiene la búsqueda; abrir de nuevo la limpia', () => {
    servicio.abrir('PUBLICAS');
    servicio.buscar('ríos');

    servicio.mostrar('MIAS');
    expect(servicio.texto()).toBe('ríos');
    expect(catalogo).toHaveBeenLastCalledWith('MIAS');

    servicio.abrir('PUBLICAS');
    expect(servicio.texto()).toBe('');
  });

  it('mientras llega no muestra la pestaña anterior', () => {
    servicio.abrir('PUBLICAS');
    const respuesta = new Subject<CapaCatalogo[]>();
    catalogo.mockReturnValue(respuesta);

    servicio.mostrar('MIAS');

    expect(servicio.estado()).toBe('cargando');
    expect(servicio.total()).toBe(0);
  });

  it('si falla lo indica', () => {
    catalogo.mockReturnValue(throwError(() => new Error('sin conexión')));

    servicio.abrir('PUBLICAS');

    expect(servicio.estado()).toBe('error');
  });
});

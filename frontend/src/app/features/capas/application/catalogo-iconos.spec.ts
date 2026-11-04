import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { DatosIcono, Icono } from '../domain/icono';
import { CatalogoIconos } from './catalogo-iconos';
import { IconosRepositorio } from './port/out/iconos.repositorio';

describe('CatalogoIconos', () => {
  const sistema: Icono = { id: 1, nombre: 'Sistema', archivo: '', imagen: 'data:image/png;base64,AA==', propio: false };
  const mio: Icono = { id: 5, nombre: 'Mío', archivo: 'mio.png', imagen: 'data:image/png;base64,BB==', propio: true };
  const repositorio = {
    listar: vi.fn<() => Observable<Icono[]>>(),
    crear: vi.fn<(datos: DatosIcono) => Observable<Icono>>(),
    editar: vi.fn<(id: number, datos: DatosIcono) => Observable<void>>(),
    eliminar: vi.fn<(id: number) => Observable<void>>()
  };
  let catalogo: CatalogoIconos;

  beforeEach(() => {
    vi.resetAllMocks();
    repositorio.listar.mockReturnValue(of([sistema, mio]));
    TestBed.configureTestingModule({ providers: [{ provide: IconosRepositorio, useValue: repositorio }] });
    catalogo = TestBed.inject(CatalogoIconos);
  });

  it('separa los del sistema de los propios', () => {
    catalogo.cargar();

    expect(catalogo.estado()).toBe('listo');
    expect(catalogo.delSistema()).toEqual([sistema]);
    expect(catalogo.propios()).toEqual([mio]);
  });

  it('ya cargado o en camino no vuelve a pedirlo', () => {
    const respuesta = new Subject<Icono[]>();
    repositorio.listar.mockReturnValue(respuesta);

    catalogo.cargar();
    catalogo.cargar();
    expect(catalogo.estado()).toBe('cargando');
    respuesta.next([]);
    catalogo.cargar();

    expect(repositorio.listar).toHaveBeenCalledOnce();
  });

  it('si falla lo informa, y se puede volver a intentar', () => {
    repositorio.listar.mockReturnValueOnce(throwError(() => new Error('sin conexión')));

    catalogo.cargar();
    expect(catalogo.estado()).toBe('error');

    catalogo.cargar();
    expect(catalogo.estado()).toBe('listo');
  });

  it('crear guarda sin espacios a los lados y lo agrega a los propios', () => {
    const nuevo: Icono = { ...mio, id: 6, nombre: 'Nuevo' };
    repositorio.crear.mockReturnValue(of(nuevo));
    catalogo.cargar();

    catalogo.crear({ nombre: ' Nuevo ', archivo: 'n.png', imagen: mio.imagen }).subscribe();

    expect(repositorio.crear).toHaveBeenCalledWith({ nombre: 'Nuevo', archivo: 'n.png', imagen: mio.imagen });
    expect(catalogo.propios()).toEqual([mio, nuevo]);
  });

  it('editar cambia solo ese ícono', () => {
    repositorio.editar.mockReturnValue(of(undefined));
    catalogo.cargar();

    catalogo.editar(5, { nombre: ' Otro ', archivo: 'o.png', imagen: sistema.imagen }).subscribe();

    expect(catalogo.propios()).toEqual([{ ...mio, nombre: 'Otro', archivo: 'o.png', imagen: sistema.imagen }]);
    expect(catalogo.delSistema()).toEqual([sistema]);
  });

  it('eliminar lo quita de la lista; si falla, queda', () => {
    repositorio.eliminar.mockReturnValueOnce(throwError(() => new Error('sin conexión')));
    catalogo.cargar();
    catalogo.eliminar(5).subscribe({ error: () => undefined });
    expect(catalogo.propios()).toEqual([mio]);

    repositorio.eliminar.mockReturnValue(of(undefined));
    catalogo.eliminar(5).subscribe();
    expect(catalogo.propios()).toEqual([]);
  });
});

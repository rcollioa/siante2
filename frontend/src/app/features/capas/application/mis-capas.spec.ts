import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { CapaNoEncontrada, CapaPropia, EdicionCapa, NuevaCapa, ElementoCapa } from '../domain/capa';
import { CapasAbiertas } from './capas-abiertas';
import { Favoritas } from './favoritas';
import { mensajeDeEscritura, MisCapas } from './mis-capas';
import { CapasRepositorio } from './port/out/capas.repositorio';

describe('MisCapas', () => {
  const bodegas: CapaPropia = { id: 1, nombre: 'Bodegas', descripcion: '', visibilidad: 'PRIVADA', categoriaId: 3 };
  const zona: CapaPropia = { id: 2, nombre: 'Zona norte', descripcion: '', visibilidad: 'PUBLICA', categoriaId: 4 };
  const edicion: EdicionCapa = { nombre: 'Árboles', descripcion: 'Parques', categoriaId: 9, visibilidad: 'PUBLICA' };
  let listarMias: ReturnType<typeof vi.fn<() => Observable<CapaPropia[]>>>;
  const repositorio = {
    crear: vi.fn<(nueva: NuevaCapa) => Observable<CapaPropia>>(),
    crearPorUrl: vi.fn<(nueva: NuevaCapa, url: string) => Observable<CapaPropia>>(),
    importar: vi.fn<(nueva: NuevaCapa, origen: string, elementos: readonly ElementoCapa[]) => Observable<CapaPropia>>(),
    editar: vi.fn<(id: number, edicion: EdicionCapa) => Observable<void>>(),
    cambiarVisibilidad: vi.fn<(id: number, visibilidad: string) => Observable<void>>(),
    eliminar: vi.fn<(id: number) => Observable<void>>()
  };
  const capasAbiertas = { actualizar: vi.fn(), cerrar: vi.fn() };
  const favoritas = { actualizar: vi.fn(), quitar: vi.fn() };
  let servicio: MisCapas;

  const nombres = () => servicio.capas().map(({ nombre }) => nombre);

  beforeEach(() => {
    vi.clearAllMocks();
    listarMias = vi.fn(() => of([bodegas, zona]));
    repositorio.editar.mockReturnValue(of(undefined));
    repositorio.cambiarVisibilidad.mockReturnValue(of(undefined));
    repositorio.eliminar.mockReturnValue(of(undefined));
    TestBed.configureTestingModule({
      providers: [
        { provide: CapasRepositorio, useValue: { listarMias, ...repositorio } },
        { provide: CapasAbiertas, useValue: capasAbiertas },
        { provide: Favoritas, useValue: favoritas }
      ]
    });
    servicio = TestBed.inject(MisCapas);
  });

  it('carga las capas del usuario', () => {
    servicio.cargar();

    expect(servicio.estado()).toBe('listo');
    expect(servicio.capas()).toEqual([bodegas, zona]);
  });

  it('indica que está cargando mientras llegan', () => {
    const respuesta = new Subject<CapaPropia[]>();
    listarMias.mockReturnValue(respuesta);

    servicio.cargar();
    expect(servicio.estado()).toBe('cargando');

    respuesta.next([]);
    expect(servicio.estado()).toBe('listo');
  });

  it('si falla lo indica, y al reintentar vuelve a pedirlas', () => {
    listarMias.mockReturnValue(throwError(() => new Error('sin conexión')));
    servicio.cargar();
    expect(servicio.estado()).toBe('error');

    listarMias.mockReturnValue(of([bodegas]));
    servicio.cargar();
    expect(servicio.estado()).toBe('listo');
    expect(listarMias).toHaveBeenCalledTimes(2);
  });

  describe('escrituras', () => {
    it('importar envía los datos sin espacios a los lados y la capa entra a la lista en su lugar', () => {
      const importada: CapaPropia = { id: 50, nombre: 'Árboles', descripcion: 'Parques', visibilidad: 'PRIVADA', categoriaId: 0 };
      repositorio.importar.mockReturnValue(of(importada));
      servicio.cargar();

      servicio.importar({ nombre: ' Árboles ', descripcion: ' Parques ' }, { origen: 'KML', elementos: [], omitidos: 0, enCarpetas: false }).subscribe();

      expect(repositorio.importar).toHaveBeenCalledWith({ nombre: 'Árboles', descripcion: 'Parques' }, 'KML', []);
      expect(servicio.capas().map(({ nombre }) => nombre)[0]).toBe('Árboles');
    });

    beforeEach(() => {
      servicio.cargar();
      listarMias.mockClear();
    });

    it('crear guarda sin espacios a los lados y agrega la capa en su lugar, sin volver a pedir la lista', () => {
      const creada: CapaPropia = { id: 50, nombre: 'Árboles', descripcion: 'Parques', visibilidad: 'PRIVADA', categoriaId: 0 };
      repositorio.crear.mockReturnValue(of(creada));
      let recibida: CapaPropia | undefined;

      servicio.crear({ nombre: ' Árboles ', descripcion: ' Parques ' }).subscribe((capa) => (recibida = capa));

      expect(repositorio.crear).toHaveBeenCalledWith({ nombre: 'Árboles', descripcion: 'Parques' });
      expect(recibida).toEqual(creada);
      expect(nombres()).toEqual(['Árboles', 'Bodegas', 'Zona norte']);
      expect(listarMias).not.toHaveBeenCalled();
    });

    it('crear por URL guarda sin espacios a los lados y agrega la capa en su lugar', () => {
      const creada: CapaPropia = { id: 50, nombre: 'Árboles', descripcion: '', visibilidad: 'PRIVADA', categoriaId: 0 };
      repositorio.crearPorUrl.mockReturnValue(of(creada));

      servicio.crearPorUrl({ nombre: ' Árboles ', descripcion: '' }, ' https://ide.cl/wms ').subscribe();

      expect(repositorio.crearPorUrl).toHaveBeenCalledWith({ nombre: 'Árboles', descripcion: '' }, 'https://ide.cl/wms');
      expect(nombres()).toEqual(['Árboles', 'Bodegas', 'Zona norte']);
    });

    it('si crear falla, la lista queda como estaba', () => {
      repositorio.crear.mockReturnValue(throwError(() => new Error('sin conexión')));

      servicio.crear({ nombre: 'Árboles', descripcion: '' }).subscribe({ error: () => undefined });

      expect(nombres()).toEqual(['Bodegas', 'Zona norte']);
    });

    it('editar guarda sin espacios a los lados y reordena la lista por nombre, sin volver a pedirla', () => {
      servicio.editar(2, { ...edicion, nombre: '  Árboles ', descripcion: ' Parques ' }).subscribe();

      expect(repositorio.editar).toHaveBeenCalledWith(2, edicion);
      expect(nombres()).toEqual(['Árboles', 'Bodegas']);
      expect(servicio.capas()[0]).toEqual({ id: 2, ...edicion });
      expect(listarMias).not.toHaveBeenCalled();
    });

    it('editar actualiza la capa abierta y las favoritas', () => {
      servicio.editar(2, edicion).subscribe();

      const datos = { nombre: 'Árboles', descripcion: 'Parques', visibilidad: 'PUBLICA' };
      expect(capasAbiertas.actualizar).toHaveBeenCalledWith(2, datos);
      expect(favoritas.actualizar).toHaveBeenCalledWith(2, datos);
    });

    it('nada cambia hasta que la API confirma', () => {
      const respuesta = new Subject<void>();
      repositorio.editar.mockReturnValue(respuesta);

      servicio.editar(2, edicion).subscribe();
      expect(nombres()).toEqual(['Bodegas', 'Zona norte']);
      expect(capasAbiertas.actualizar).not.toHaveBeenCalled();

      respuesta.next();
      expect(nombres()).toEqual(['Árboles', 'Bodegas']);
    });

    it('publicar cambia solo la visibilidad', () => {
      servicio.cambiarVisibilidad(1, 'PUBLICA').subscribe();

      expect(repositorio.cambiarVisibilidad).toHaveBeenCalledWith(1, 'PUBLICA');
      expect(servicio.capas()[0]).toEqual({ ...bodegas, visibilidad: 'PUBLICA' });
      expect(capasAbiertas.actualizar).toHaveBeenCalledWith(1, { visibilidad: 'PUBLICA' });
      expect(favoritas.actualizar).toHaveBeenCalledWith(1, { visibilidad: 'PUBLICA' });
    });

    it('eliminar la saca de la lista, la cierra en el mapa y la quita de las favoritas', () => {
      servicio.eliminar(1).subscribe();

      expect(repositorio.eliminar).toHaveBeenCalledWith(1);
      expect(nombres()).toEqual(['Zona norte']);
      expect(capasAbiertas.cerrar).toHaveBeenCalledWith(1);
      expect(favoritas.quitar).toHaveBeenCalledWith(1);
    });

    it('si la capa ya no existe entrega el error y vuelve a pedir la lista', () => {
      repositorio.eliminar.mockReturnValue(throwError(() => new CapaNoEncontrada(1)));
      listarMias.mockReturnValue(of([zona]));
      let recibido: unknown;

      servicio.eliminar(1).subscribe({ error: (error: unknown) => (recibido = error) });

      expect(recibido).toBeInstanceOf(CapaNoEncontrada);
      expect(nombres()).toEqual(['Zona norte']);
      expect(capasAbiertas.cerrar).not.toHaveBeenCalled();
    });

    it('con otro error la lista queda como estaba, sin volver a pedirla', () => {
      repositorio.eliminar.mockReturnValue(throwError(() => new Error('sin conexión')));

      servicio.eliminar(1).subscribe({ error: () => undefined });

      expect(nombres()).toEqual(['Bodegas', 'Zona norte']);
      expect(listarMias).not.toHaveBeenCalled();
    });
  });

  it('explica el error de una escritura', () => {
    expect(mensajeDeEscritura(new CapaNoEncontrada(1))).toBe('La capa ya no existe o no tiene permiso para modificarla.');
    expect(mensajeDeEscritura(new Error('sin conexión'))).toBe('No se pudo guardar el cambio. Intente nuevamente.');
  });
});

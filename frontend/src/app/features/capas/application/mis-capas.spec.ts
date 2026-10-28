import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { ResumenCapa } from '../domain/capa';
import { MisCapas } from './mis-capas';
import { CapasRepositorio } from './port/out/capas.repositorio';

describe('MisCapas', () => {
  const bodegas: ResumenCapa = { id: 1, nombre: 'Bodegas', descripcion: '', visibilidad: 'PRIVADA' };
  let listarMias: ReturnType<typeof vi.fn<() => Observable<ResumenCapa[]>>>;
  let servicio: MisCapas;

  beforeEach(() => {
    listarMias = vi.fn(() => of([bodegas]));
    TestBed.configureTestingModule({ providers: [{ provide: CapasRepositorio, useValue: { listarMias } }] });
    servicio = TestBed.inject(MisCapas);
  });

  it('carga las capas del usuario', () => {
    servicio.cargar();

    expect(servicio.estado()).toBe('listo');
    expect(servicio.capas()).toEqual([bodegas]);
  });

  it('indica que está cargando mientras llegan', () => {
    const respuesta = new Subject<ResumenCapa[]>();
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
});

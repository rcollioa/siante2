import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { ResumenCapa } from '../domain/capa';
import { Favoritas } from './favoritas';
import { CapasRepositorio } from './port/out/capas.repositorio';

describe('Favoritas', () => {
  const rios: ResumenCapa = { id: 3, nombre: 'Ríos', descripcion: '', visibilidad: 'PUBLICA' };
  const hospitales: ResumenCapa = { id: 5, nombre: 'Hospitales', descripcion: '', visibilidad: 'FORMAL' };
  let listarFavoritas: ReturnType<typeof vi.fn<() => Observable<ResumenCapa[]>>>;
  let servicio: Favoritas;

  beforeEach(() => {
    listarFavoritas = vi.fn(() => of([rios, hospitales]));
    TestBed.configureTestingModule({ providers: [{ provide: CapasRepositorio, useValue: { listarFavoritas } }] });
    servicio = TestBed.inject(Favoritas);
  });

  it('separa las favoritas de las capas formales con una sola consulta', () => {
    servicio.cargar();

    expect(servicio.estado()).toBe('listo');
    expect(servicio.favoritas()).toEqual([rios]);
    expect(servicio.formales()).toEqual([hospitales]);
    expect(listarFavoritas).toHaveBeenCalledTimes(1);
  });

  it('si falla lo indica', () => {
    listarFavoritas.mockReturnValue(throwError(() => new Error('sin conexión')));

    servicio.cargar();

    expect(servicio.estado()).toBe('error');
  });
});

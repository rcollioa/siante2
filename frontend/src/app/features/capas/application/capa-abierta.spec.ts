import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { MapaPort } from '../../mapa/application/port/out/mapa.port';
import { ElementoMapa } from '../../mapa/domain/mapa';
import { Capa, CapaNoEncontrada, ElementoCapa } from '../domain/capa';
import { CapaAbierta } from './capa-abierta';
import { CapasRepositorio } from './port/out/capas.repositorio';

describe('CapaAbierta', () => {
  const elemento: ElementoCapa = {
    id: 0,
    geometria: { type: 'Point', coordinates: [-70.65, -33.44] },
    radio: 0,
    nombre: 'Bodega',
    descripcion: '',
    estilo: { color: '', grosor: 0, relleno: '', icono: '' },
    metadatos: []
  };
  const capa = (cambios: Partial<Capa> = {}): Capa => ({
    id: 1,
    nombre: 'Bodegas',
    descripcion: '',
    visibilidad: 'PRIVADA',
    propia: true,
    tipo: 'VECTORIAL',
    elementos: [elemento],
    ...cambios
  });
  let ver: ReturnType<typeof vi.fn<(id: number) => Observable<Capa>>>;
  // Dobles en memoria de los puertos: sin HTTP ni OpenLayers.
  const mapa = { mostrarElementos: vi.fn(), quitarElementos: vi.fn(), elementoSeleccionado: signal<ElementoMapa | null>(null) };
  let servicio: CapaAbierta;

  beforeEach(() => {
    vi.clearAllMocks();
    mapa.elementoSeleccionado.set(null);
    ver = vi.fn(() => of(capa()));
    TestBed.configureTestingModule({
      providers: [
        { provide: CapasRepositorio, useValue: { ver } },
        { provide: MapaPort, useValue: mapa }
      ]
    });
    servicio = TestBed.inject(CapaAbierta);
  });

  it('abre la capa y muestra sus elementos en el mapa', () => {
    servicio.abrir(1);

    expect(servicio.capa()?.nombre).toBe('Bodegas');
    expect(servicio.abriendo()).toBeNull();
    expect(mapa.mostrarElementos).toHaveBeenCalledWith([elemento]);
  });

  it('indica qué capa se está abriendo mientras llega', () => {
    const respuesta = new Subject<Capa>();
    ver.mockReturnValue(respuesta);

    servicio.abrir(1);
    expect(servicio.abriendo()).toBe(1);

    respuesta.next(capa());
    expect(servicio.abriendo()).toBeNull();
  });

  it('una capa por URL se abre sin elementos', () => {
    ver.mockReturnValue(of(capa({ tipo: 'URL', elementos: [] })));

    servicio.abrir(1);

    expect(mapa.mostrarElementos).toHaveBeenCalledWith([]);
    expect(servicio.capa()?.tipo).toBe('URL');
  });

  it('el elemento en que se hizo clic se muestra con su información', () => {
    mapa.elementoSeleccionado.set(elemento);

    expect(servicio.elemento()).toBe(elemento);
  });

  it('si la capa ya no existe lo avisa y deja la que estaba abierta', () => {
    servicio.abrir(1);
    ver.mockReturnValue(throwError(() => new CapaNoEncontrada(2)));

    servicio.abrir(2);

    expect(servicio.error()).toBe('La capa ya no existe o no tiene permiso para verla.');
    expect(servicio.capa()?.id).toBe(1);
    expect(mapa.mostrarElementos).toHaveBeenCalledTimes(1);

    servicio.descartarError();
    expect(servicio.error()).toBe('');
  });

  it('otros errores invitan a intentar de nuevo', () => {
    ver.mockReturnValue(throwError(() => new Error('sin conexión')));

    servicio.abrir(1);

    expect(servicio.error()).toBe('No se pudo abrir la capa. Intente nuevamente.');
  });

  it('cerrar quita la capa del mapa', () => {
    servicio.abrir(1);

    servicio.cerrar();

    expect(servicio.capa()).toBeNull();
    expect(mapa.quitarElementos).toHaveBeenCalled();
  });
});

import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import Feature from 'ol/Feature';
import { Observable, of, Subject, throwError } from 'rxjs';
import { MapaService } from '../mapa/mapa.service';
import { CapaAbiertaService } from './capa-abierta.service';
import { Capa, CapasApiService } from './capas-api.service';
import { ELEMENTO } from './elementos-openlayers';

describe('CapaAbiertaService', () => {
  const elemento = {
    id: 0,
    geometria: { type: 'Point', coordinates: [-70.65, -33.44], geometries: [] },
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
  const mapa = { mostrarElementos: vi.fn(), quitarElementos: vi.fn(), elementoSeleccionado: signal<Feature | null>(null) };
  let servicio: CapaAbiertaService;

  beforeEach(() => {
    vi.clearAllMocks();
    mapa.elementoSeleccionado.set(null);
    ver = vi.fn(() => of(capa()));
    TestBed.configureTestingModule({
      providers: [
        { provide: CapasApiService, useValue: { ver } },
        { provide: MapaService, useValue: mapa }
      ]
    });
    servicio = TestBed.inject(CapaAbiertaService);
  });

  it('abre la capa y muestra sus elementos en el mapa', () => {
    servicio.abrir(1);

    expect(servicio.capa()?.nombre).toBe('Bodegas');
    expect(servicio.abriendo()).toBeNull();
    const [features] = mapa.mostrarElementos.mock.calls[0] as [Feature[]];
    expect(features.map((f) => f.get(ELEMENTO))).toEqual([elemento]);
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
    const feature = new Feature();
    feature.set(ELEMENTO, elemento);

    mapa.elementoSeleccionado.set(feature);

    expect(servicio.elemento()).toBe(elemento);
  });

  it('si la capa ya no existe lo avisa y deja la que estaba abierta', () => {
    servicio.abrir(1);
    ver.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));

    servicio.abrir(2);

    expect(servicio.error()).toBe('La capa ya no existe o no tiene permiso para verla.');
    expect(servicio.capa()?.id).toBe(1);
    expect(mapa.mostrarElementos).toHaveBeenCalledTimes(1);
  });

  it('otros errores invitan a intentar de nuevo', () => {
    ver.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 503 })));

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

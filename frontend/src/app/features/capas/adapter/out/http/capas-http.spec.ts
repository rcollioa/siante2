import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SIN_PANTALLA_CARGA } from '../../../../../shared/pantalla-carga/pantalla-carga.interceptor';
import { Capa, CapaNoEncontrada, EdicionCapa, ServidorDeMapasNoDisponible } from '../../../domain/capa';
import { CapasHttp } from './capas-http';

describe('CapasHttp', () => {
  let servicio: CapasHttp;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(CapasHttp);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('escrituras', () => {
    const edicion: EdicionCapa = { nombre: 'Bodegas', descripcion: 'Región', categoriaId: 3, visibilidad: 'PUBLICA' };

    it('editar es un PATCH con los datos, que termina al responder 204', () => {
      const terminado = vi.fn();
      servicio.editar(5, edicion).subscribe({ complete: terminado });

      const pedido = http.expectOne('/siante2/api/capas/5');
      expect(pedido.request.method).toBe('PATCH');
      expect(pedido.request.body).toEqual(edicion);
      pedido.flush(null, { status: 204, statusText: 'No Content' });
      expect(terminado).toHaveBeenCalled();
    });

    it('publicar es un PATCH con la visibilidad', () => {
      servicio.cambiarVisibilidad(5, 'PRIVADA').subscribe();

      const pedido = http.expectOne('/siante2/api/capas/5/visibilidad');
      expect(pedido.request.method).toBe('PATCH');
      expect(pedido.request.body).toEqual({ visibilidad: 'PRIVADA' });
      pedido.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('eliminar es un POST, porque WebSEAL solo deja pasar los verbos que ya usa SIATE', () => {
      servicio.eliminar(5).subscribe();

      const pedido = http.expectOne('/siante2/api/capas/5/eliminar');
      expect(pedido.request.method).toBe('POST');
      pedido.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('no muestran la pantalla de carga: su diálogo avisa que está guardando', () => {
      servicio.eliminar(5).subscribe();

      const pedido = http.expectOne('/siante2/api/capas/5/eliminar');
      expect(pedido.request.context.get(SIN_PANTALLA_CARGA)).toBe(true);
      pedido.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('un 404 es CapaNoEncontrada en las tres', () => {
      const errores: unknown[] = [];
      const guardar = { error: (error: unknown) => errores.push(error) };
      servicio.editar(5, edicion).subscribe(guardar);
      servicio.cambiarVisibilidad(5, 'PUBLICA').subscribe(guardar);
      servicio.eliminar(5).subscribe(guardar);

      const noEncontrada = { status: 404, statusText: 'Not Found' };
      http.expectOne('/siante2/api/capas/5').flush({ code: 'NOT_FOUND', message: '' }, noEncontrada);
      http.expectOne('/siante2/api/capas/5/visibilidad').flush({ code: 'NOT_FOUND', message: '' }, noEncontrada);
      http.expectOne('/siante2/api/capas/5/eliminar').flush({ code: 'NOT_FOUND', message: '' }, noEncontrada);

      expect(errores).toEqual([new CapaNoEncontrada(5), new CapaNoEncontrada(5), new CapaNoEncontrada(5)]);
    });
  });

  it('lista las capas del usuario sin guardarlas: cada vez pregunta, por si cambiaron en SIATE', () => {
    let recibidas: unknown;
    servicio.listarMias().subscribe((capas) => (recibidas = capas));
    const capa = { id: 1, nombre: 'Bodegas', descripcion: '', visibilidad: 'PRIVADA', categoriaId: 3 };
    http.expectOne('/siante2/api/capas/mias').flush({ data: [capa] });
    expect(recibidas).toEqual([capa]);

    servicio.listarMias().subscribe();
    http.expectOne('/siante2/api/capas/mias').flush({ data: [] });
  });

  it('pide una capa por su ID', () => {
    servicio.ver(5).subscribe();
    http.expectOne('/siante2/api/capas/5').flush({});
  });

  it('un 404 es CapaNoEncontrada, sin que la aplicación conozca HTTP', () => {
    let error: unknown;
    servicio.ver(5).subscribe({ error: (e: unknown) => (error = e) });
    http.expectOne('/siante2/api/capas/5').flush({}, { status: 404, statusText: 'Not Found' });

    expect(error).toBeInstanceOf(CapaNoEncontrada);
    expect((error as CapaNoEncontrada).id).toBe(5);
  });

  it('una capa WMS trae sus capas y la URL del backend para pedir las imágenes; una vectorial, sin servicio', () => {
    let wms: Capa | undefined;
    let vectorial: Capa | undefined;
    servicio.ver(7).subscribe((capa) => (wms = capa));
    servicio.ver(8).subscribe((capa) => (vectorial = capa));
    http.expectOne('/siante2/api/capas/7').flush({
      id: 7, nombre: 'IDE', descripcion: '', visibilidad: 'PUBLICA', propia: false, tipo: 'URL', elementos: [],
      servicio: { tipo: 'WMS', capas: [{ nombre: 'rios', titulo: 'Ríos', extension: null }], disponible: false }
    });
    http.expectOne('/siante2/api/capas/8').flush({
      id: 8, nombre: 'Bodegas', descripcion: '', visibilidad: 'PRIVADA', propia: true, tipo: 'VECTORIAL', elementos: [],
      servicio: null
    });

    expect(wms?.servicio).toEqual({
      tipo: 'WMS',
      capas: [{ nombre: 'rios', titulo: 'Ríos', extension: null }],
      urlMapa: '/siante2/api/capas/7/wms',
      disponible: false,
      grilla: null
    });
    expect(vectorial?.servicio).toBeNull();
  });

  it('una capa WMTS pide sus mosaicos al backend y trae la grilla', () => {
    const grilla = {
      crs: 'EPSG:3857',
      matrices: [{ id: '0', resolucion: 1, origenX: 0, origenY: 0, anchoMosaico: 256, altoMosaico: 256, columnas: 1, filas: 1 }]
    };
    let wmts: Capa | undefined;
    servicio.ver(9).subscribe((capa) => (wmts = capa));
    http.expectOne('/siante2/api/capas/9').flush({
      id: 9, nombre: 'Orto', descripcion: '', visibilidad: 'PRIVADA', propia: true, tipo: 'URL', elementos: [],
      servicio: { tipo: 'WMTS', capas: [], disponible: true, grilla }
    });

    expect(wmts?.servicio).toMatchObject({ urlMapa: '/siante2/api/capas/9/wmts', grilla });
  });

  it('si el servidor de mapas de la capa no responde es ServidorDeMapasNoDisponible', () => {
    let error: unknown;
    servicio.ver(7).subscribe({ error: (e: unknown) => (error = e) });
    http.expectOne('/siante2/api/capas/7').flush(
      { code: 'EXTERNAL_SERVICE_UNAVAILABLE', message: 'El servidor de mapas de la capa no responde.' },
      { status: 503, statusText: 'Service Unavailable' }
    );

    expect(error).toBeInstanceOf(ServidorDeMapasNoDisponible);
  });

  it('consulta un punto al backend con los parámetros de WMS, nunca al servidor de mapas', () => {
    let elementos: unknown;
    servicio
      .consultarPunto({ capa: 7, capas: ['rios', 'lagos'], bbox: '1,2,3,4', ancho: 256, alto: 256, crs: 'EPSG:3857', version: '1.3.0', i: 10, j: 20 })
      .subscribe((respuesta) => (elementos = respuesta));

    const pedido = http.expectOne((req) => req.url === '/siante2/api/capas/7/wms/info');
    expect(pedido.request.params.toString()).toBe('LAYERS=rios,lagos&BBOX=1,2,3,4&WIDTH=256&HEIGHT=256&CRS=EPSG:3857&VERSION=1.3.0&I=10&J=20');
    pedido.flush({ data: [{ capa: 'Ríos', datos: [] }] });

    expect(elementos).toEqual([{ capa: 'Ríos', datos: [] }]);
  });

  it('si el servidor de mapas no responde la consulta es ServidorDeMapasNoDisponible', () => {
    let error: unknown;
    servicio
      .consultarPunto({ capa: 7, capas: ['rios'], bbox: '1,2,3,4', ancho: 256, alto: 256, crs: 'EPSG:3857', version: '1.3.0', i: 0, j: 0 })
      .subscribe({ error: (e: unknown) => (error = e) });
    http
      .expectOne((req) => req.url === '/siante2/api/capas/7/wms/info')
      .flush({ code: 'EXTERNAL_SERVICE_UNAVAILABLE', message: '' }, { status: 503, statusText: 'Service Unavailable' });

    expect(error).toBeInstanceOf(ServidorDeMapasNoDisponible);
  });

  it('los demás errores llegan tal cual', () => {
    let error: unknown;
    servicio.ver(5).subscribe({ error: (e: unknown) => (error = e) });
    http.expectOne('/siante2/api/capas/5').flush({}, { status: 503, statusText: 'Service Unavailable' });

    expect(error).not.toBeInstanceOf(CapaNoEncontrada);
  });
});

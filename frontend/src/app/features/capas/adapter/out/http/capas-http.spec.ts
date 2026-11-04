import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SIN_PANTALLA_CARGA } from '../../../../../shared/pantalla-carga/pantalla-carga.interceptor';
import { CambiosRechazados, Capa, CapaNoEncontrada, EdicionCapa, ElementoCapa, ServidorDeMapasNoDisponible } from '../../../domain/capa';
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

  describe('importar', () => {
    const elemento: ElementoCapa = {
      id: -1,
      geometria: { type: 'Point', coordinates: [-70.65, -33.44] },
      radio: 0,
      nombre: '',
      descripcion: '',
      estilo: { color: '', grosor: 0, relleno: '', icono: '' },
      metadatos: []
    };

    it('es un POST con la capa y sus elementos, y entrega la capa creada', () => {
      const creada = { id: 50, nombre: 'Bodegas', descripcion: '', visibilidad: 'PRIVADA', categoriaId: 0 };
      let recibida: unknown;
      servicio.importar({ nombre: 'Bodegas', descripcion: '' }, 'KML', [elemento]).subscribe((capa) => (recibida = capa));

      const pedido = http.expectOne('/siante2/api/capas/importar');
      expect(pedido.request.method).toBe('POST');
      expect(pedido.request.body).toEqual({ nombre: 'Bodegas', descripcion: '', origen: 'KML', elementos: [elemento] });
      expect(pedido.request.context.get(SIN_PANTALLA_CARGA)).toBe(true);
      pedido.flush(creada, { status: 201, statusText: 'Created' });
      expect(recibida).toEqual(creada);
    });

    it('un 400 entrega el mensaje del backend', () => {
      let error: unknown;
      servicio.importar({ nombre: 'Bodegas', descripcion: '' }, 'KML', [elemento]).subscribe({ error: (e: unknown) => (error = e) });

      http
        .expectOne('/siante2/api/capas/importar')
        .flush({ code: 'INVALID_REQUEST', message: 'Nombre requerido.' }, { status: 400, statusText: 'Bad Request' });

      expect(error).toEqual(new CambiosRechazados('Nombre requerido.'));
    });
  });

  describe('guardarElementos', () => {
    const nuevo: ElementoCapa = {
      id: -1,
      geometria: { type: 'Point', coordinates: [-70.65, -33.44] },
      radio: 0,
      nombre: 'Bodega',
      descripcion: '',
      estilo: { color: '', grosor: 0, relleno: '', icono: '' },
      metadatos: []
    };
    const cambios = { nuevos: [nuevo], modificados: [], eliminados: [3] };

    it('es un POST con los cambios, con pantalla de carga, y entrega los ID de los nuevos', () => {
      let ids: number[] = [];
      servicio.guardarElementos(5, cambios).subscribe((recibidos) => (ids = recibidos));

      const pedido = http.expectOne('/siante2/api/capas/5/elementos');
      expect(pedido.request.method).toBe('POST');
      expect(pedido.request.body).toEqual(cambios);
      expect(pedido.request.context.get(SIN_PANTALLA_CARGA)).toBe(false);
      pedido.flush({ nuevos: [12] });
      expect(ids).toEqual([12]);
    });

    it('un 404 es CapaNoEncontrada y un 400 entrega el mensaje del backend', () => {
      const errores: unknown[] = [];
      const guardar = { error: (error: unknown) => errores.push(error) };
      servicio.guardarElementos(5, cambios).subscribe(guardar);
      http.expectOne('/siante2/api/capas/5/elementos').flush({ code: 'NOT_FOUND', message: '' }, { status: 404, statusText: 'Not Found' });
      servicio.guardarElementos(5, cambios).subscribe(guardar);
      http
        .expectOne('/siante2/api/capas/5/elementos')
        .flush({ code: 'INVALID_REQUEST', message: 'El color no es válido.' }, { status: 400, statusText: 'Bad Request' });

      expect(errores).toEqual([new CapaNoEncontrada(5), new CambiosRechazados('El color no es válido.')]);
    });
  });

  describe('escrituras', () => {
    const edicion: EdicionCapa = { nombre: 'Bodegas', descripcion: 'Región', categoriaId: 3, visibilidad: 'PUBLICA' };

    it('crear es un POST con los datos, sin pantalla de carga, y entrega la capa creada', () => {
      const creada = { id: 50, nombre: 'Bodegas', descripcion: '', visibilidad: 'PRIVADA', categoriaId: 0 };
      let recibida: unknown;
      servicio.crear({ nombre: 'Bodegas', descripcion: '' }).subscribe((capa) => (recibida = capa));

      const pedido = http.expectOne('/siante2/api/capas');
      expect(pedido.request.method).toBe('POST');
      expect(pedido.request.body).toEqual({ nombre: 'Bodegas', descripcion: '' });
      expect(pedido.request.context.get(SIN_PANTALLA_CARGA)).toBe(true);
      pedido.flush(creada, { status: 201, statusText: 'Created' });
      expect(recibida).toEqual(creada);
    });

    it('crear por URL es un POST con los datos y la URL, sin pantalla de carga', () => {
      const creada = { id: 50, nombre: 'Ríos', descripcion: '', visibilidad: 'PRIVADA', categoriaId: 0 };
      let recibida: unknown;
      servicio.crearPorUrl({ nombre: 'Ríos', descripcion: '' }, 'https://ide.cl/wms').subscribe((capa) => (recibida = capa));

      const pedido = http.expectOne('/siante2/api/capas/url');
      expect(pedido.request.method).toBe('POST');
      expect(pedido.request.body).toEqual({ nombre: 'Ríos', descripcion: '', url: 'https://ide.cl/wms' });
      expect(pedido.request.context.get(SIN_PANTALLA_CARGA)).toBe(true);
      pedido.flush(creada, { status: 201, statusText: 'Created' });
      expect(recibida).toEqual(creada);
    });

    it('crear por URL traduce el rechazo del backend con su mensaje', () => {
      let error: unknown;
      servicio.crearPorUrl({ nombre: 'Ríos', descripcion: '' }, 'https://ide.cl/wfs').subscribe({ error: (e: unknown) => (error = e) });

      http
        .expectOne('/siante2/api/capas/url')
        .flush({ code: 'INVALID_REQUEST', message: 'El mapa no puede mostrar servidores WFS.' }, { status: 400, statusText: 'Bad Request' });

      expect(error).toEqual(new CambiosRechazados('El mapa no puede mostrar servidores WFS.'));
    });

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
      id: 7, nombre: 'IDE', descripcion: '', visibilidad: 'PUBLICA', propia: false, editable: false, tipo: 'URL', elementos: [],
      servicio: { tipo: 'WMS', capas: [{ nombre: 'rios', titulo: 'Ríos', extension: null }], disponible: false }
    });
    http.expectOne('/siante2/api/capas/8').flush({
      id: 8, nombre: 'Bodegas', descripcion: '', visibilidad: 'PRIVADA', propia: true, editable: true, tipo: 'VECTORIAL', elementos: [],
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
      id: 9, nombre: 'Orto', descripcion: '', visibilidad: 'PRIVADA', propia: true, editable: false, tipo: 'URL', elementos: [],
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

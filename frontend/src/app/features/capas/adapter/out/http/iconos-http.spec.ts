import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SIN_PANTALLA_CARGA } from '../../../../../shared/pantalla-carga/pantalla-carga.interceptor';
import { CambiosRechazados } from '../../../domain/capa';
import { IconosHttp } from './iconos-http';

describe('IconosHttp', () => {
  const datos = { nombre: 'Bodega', archivo: 'bodega.png', imagen: 'data:image/png;base64,AA==' };
  let servicio: IconosHttp;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(IconosHttp);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listar entrega lo que viene en data, sin pantalla de carga', () => {
    let recibidos: unknown;
    servicio.listar().subscribe((iconos) => (recibidos = iconos));

    const pedido = http.expectOne('/siante2/api/iconos');
    expect(pedido.request.context.get(SIN_PANTALLA_CARGA)).toBe(true);
    pedido.flush({ data: [{ id: 1, ...datos, propio: false }] });

    expect(recibidos).toEqual([{ id: 1, ...datos, propio: false }]);
  });

  it('crear es un POST con los datos y entrega el ícono', () => {
    let recibido: unknown;
    servicio.crear(datos).subscribe((icono) => (recibido = icono));

    const pedido = http.expectOne('/siante2/api/iconos');
    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.body).toEqual(datos);
    pedido.flush({ id: 50, ...datos, propio: true }, { status: 201, statusText: 'Created' });

    expect(recibido).toEqual({ id: 50, ...datos, propio: true });
  });

  it('editar es un PATCH y eliminar un POST, que terminan al responder 204', () => {
    const terminado = vi.fn();
    servicio.editar(5, datos).subscribe({ complete: terminado });
    servicio.eliminar(5).subscribe({ complete: terminado });

    const edicion = http.expectOne('/siante2/api/iconos/5');
    expect(edicion.request.method).toBe('PATCH');
    edicion.flush(null, { status: 204, statusText: 'No Content' });
    const eliminacion = http.expectOne('/siante2/api/iconos/5/eliminar');
    expect(eliminacion.request.method).toBe('POST');
    eliminacion.flush(null, { status: 204, statusText: 'No Content' });

    expect(terminado).toHaveBeenCalledTimes(2);
  });

  it('un 400 se traduce con su mensaje y otro error pasa tal cual', () => {
    const errores: unknown[] = [];
    servicio.crear(datos).subscribe({ error: (e: unknown) => errores.push(e) });
    http
      .expectOne('/siante2/api/iconos')
      .flush({ code: 'INVALID_REQUEST', message: 'Nombre requerido.' }, { status: 400, statusText: 'Bad Request' });
    servicio.editar(5, datos).subscribe({ error: (e: unknown) => errores.push(e) });
    http.expectOne('/siante2/api/iconos/5').flush(null, { status: 404, statusText: 'Not Found' });

    expect(errores[0]).toEqual(new CambiosRechazados('Nombre requerido.'));
    expect(errores[1]).not.toBeInstanceOf(CambiosRechazados);
  });
});

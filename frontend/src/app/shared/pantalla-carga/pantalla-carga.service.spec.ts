import { PantallaCargaService } from './pantalla-carga.service';

describe('PantallaCargaService', () => {
  let servicio: PantallaCargaService;

  beforeEach(() => {
    servicio = new PantallaCargaService();
  });

  it('con varias peticiones en curso, se oculta recién cuando termina la última', () => {
    servicio.mostrar();
    servicio.mostrar();
    servicio.ocultar();
    expect(servicio.visible()).toBe(true);
    servicio.ocultar();
    expect(servicio.visible()).toBe(false);
  });

  it('un ocultar de más no deja el contador negativo', () => {
    servicio.ocultar();
    servicio.mostrar();
    expect(servicio.visible()).toBe(true);
  });

  it('liberar un bloqueo dos veces descuenta solo una vez', () => {
    const liberar = servicio.bloquear();
    servicio.mostrar();
    liberar();
    liberar();
    expect(servicio.visible()).toBe(true);
  });

  it('muestra el mensaje pedido', () => {
    servicio.bloquear('Cargando datos');
    expect(servicio.mensaje()).toBe('Cargando datos');
  });
});

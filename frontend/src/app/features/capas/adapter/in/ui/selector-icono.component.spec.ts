import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { CatalogoIconos, EstadoIconos } from '../../../application/catalogo-iconos';
import { IconoLeido, LectorIconos } from '../../../application/lector-iconos';
import { CambiosRechazados } from '../../../domain/capa';
import { DatosIcono, Icono, IconoInvalido } from '../../../domain/icono';
import { SelectorIconoComponent } from './selector-icono.component';

describe('SelectorIconoComponent', () => {
  const PNG = 'data:image/png;base64,AA==';
  const OTRO = 'data:image/png;base64,BB==';
  const sistema: Icono = { id: 1, nombre: 'Sistema', archivo: '', imagen: PNG, propio: false };
  const mio: Icono = { id: 5, nombre: 'Mío', archivo: 'mio.png', imagen: OTRO, propio: true };
  const estado = signal<EstadoIconos>('listo');
  const catalogo = {
    estado,
    delSistema: signal([sistema]),
    propios: signal([mio]),
    cargar: vi.fn(),
    crear: vi.fn<(datos: DatosIcono) => Observable<Icono>>(),
    editar: vi.fn<(id: number, datos: DatosIcono) => Observable<void>>(),
    eliminar: vi.fn<(id: number) => Observable<void>>()
  };
  const leer = vi.fn<(archivo: File) => Promise<IconoLeido>>();
  let fixture: ComponentFixture<SelectorIconoComponent>;
  let elemento: HTMLElement;
  let elegidos: string[];

  const boton = (nombre: string) => elemento.querySelector(`button[aria-label="${nombre}"]`) as HTMLButtonElement;
  const conTexto = (texto: string) =>
    Array.from(elemento.querySelectorAll('button')).find((b) => b.textContent?.trim() === texto) as HTMLButtonElement;
  const nombre = () => elemento.querySelector('#nombre-icono') as HTMLInputElement;
  const errores = () => Array.from(elemento.querySelectorAll('.error')).map((e) => e.textContent?.trim());

  function mostrar(icono = ''): void {
    fixture.componentRef.setInput('icono', icono);
    fixture.detectChanges();
  }

  function clic(destino: HTMLElement): void {
    destino.click();
    fixture.detectChanges();
  }

  function escribirNombre(valor: string): void {
    nombre().value = valor;
    nombre().dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  async function elegirArchivo(): Promise<void> {
    const campo = elemento.querySelector('#archivo-icono') as HTMLInputElement;
    Object.defineProperty(campo, 'files', { configurable: true, value: [{ name: 'nuevo.png' }] });
    campo.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function enviar(): void {
    (elemento.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }

  beforeAll(() => {
    if (!HTMLDialogElement.prototype.showModal) {
      HTMLDialogElement.prototype.showModal = function (this: HTMLDialogElement) {
        this.open = true;
      };
      HTMLDialogElement.prototype.close = function (this: HTMLDialogElement) {
        this.open = false;
      };
    }
  });

  beforeEach(() => {
    vi.resetAllMocks();
    estado.set('listo');
    leer.mockResolvedValue({ archivo: 'nuevo.png', imagen: PNG });
    catalogo.crear.mockReturnValue(of(mio));
    catalogo.editar.mockReturnValue(of(undefined));
    catalogo.eliminar.mockReturnValue(of(undefined));
    elegidos = [];
    TestBed.configureTestingModule({
      providers: [
        { provide: CatalogoIconos, useValue: catalogo },
        { provide: LectorIconos, useValue: { leer } }
      ]
    });
    fixture = TestBed.createComponent(SelectorIconoComponent);
    fixture.componentInstance.elegido.subscribe((icono) => elegidos.push(icono));
    elemento = fixture.nativeElement;
  });

  it('pide el catálogo y marca el marcador normal si el elemento no tiene un ícono propio', () => {
    mostrar('./assets/img/marker.svg');

    expect(catalogo.cargar).toHaveBeenCalled();
    expect(boton('Marcador normal').getAttribute('aria-pressed')).toBe('true');
    expect(boton('Sistema').getAttribute('aria-pressed')).toBe('false');
  });

  it('marca el ícono que usa el elemento', () => {
    mostrar(OTRO);

    expect(boton('Mío').getAttribute('aria-pressed')).toBe('true');
    expect(boton('Marcador normal').getAttribute('aria-pressed')).toBe('false');
  });

  it('un clic entrega la imagen del ícono, y el marcador normal lo que guarda SIATE', () => {
    mostrar();

    boton('Sistema').click();
    boton('Mío').click();
    boton('Marcador normal').click();

    expect(elegidos).toEqual([PNG, OTRO, './assets/img/marker.svg']);
  });

  it('solo los propios se pueden editar o eliminar', () => {
    mostrar();

    expect(boton('Editar Mío')).not.toBeNull();
    expect(boton('Eliminar Mío')).not.toBeNull();
    expect(boton('Editar Sistema')).toBeNull();
  });

  it('si el catálogo falla lo dice y permite reintentar', () => {
    estado.set('error');
    mostrar();
    catalogo.cargar.mockClear();

    conTexto('Reintentar').click();

    expect(catalogo.cargar).toHaveBeenCalled();
  });

  it('crea un ícono con nombre y archivo, y cierra el formulario', async () => {
    mostrar();
    clic(conTexto('Agregar ícono'));
    escribirNombre(' Bodega ');
    await elegirArchivo();

    enviar();

    expect(catalogo.crear).toHaveBeenCalledWith({ nombre: ' Bodega ', archivo: 'nuevo.png', imagen: PNG });
    expect(elemento.querySelector('form')).toBeNull();
  });

  it('sin nombre ni archivo lo explica y no crea', () => {
    mostrar();
    clic(conTexto('Agregar ícono'));

    enviar();

    expect(errores()).toEqual(['Nombre requerido.', 'Seleccione un archivo SVG o PNG.']);
    expect(catalogo.crear).not.toHaveBeenCalled();
  });

  it('muestra por qué no sirve el archivo y lo puede cambiar', async () => {
    leer.mockRejectedValueOnce(new IconoInvalido('Muy grande.'));
    mostrar();
    clic(conTexto('Agregar ícono'));
    await elegirArchivo();
    expect(errores()).toEqual(['Muy grande.']);

    await elegirArchivo();

    expect(errores()).toEqual([]);
  });

  it('editar parte con sus datos y, sin otro archivo, conserva la imagen', () => {
    mostrar();
    clic(boton('Editar Mío'));
    expect(nombre().value).toBe('Mío');
    escribirNombre('Otro nombre');

    enviar();

    expect(catalogo.editar).toHaveBeenCalledWith(5, { nombre: 'Otro nombre', archivo: 'mio.png', imagen: OTRO });
    expect(elemento.querySelector('form')).toBeNull();
  });

  it('mientras guarda no deja enviar dos veces', async () => {
    const respuesta = new Subject<Icono>();
    catalogo.crear.mockReturnValue(respuesta);
    mostrar();
    clic(conTexto('Agregar ícono'));
    escribirNombre('Bodega');
    await elegirArchivo();

    enviar();
    enviar();

    expect(catalogo.crear).toHaveBeenCalledOnce();
    expect(conTexto('Guardando…').disabled).toBe(true);
  });

  it('si el backend rechaza muestra su mensaje, y otro error uno general, conservando el formulario', async () => {
    catalogo.crear.mockReturnValueOnce(throwError(() => new CambiosRechazados('Imagen rechazada.')));
    mostrar();
    clic(conTexto('Agregar ícono'));
    escribirNombre('Bodega');
    await elegirArchivo();

    enviar();
    expect(errores()).toEqual(['Imagen rechazada.']);

    catalogo.crear.mockReturnValueOnce(throwError(() => new Error('sin conexión')));
    enviar();
    expect(errores()).toEqual(['No se pudo guardar el ícono. Intente nuevamente.']);
    expect(nombre().value).toBe('Bodega');
  });

  it('cancelar cierra sin guardar', () => {
    mostrar();
    clic(conTexto('Agregar ícono'));

    clic(conTexto('Cancelar'));

    expect(elemento.querySelector('form')).toBeNull();
    expect(catalogo.crear).not.toHaveBeenCalled();
  });

  it('eliminar pregunta antes, y al confirmar lo elimina', () => {
    mostrar();
    clic(boton('Eliminar Mío'));
    expect(catalogo.eliminar).not.toHaveBeenCalled();
    expect(elemento.querySelector('dialog')?.textContent).toContain('¿Confirma el borrado del ícono?');

    clic(conTexto('Sí'));

    expect(catalogo.eliminar).toHaveBeenCalledWith(5);
  });
});

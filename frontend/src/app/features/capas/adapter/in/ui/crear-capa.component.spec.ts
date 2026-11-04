import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { LectorArchivos } from '../../../application/lector-archivos';
import { MisCapas } from '../../../application/mis-capas';
import { ArchivoInvalido, ArchivoLeido } from '../../../domain/archivo-geografico';
import { CambiosRechazados, CapaPropia, NuevaCapa } from '../../../domain/capa';
import { TablaLeida } from '../../../domain/tabla-geografica';
import { CrearCapaComponent } from './crear-capa.component';

describe('CrearCapaComponent', () => {
  const creada: CapaPropia = { id: 50, nombre: 'Bodegas', descripcion: 'Región', visibilidad: 'PRIVADA', categoriaId: 0 };
  const crear = vi.fn<(nueva: NuevaCapa) => Observable<CapaPropia>>();
  const importar = vi.fn<(nueva: NuevaCapa, archivo: ArchivoLeido) => Observable<CapaPropia>>();
  const crearPorUrl = vi.fn<(nueva: NuevaCapa, url: string) => Observable<CapaPropia>>();
  const leer = vi.fn<(archivo: File) => Promise<ArchivoLeido>>();
  const leerTabla = vi.fn<(archivo: File) => Promise<TablaLeida>>();
  const leido: ArchivoLeido = {
    origen: 'KML',
    elementos: [{ id: -1, geometria: { type: 'Point', coordinates: [-70, -33] }, radio: 0, nombre: '', descripcion: '', estilo: { color: '', grosor: 0, relleno: '', icono: '' }, metadatos: [] }],
    omitidos: 0,
    enCarpetas: false
  };
  let fixture: ComponentFixture<CrearCapaComponent>;
  let elemento: HTMLElement;
  let recibidas: CapaPropia[];
  let importadas: CapaPropia[];

  const dialogo = () => elemento.querySelector('dialog') as HTMLDialogElement;
  const nombre = () => elemento.querySelector('#nombre-nueva-capa') as HTMLInputElement;
  const descripcion = () => elemento.querySelector('#descripcion-nueva-capa') as HTMLTextAreaElement;
  const aplicar = () => elemento.querySelector('button[type="submit"]') as HTMLButtonElement;
  const errores = () => Array.from(elemento.querySelectorAll('.error')).map((e) => e.textContent?.trim());

  function escribir(campo: HTMLInputElement | HTMLTextAreaElement, valor: string): void {
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  /** Enviar el formulario como lo hace el botón, que jsdom no dispara con un clic. */
  function enviar(): void {
    (elemento.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }

  function abrir(): void {
    fixture.componentInstance.abrir();
    fixture.detectChanges();
  }

  // jsdom no siempre implementa el diálogo modal: basta con marcarlo abierto o cerrado.
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
    crear.mockReset();
    crear.mockReturnValue(of(creada));
    importar.mockReset();
    importar.mockReturnValue(of(creada));
    crearPorUrl.mockReset();
    crearPorUrl.mockReturnValue(of(creada));
    leer.mockReset();
    leer.mockResolvedValue(leido);
    recibidas = [];
    importadas = [];
    TestBed.configureTestingModule({
      providers: [
        { provide: MisCapas, useValue: { crear, importar, crearPorUrl } },
        { provide: LectorArchivos, useValue: { leer, leerTabla } }
      ]
    });
    fixture = TestBed.createComponent(CrearCapaComponent);
    fixture.componentInstance.creada.subscribe((capa) => recibidas.push(capa));
    fixture.componentInstance.importada.subscribe((capa) => importadas.push(capa));
    fixture.detectChanges();
    elemento = fixture.nativeElement;
  });

  it('abre vacío y sin mostrar errores', () => {
    abrir();

    expect(dialogo().open).toBe(true);
    expect(nombre().value).toBe('');
    expect(errores()).toEqual([]);
    expect(document.activeElement).toBe(nombre());
  });

  it('crea la capa, cierra y avisa cuál se creó', () => {
    abrir();
    escribir(nombre(), 'Bodegas');
    escribir(descripcion(), 'Región');

    enviar();

    expect(crear).toHaveBeenCalledWith({ nombre: 'Bodegas', descripcion: 'Región' });
    expect(dialogo().open).toBe(false);
    expect(recibidas).toEqual([creada]);
  });

  it('con un nombre inválido muestra el error de SIATE y no crea', () => {
    abrir();

    enviar();
    expect(errores()).toEqual(['Nombre requerido.']);
    expect(nombre().getAttribute('aria-invalid')).toBe('true');
    expect(nombre().getAttribute('aria-describedby')).toBe('error-nombre-nueva-capa');

    escribir(nombre(), 'abc');
    expect(errores()).toEqual(['El nombre debe tener al menos 4 caracteres.']);

    enviar();
    expect(crear).not.toHaveBeenCalled();
    expect(dialogo().open).toBe(true);
  });

  it('al salir del nombre ya muestra si falta', () => {
    abrir();

    nombre().dispatchEvent(new Event('blur'));
    fixture.detectChanges();

    expect(errores()).toEqual(['Nombre requerido.']);
  });

  it('mientras crea no deja enviar dos veces', () => {
    const respuesta = new Subject<CapaPropia>();
    crear.mockReturnValue(respuesta);
    abrir();
    escribir(nombre(), 'Bodegas');

    enviar();
    expect(aplicar().disabled).toBe(true);
    expect(aplicar().textContent).toContain('Creando');

    enviar();
    expect(crear).toHaveBeenCalledOnce();

    respuesta.next(creada);
    expect(dialogo().open).toBe(false);
  });

  it('si falla lo explica, queda abierto y conserva lo escrito', () => {
    crear.mockReturnValue(throwError(() => new Error('sin conexión')));
    abrir();
    escribir(nombre(), 'Bodegas');

    enviar();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toBe('No se pudo crear la capa. Intente nuevamente.');
    expect(dialogo().open).toBe(true);
    expect(nombre().value).toBe('Bodegas');
    expect(aplicar().disabled).toBe(false);
    expect(recibidas).toEqual([]);
  });

  describe('desde un Excel o un CSV', () => {
    const tabla: TablaLeida = {
      origen: 'CSV',
      titulos: ['Nombre', 'Lat', 'Lon'],
      filas: [['Bodega', '-33', '-70'], ['Mala', 'x', 'y']]
    };
    const select = (id: string) => elemento.querySelector(`#${id}`) as HTMLSelectElement;
    const estado = () => elemento.querySelector('[role="status"]')?.textContent?.trim();

    async function elegir(leida = tabla, nombreArchivo = 'sitios.csv'): Promise<void> {
      leerTabla.mockResolvedValue(leida);
      abrir();
      (elemento.querySelector('input[value="ARCHIVO"]') as HTMLInputElement).click();
      fixture.detectChanges();
      const campo = elemento.querySelector('#archivo-nueva-capa') as HTMLInputElement;
      Object.defineProperty(campo, 'files', { configurable: true, value: [{ name: nombreArchivo }] });
      campo.dispatchEvent(new Event('change'));
      await fixture.whenStable();
      fixture.detectChanges();
    }

    function cambiar(id: string, valor: string): void {
      select(id).value = valor;
      select(id).dispatchEvent(new Event('change'));
      fixture.detectChanges();
    }

    it('reconoce las columnas por sus títulos, avisa lo que importa y lo que omite, y crea la capa', async () => {
      await elegir();

      expect(leer).not.toHaveBeenCalled();
      expect(select('columna-ubicacion-0').value).toBe('1');
      expect(select('columna-ubicacion-1').value).toBe('2');
      expect(estado()).toContain('Se importará 1 elemento.');
      expect(estado()).toContain('1 elemento no tiene una geometría válida');
      expect(nombre().value).toBe('sitios');

      enviar();

      const [, leido] = importar.mock.lastCall!;
      expect(leido.origen).toBe('CSV');
      expect(leido.elementos[0].nombre).toBe('Bodega');
      expect(importadas).toEqual([creada]);
    });

    it('sin columnas reconocidas las pide antes de crear', async () => {
      await elegir({ ...tabla, titulos: ['A', 'B', 'C'] });
      escribir(nombre(), 'Bodegas');

      enviar();

      expect(errores()).toEqual(['Elija las columnas con la ubicación.']);
      expect(importar).not.toHaveBeenCalled();
    });

    it('elegir las columnas deja lista la importación', async () => {
      await elegir({ ...tabla, titulos: ['A', 'B', 'C'] });

      cambiar('columna-ubicacion-0', '1');
      expect(estado()).toBe('');
      cambiar('columna-ubicacion-1', '2');

      expect(estado()).toContain('Se importará 1 elemento.');
    });

    it('cambiar el tipo pide sus propias columnas', async () => {
      await elegir();

      cambiar('tipo-ubicacion', 'UTM');

      expect(select('columna-ubicacion-2')).not.toBeNull();
      expect(select('columna-ubicacion-0').value).toBe('-1');
    });

    it('si ninguna fila sirve lo explica, y otra columna lo corrige', async () => {
      await elegir({ ...tabla, titulos: ['Nombre', 'Lat', 'Lon', 'Otra'], filas: [['A', 'x', '-70', '-33']] });
      expect(alerta()).toContain('Las coordenadas del archivo no son válidas');

      cambiar('columna-ubicacion-0', '3');

      expect(alerta()).toBeUndefined();
      expect(estado()).toContain('Se importará 1 elemento.');
    });

    const alerta = () => elemento.querySelector('#error-archivo-nueva-capa')?.textContent?.trim();
  });

  describe('desde una URL', () => {
    const campoUrl = () => elemento.querySelector('#url-nueva-capa') as HTMLInputElement;

    function elegirTipoUrl(): void {
      (elemento.querySelector('input[value="URL"]') as HTMLInputElement).click();
      fixture.detectChanges();
    }

    it('el campo solo aparece al elegir esa opción', () => {
      abrir();
      expect(campoUrl()).toBeNull();

      elegirTipoUrl();

      expect(campoUrl()).not.toBeNull();
    });

    it('crea la capa con su URL y la entrega para abrirla, sin entrar a dibujar', () => {
      abrir();
      elegirTipoUrl();
      escribir(nombre(), 'Ríos');
      escribir(campoUrl(), 'https://ide.cl/wms');

      enviar();

      expect(crearPorUrl).toHaveBeenCalledWith({ nombre: 'Ríos', descripcion: '' }, 'https://ide.cl/wms');
      expect(crear).not.toHaveBeenCalled();
      expect(dialogo().open).toBe(false);
      expect(importadas).toEqual([creada]);
      expect(recibidas).toEqual([]);
    });

    it.each([
      ['', 'URL requerida.'],
      ['   ', 'URL requerida.'],
      ['ide.cl/wms', 'Debe ser una URL válida.'],
      ['ftp://ide.cl/wms', 'Debe ser una URL válida.'],
      ['https://', 'Debe ser una URL válida.'],
      ['https://ide cl/wms', 'Debe ser una URL válida.'],
      ['https://ide.cl/' + 'a'.repeat(2000), 'La URL no puede tener más de 2000 caracteres.']
    ])('con la URL «%s» muestra el error y no crea', (url, mensaje) => {
      abrir();
      elegirTipoUrl();
      escribir(nombre(), 'Ríos');
      escribir(campoUrl(), url);

      enviar();

      expect(errores()).toEqual([mensaje]);
      expect(campoUrl().getAttribute('aria-describedby')).toBe('error-url-nueva-capa');
      expect(crearPorUrl).not.toHaveBeenCalled();
      expect(dialogo().open).toBe(true);
    });

    it('muestra el motivo del rechazo del backend y conserva la URL', () => {
      crearPorUrl.mockReturnValue(throwError(() => new CambiosRechazados('El mapa no puede mostrar servidores WFS.')));
      abrir();
      elegirTipoUrl();
      escribir(nombre(), 'Ríos');
      escribir(campoUrl(), 'https://ide.cl/wfs');

      enviar();

      expect(elemento.querySelector('[role="alert"]')?.textContent).toBe('El mapa no puede mostrar servidores WFS.');
      expect(campoUrl().value).toBe('https://ide.cl/wfs');
    });

    it('al volver a abrir el diálogo la URL queda vacía', () => {
      abrir();
      elegirTipoUrl();
      escribir(campoUrl(), 'https://ide.cl/wms');

      abrir();
      elegirTipoUrl();

      expect(campoUrl().value).toBe('');
    });
  });

  describe('desde un archivo', () => {
    const opcion = (valor: string) => elemento.querySelector(`input[value="${valor}"]`) as HTMLInputElement;
    const campoArchivo = () => elemento.querySelector('#archivo-nueva-capa') as HTMLInputElement;
    const estado = () => elemento.querySelector('[role="status"]')?.textContent?.trim();
    const alerta = () => elemento.querySelector('#error-archivo-nueva-capa')?.textContent?.trim();

    function elegirTipoArchivo(): void {
      opcion('ARCHIVO').click();
      fixture.detectChanges();
    }

    async function elegir(nombre = 'Bodegas norte.kml'): Promise<void> {
      Object.defineProperty(campoArchivo(), 'files', { configurable: true, value: [{ name: nombre }] });
      campoArchivo().dispatchEvent(new Event('change'));
      await fixture.whenStable();
      fixture.detectChanges();
    }

    beforeEach(() => abrir());

    it('parte como capa en blanco, sin pedir un archivo', () => {
      expect(opcion('BLANCO').checked).toBe(true);
      expect(campoArchivo()).toBeNull();
    });

    it('dice cuántos elementos va a importar y propone el nombre del archivo', async () => {
      elegirTipoArchivo();
      await elegir();

      expect(estado()).toBe('Se importará 1 elemento.');
      expect(nombre().value).toBe('Bodegas norte');
    });

    it('no reemplaza el nombre que ya escribió', async () => {
      escribir(nombre(), 'Mi capa');
      elegirTipoArchivo();
      await elegir();

      expect(nombre().value).toBe('Mi capa');
    });

    it('avisa lo que se omite y que las carpetas no se conservan', async () => {
      leer.mockResolvedValue({ ...leido, elementos: [leido.elementos[0], leido.elementos[0]], omitidos: 3, enCarpetas: true });
      elegirTipoArchivo();
      await elegir();

      expect(estado()).toContain('Se importarán 2 elementos.');
      expect(estado()).toContain('3 elementos no tienen una geometría válida');
      expect(estado()).toContain('Las carpetas del KML no se conservan');
    });

    it('crea la capa con sus elementos, y avisa aparte que viene de un archivo', async () => {
      elegirTipoArchivo();
      await elegir();

      enviar();

      expect(importar).toHaveBeenCalledWith({ nombre: 'Bodegas norte', descripcion: '' }, leido);
      expect(crear).not.toHaveBeenCalled();
      expect(importadas).toEqual([creada]);
      expect(recibidas).toEqual([]);
      expect(dialogo().open).toBe(false);
    });

    it('sin archivo lo pide y no crea nada', () => {
      elegirTipoArchivo();
      escribir(nombre(), 'Bodegas');

      enviar();

      expect(alerta()).toBe('Seleccione un archivo.');
      expect(campoArchivo().getAttribute('aria-invalid')).toBe('true');
      expect(importar).not.toHaveBeenCalled();
      expect(crear).not.toHaveBeenCalled();
    });

    it('un archivo inválido explica por qué y no deja crear', async () => {
      leer.mockRejectedValue(new ArchivoInvalido('El archivo no tiene geometrías.'));
      elegirTipoArchivo();
      await elegir();
      escribir(nombre(), 'Bodegas');

      enviar();

      expect(alerta()).toBe('El archivo no tiene geometrías.');
      expect(importar).not.toHaveBeenCalled();
    });

    it('un error inesperado al leer no muestra detalles técnicos', async () => {
      leer.mockRejectedValue(new Error('NotReadableError'));
      elegirTipoArchivo();
      await elegir();

      expect(alerta()).toBe('No se pudo leer el archivo.');
    });

    it('mientras lee no deja crear', async () => {
      let terminar: (archivo: ArchivoLeido) => void = () => undefined;
      leer.mockReturnValue(new Promise((resuelve) => (terminar = resuelve)));
      elegirTipoArchivo();
      const lectura = elegir();
      fixture.detectChanges();

      expect(estado()).toBe('Leyendo el archivo…');
      expect(aplicar().disabled).toBe(true);

      terminar(leido);
      await lectura;
      await new Promise((listo) => setTimeout(listo));
      fixture.detectChanges();
      expect(aplicar().disabled).toBe(false);
    });

    it('si eligió otro archivo mientras leía, solo cuenta el último', async () => {
      let terminarPrimero: (archivo: ArchivoLeido) => void = () => undefined;
      leer.mockReturnValueOnce(new Promise((resuelve) => (terminarPrimero = resuelve)));
      elegirTipoArchivo();
      const primera = elegir('primero.kml');

      await elegir('segundo.kml');
      terminarPrimero({ ...leido, omitidos: 9 });
      await primera;

      expect(estado()).toBe('Se importará 1 elemento.');
      expect(nombre().value).toBe('segundo');
    });

    it('al quitar el archivo elegido deja de contar', async () => {
      elegirTipoArchivo();
      await elegir();

      Object.defineProperty(campoArchivo(), 'files', { configurable: true, value: [] });
      campoArchivo().dispatchEvent(new Event('change'));
      await fixture.whenStable();
      fixture.detectChanges();

      expect(estado()).toBe('');
    });

    describe('al soltar un archivo en la ventana', () => {
      const soltado = (nombreArchivo = 'Bodegas norte.kml') => ({ name: nombreArchivo }) as File;
      const zona = () => elemento.querySelector('.elegido')?.textContent?.trim();

      async function recibir(...archivos: File[]): Promise<void> {
        fixture.componentInstance.recibirArchivos(archivos);
        await fixture.whenStable();
        fixture.detectChanges();
      }

      beforeEach(() => fixture.componentInstance['cerrar']());

      it('abre el formulario con el archivo leído y su nombre a la vista', async () => {
        await recibir(soltado());

        expect(dialogo().open).toBe(true);
        expect(opcion('ARCHIVO').checked).toBe(true);
        expect(estado()).toBe('Se importará 1 elemento.');
        expect(zona()).toBe('Bodegas norte.kml');
        expect(nombre().value).toBe('Bodegas norte');
        expect(document.activeElement).toBe(nombre());
      });

      it('con el formulario abierto conserva lo escrito', async () => {
        abrir();
        escribir(nombre(), 'Mi capa');
        escribir(descripcion(), 'Región');

        await recibir(soltado());

        expect(opcion('ARCHIVO').checked).toBe(true);
        expect(nombre().value).toBe('Mi capa');
        expect(descripcion().value).toBe('Región');
      });

      it('un segundo archivo reemplaza el nombre sugerido, pero no el escrito', async () => {
        await recibir(soltado('Primero.kml'));
        await recibir(soltado('Segundo.kml'));
        expect(nombre().value).toBe('Segundo');

        escribir(nombre(), 'Mi capa');
        await recibir(soltado('Tercero.kml'));
        expect(nombre().value).toBe('Mi capa');
      });

      it('la sugerencia de un archivo anterior no sobrevive a cerrar el formulario', async () => {
        await recibir(soltado('Primero.kml'));
        abrir();
        escribir(nombre(), 'Primero');

        await recibir(soltado('Segundo.kml'));

        expect(nombre().value).toBe('Primero');
      });

      it('varios archivos a la vez se rechazan, sin elegir uno por su cuenta', async () => {
        await recibir(soltado('Primero.kml'), soltado('Segundo.kml'));

        expect(alerta()).toBe('Arrastre un solo archivo.');
        expect(leer).not.toHaveBeenCalled();
        expect(zona()).toBe('o arrástrelo hasta aquí');
      });

      it('un archivo con un formato que no se puede leer lo explica en el formulario', async () => {
        leer.mockRejectedValue(new ArchivoInvalido('El formato del archivo no está disponible.'));

        await recibir(soltado('bodegas.shp'));

        expect(alerta()).toBe('El formato del archivo no está disponible.');
        expect(zona()).toBe('bodegas.shp');
      });

      it('mientras crea la capa no acepta otro archivo', async () => {
        const respuesta = new Subject<CapaPropia>();
        importar.mockReturnValue(respuesta);
        await recibir(soltado('Primero.kml'));
        enviar();

        await recibir(soltado('Segundo.kml'));

        expect(leer).toHaveBeenCalledTimes(1);
        expect(zona()).toBe('Primero.kml');
      });

      it('elegir después el mismo archivo del campo vuelve a contar, porque el campo queda vacío', async () => {
        abrir();
        elegirTipoArchivo();
        await elegir('Elegido.kml');

        expect(campoArchivo().value).toBe('');
        expect(zona()).toBe('Elegido.kml');
      });
    });

    it('volver a capa en blanco olvida el archivo', async () => {
      elegirTipoArchivo();
      await elegir();

      opcion('BLANCO').click();
      fixture.detectChanges();
      enviar();

      expect(crear).toHaveBeenCalledWith({ nombre: 'Bodegas norte', descripcion: '' });
      expect(importar).not.toHaveBeenCalled();
    });

    it('un rechazo del backend muestra su mensaje y queda abierto', async () => {
      importar.mockReturnValue(throwError(() => new CambiosRechazados('No se pueden importar más de 10000 elementos.')));
      elegirTipoArchivo();
      await elegir();

      enviar();

      expect(elemento.querySelector('form > [role="alert"]')?.textContent).toBe('No se pueden importar más de 10000 elementos.');
      expect(dialogo().open).toBe(true);
    });
  });

  it('Cancelar cierra sin crear y al volver a abrir parte de nuevo', () => {
    abrir();
    escribir(nombre(), 'ab');
    enviar();

    (elemento.querySelector('.botones button[type="button"]') as HTMLButtonElement).click();
    expect(dialogo().open).toBe(false);
    expect(crear).not.toHaveBeenCalled();

    abrir();
    expect(nombre().value).toBe('');
    expect(errores()).toEqual([]);
  });
});

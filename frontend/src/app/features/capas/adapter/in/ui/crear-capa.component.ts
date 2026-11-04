import { ChangeDetectionStrategy, Component, computed, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { ArchivosSoltados } from '../../../../../shared/archivos-soltados/archivos-soltados.service';
import { LectorArchivos } from '../../../application/lector-archivos';
import { MisCapas } from '../../../application/mis-capas';
import { ArchivoInvalido, ArchivoLeido, esTabla, EXTENSIONES_ARCHIVO, nombreSugerido } from '../../../domain/archivo-geografico';
import {
  CambiosRechazados,
  CapaPropia,
  errorDeDescripcion,
  errorDeNombre,
  errorDeUrl,
  MAXIMO_DESCRIPCION,
  MAXIMO_NOMBRE,
  MAXIMO_URL
} from '../../../domain/capa';
import { columnasSugeridas, convertirTabla, TablaLeida, tipoSugerido, TIPOS_UBICACION, TipoUbicacion } from '../../../domain/tabla-geografica';

type TipoNuevaCapa = 'BLANCO' | 'ARCHIVO' | 'URL';

/** "Nueva capa" de SIATE: en blanco, desde un archivo o desde la URL de un servidor de mapas, con nombre y descripción, en
 * un solo diálogo en vez de sus tres pasos. */
@Component({
  selector: 'app-crear-capa',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './crear-capa.component.html',
  styleUrls: ['./editar-capa.component.css', './crear-capa.component.css']
})
export class CrearCapaComponent {
  /** La capa recién creada, para abrirla: en SIATE se abre al terminar el asistente. */
  readonly creada = output<CapaPropia>();
  /** Lo mismo, para una capa que ya trae contenido, de un archivo o de un servidor: se abre para verlo. */
  readonly importada = output<CapaPropia>();

  private readonly misCapas = inject(MisCapas);
  private readonly lector = inject(LectorArchivos);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private readonly campoNombre = viewChild.required<ElementRef<HTMLInputElement>>('campoNombre');
  /** La lectura en curso: si eligió otro archivo mientras tanto, la anterior ya no cuenta. */
  private lecturaActual = 0;
  private ultimoSugerido = '';

  protected readonly soltados = inject(ArchivosSoltados);
  protected readonly extensiones = EXTENSIONES_ARCHIVO.join(',');
  protected readonly tipo = signal<TipoNuevaCapa>('BLANCO');
  protected readonly leyendo = signal(false);
  protected readonly archivo = signal<ArchivoLeido | null>(null);
  /** Un Excel o un CSV: el archivo queda listo recién al saber en qué columnas está la ubicación. */
  protected readonly tabla = signal<TablaLeida | null>(null);
  protected readonly tiposUbicacion = TIPOS_UBICACION;
  protected readonly tipoUbicacion = signal<TipoUbicacion>('GEODESICAS');
  protected readonly columnas = signal<number[]>([]);
  protected readonly columnasDelTipo = computed(() => TIPOS_UBICACION.find(({ id }) => id === this.tipoUbicacion())!.columnas);
  /** El campo de archivo se vacía después de cada elección, y un archivo soltado nunca pasa por él. */
  protected readonly nombreArchivo = signal('');
  protected readonly errorArchivo = signal('');
  protected readonly maximoNombre = MAXIMO_NOMBRE;
  protected readonly maximoUrl = MAXIMO_URL;
  protected readonly maximoDescripcion = MAXIMO_DESCRIPCION;
  protected readonly nombre = signal('');
  protected readonly descripcion = signal('');
  protected readonly url = signal('');
  /** Los errores se muestran al salir de un campo o al intentar crear, no mientras se escribe por primera vez. */
  protected readonly mostrarErrores = signal(false);
  protected readonly enviando = signal(false);
  protected readonly error = signal('');
  protected readonly errorNombre = computed(() => errorDeNombre(this.nombre()));
  protected readonly errorDescripcion = computed(() => errorDeDescripcion(this.descripcion()));
  protected readonly errorUrl = computed(() => errorDeUrl(this.url()));

  abrir(): void {
    this.ultimoSugerido = '';
    this.nombre.set('');
    this.descripcion.set('');
    this.url.set('');
    this.mostrarErrores.set(false);
    this.error.set('');
    this.elegirTipo('BLANCO');
    this.dialogo().nativeElement.showModal();
    // El diálogo enfoca lo primero que encuentra, la ×: un Enter lo cerraría en vez de crear.
    this.campoNombre().nativeElement.focus();
  }

  /** Un archivo soltado en la ventana. Si ya estaba llenando el formulario, conserva lo escrito. */
  recibirArchivos(archivos: readonly File[]): void {
    if (this.enviando()) {
      return;
    }
    if (!this.dialogo().nativeElement.open) {
      this.abrir();
    }
    this.elegirTipo('ARCHIVO');
    if (archivos.length > 1) {
      this.errorArchivo.set('Arrastre un solo archivo.');
    } else {
      void this.leer(archivos[0]);
    }
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  protected texto(evento: Event): string {
    return (evento.target as HTMLInputElement | HTMLTextAreaElement).value;
  }

  protected elegirTipo(tipo: TipoNuevaCapa): void {
    this.tipo.set(tipo);
    this.lecturaActual++;
    this.leyendo.set(false);
    this.archivo.set(null);
    this.tabla.set(null);
    this.errorArchivo.set('');
    this.nombreArchivo.set('');
  }

  protected elegirArchivo(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const elegido = campo.files?.[0];
    // El navegador no avisa si vuelve a elegir el mismo, por ejemplo después de soltar otro.
    campo.value = '';
    void this.leer(elegido);
  }

  protected elegirTipoUbicacion(evento: Event): void {
    const tabla = this.tabla();
    const tipo = (evento.target as HTMLSelectElement).value as TipoUbicacion;
    if (tabla) {
      this.tipoUbicacion.set(tipo);
      this.columnas.set(columnasSugeridas(tabla, tipo));
      this.convertir();
    }
  }

  protected elegirColumna(indice: number, evento: Event): void {
    const columna = Number((evento.target as HTMLSelectElement).value);
    this.columnas.update((columnas) => columnas.map((actual, i) => (i === indice ? columna : actual)));
    this.convertir();
  }

  private usarTabla(tabla: TablaLeida): void {
    const tipo = tipoSugerido(tabla);
    this.tabla.set(tabla);
    this.tipoUbicacion.set(tipo);
    this.columnas.set(columnasSugeridas(tabla, tipo));
    this.convertir();
  }

  /** Hasta elegir todas las columnas no hay nada que avisar: el formulario pide las que faltan al crear. */
  private convertir(): void {
    const tabla = this.tabla();
    this.archivo.set(null);
    this.errorArchivo.set('');
    if (tabla && !this.columnas().includes(-1)) {
      try {
        this.archivo.set(convertirTabla(tabla, this.tipoUbicacion(), this.columnas()));
      } catch (error) {
        this.errorArchivo.set(error instanceof ArchivoInvalido ? error.message : 'No se pudo leer el archivo.');
      }
    }
  }

  private async leer(elegido: File | undefined): Promise<void> {
    const lectura = ++this.lecturaActual;
    this.archivo.set(null);
    this.tabla.set(null);
    this.errorArchivo.set('');
    this.nombreArchivo.set(elegido?.name ?? '');
    if (!elegido) {
      this.leyendo.set(false);
      return;
    }
    this.leyendo.set(true);
    try {
      if (esTabla(elegido.name)) {
        const tabla = await this.lector.leerTabla(elegido);
        if (lectura === this.lecturaActual) {
          this.usarTabla(tabla);
          this.sugerirNombre(nombreSugerido(elegido.name, MAXIMO_NOMBRE));
        }
      } else {
        const leido = await this.lector.leer(elegido);
        if (lectura === this.lecturaActual) {
          this.archivo.set(leido);
          this.sugerirNombre(nombreSugerido(elegido.name, MAXIMO_NOMBRE));
        }
      }
    } catch (error) {
      if (lectura === this.lecturaActual) {
        this.errorArchivo.set(error instanceof ArchivoInvalido ? error.message : 'No se pudo leer el archivo.');
      }
    } finally {
      if (lectura === this.lecturaActual) {
        this.leyendo.set(false);
      }
    }
  }

  /** Como punto de partida: no reemplaza lo que escribió, pero sí lo que se sugirió por un archivo anterior. */
  private sugerirNombre(sugerido: string): void {
    const actual = this.nombre();
    if (!actual.trim() || actual === this.ultimoSugerido) {
      this.nombre.set(sugerido);
    }
    this.ultimoSugerido = sugerido;
  }

  protected guardar(evento: Event): void {
    evento.preventDefault();
    this.mostrarErrores.set(true);
    const archivo = this.archivo();
    const faltaArchivo = this.tipo() === 'ARCHIVO' && !archivo;
    const porUrl = this.tipo() === 'URL';
    if (faltaArchivo && !this.errorArchivo() && !this.leyendo()) {
      this.errorArchivo.set(this.tabla() ? 'Elija las columnas con la ubicación.' : 'Seleccione un archivo.');
    }
    if (this.errorNombre() || this.errorDescripcion() || faltaArchivo || (porUrl && this.errorUrl()) || this.enviando()) {
      return;
    }
    const nueva = { nombre: this.nombre(), descripcion: this.descripcion() };
    this.enviando.set(true);
    this.error.set('');
    const creacion = archivo
      ? this.misCapas.importar(nueva, archivo)
      : porUrl
        ? this.misCapas.crearPorUrl(nueva, this.url())
        : this.misCapas.crear(nueva);
    creacion.subscribe({
      next: (capa) => {
        this.enviando.set(false);
        this.cerrar();
        (archivo || porUrl ? this.importada : this.creada).emit(capa);
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.error.set(error instanceof CambiosRechazados ? error.message : 'No se pudo crear la capa. Intente nuevamente.');
      }
    });
  }
}

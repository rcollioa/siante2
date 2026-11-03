import { ChangeDetectionStrategy, Component, computed, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { ArchivosSoltados } from '../../../../../shared/archivos-soltados/archivos-soltados.service';
import { LectorArchivos } from '../../../application/lector-archivos';
import { MisCapas } from '../../../application/mis-capas';
import { ArchivoInvalido, ArchivoLeido, EXTENSIONES_ARCHIVO, nombreSugerido } from '../../../domain/archivo-geografico';
import { CambiosRechazados, CapaPropia, errorDeDescripcion, errorDeNombre, MAXIMO_DESCRIPCION, MAXIMO_NOMBRE } from '../../../domain/capa';

/** "Nueva capa" de SIATE: en blanco o desde un archivo, con nombre y descripción, en un solo diálogo en vez de sus tres
 * pasos. La opción URL llegará aquí mismo. */
@Component({
  selector: 'app-crear-capa',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './crear-capa.component.html',
  styleUrls: ['./editar-capa.component.css', './crear-capa.component.css']
})
export class CrearCapaComponent {
  /** La capa recién creada, para abrirla: en SIATE se abre al terminar el asistente. */
  readonly creada = output<CapaPropia>();
  /** Lo mismo, para una capa que ya trae elementos: se abre para verlos. */
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
  protected readonly tipo = signal<'BLANCO' | 'ARCHIVO'>('BLANCO');
  protected readonly leyendo = signal(false);
  protected readonly archivo = signal<ArchivoLeido | null>(null);
  /** El campo de archivo se vacía después de cada elección, y un archivo soltado nunca pasa por él. */
  protected readonly nombreArchivo = signal('');
  protected readonly errorArchivo = signal('');
  protected readonly maximoNombre = MAXIMO_NOMBRE;
  protected readonly maximoDescripcion = MAXIMO_DESCRIPCION;
  protected readonly nombre = signal('');
  protected readonly descripcion = signal('');
  /** Los errores se muestran al salir de un campo o al intentar crear, no mientras se escribe por primera vez. */
  protected readonly mostrarErrores = signal(false);
  protected readonly enviando = signal(false);
  protected readonly error = signal('');
  protected readonly errorNombre = computed(() => errorDeNombre(this.nombre()));
  protected readonly errorDescripcion = computed(() => errorDeDescripcion(this.descripcion()));

  abrir(): void {
    this.ultimoSugerido = '';
    this.nombre.set('');
    this.descripcion.set('');
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

  protected elegirTipo(tipo: 'BLANCO' | 'ARCHIVO'): void {
    this.tipo.set(tipo);
    this.lecturaActual++;
    this.leyendo.set(false);
    this.archivo.set(null);
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

  private async leer(elegido: File | undefined): Promise<void> {
    const lectura = ++this.lecturaActual;
    this.archivo.set(null);
    this.errorArchivo.set('');
    this.nombreArchivo.set(elegido?.name ?? '');
    if (!elegido) {
      this.leyendo.set(false);
      return;
    }
    this.leyendo.set(true);
    try {
      const leido = await this.lector.leer(elegido);
      if (lectura === this.lecturaActual) {
        this.archivo.set(leido);
        this.sugerirNombre(nombreSugerido(elegido.name, MAXIMO_NOMBRE));
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
    if (faltaArchivo && !this.errorArchivo() && !this.leyendo()) {
      this.errorArchivo.set('Seleccione un archivo.');
    }
    if (this.errorNombre() || this.errorDescripcion() || faltaArchivo || this.enviando()) {
      return;
    }
    const nueva = { nombre: this.nombre(), descripcion: this.descripcion() };
    this.enviando.set(true);
    this.error.set('');
    (archivo ? this.misCapas.importar(nueva, archivo) : this.misCapas.crear(nueva)).subscribe({
      next: (capa) => {
        this.enviando.set(false);
        this.cerrar();
        (archivo ? this.importada : this.creada).emit(capa);
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.error.set(error instanceof CambiosRechazados ? error.message : 'No se pudo crear la capa. Intente nuevamente.');
      }
    });
  }
}

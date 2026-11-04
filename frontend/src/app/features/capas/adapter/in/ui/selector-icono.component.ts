import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal, viewChild } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfirmacionComponent } from '../../../../../shared/confirmacion/confirmacion.component';
import { CatalogoIconos } from '../../../application/catalogo-iconos';
import { IconoLeido, LectorIconos } from '../../../application/lector-iconos';
import { CambiosRechazados } from '../../../domain/capa';
import { errorDeNombreIcono, esImagenIncrustada, ICONO_POR_DEFECTO, Icono, IconoInvalido, MAXIMO_NOMBRE_ICONO } from '../../../domain/icono';

/** "Iconos personalizados" de SIATE: un clic aplica el ícono al marcador seleccionado, que se guarda con los demás cambios.
 * El elemento se queda con una copia: cambiar o eliminar un ícono no altera los marcadores que ya lo usan. */
@Component({
  selector: 'app-selector-icono',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ConfirmacionComponent],
  templateUrl: './selector-icono.component.html',
  styleUrl: './selector-icono.component.css'
})
export class SelectorIconoComponent {
  private readonly lector = inject(LectorIconos);
  private readonly confirmacion = viewChild.required(ConfirmacionComponent);
  /** La lectura en curso: si eligió otro archivo mientras tanto, la anterior ya no cuenta. */
  private lecturaActual = 0;

  protected readonly catalogo = inject(CatalogoIconos);
  readonly icono = input.required<string>();
  readonly elegido = output<string>();

  protected readonly maximoNombre = MAXIMO_NOMBRE_ICONO;
  protected readonly esPorDefecto = computed(() => !esImagenIncrustada(this.icono()));
  /** Abierto para crear (null) o para editar ese ícono. */
  protected readonly formulario = signal<{ editando: Icono | null } | null>(null);
  protected readonly nombre = signal('');
  protected readonly leido = signal<IconoLeido | null>(null);
  protected readonly leyendo = signal(false);
  protected readonly errorArchivo = signal('');
  protected readonly mostrarErrores = signal(false);
  protected readonly enviando = signal(false);
  protected readonly error = signal('');
  protected readonly errorNombre = computed(() => errorDeNombreIcono(this.nombre()));

  constructor() {
    this.catalogo.cargar();
  }

  protected usarPorDefecto(): void {
    this.elegido.emit(ICONO_POR_DEFECTO);
  }

  protected abrirFormulario(editando: Icono | null): void {
    this.lecturaActual++;
    this.formulario.set({ editando });
    this.nombre.set(editando?.nombre ?? '');
    this.leido.set(null);
    this.leyendo.set(false);
    this.errorArchivo.set('');
    this.mostrarErrores.set(false);
    this.error.set('');
  }

  protected cerrarFormulario(): void {
    this.lecturaActual++;
    this.formulario.set(null);
  }

  protected escribirNombre(evento: Event): void {
    this.nombre.set((evento.target as HTMLInputElement).value);
  }

  protected async elegirArchivo(evento: Event): Promise<void> {
    const campo = evento.target as HTMLInputElement;
    const archivo = campo.files?.[0];
    // El navegador no avisa si vuelve a elegir el mismo después de un error.
    campo.value = '';
    if (!archivo) {
      return;
    }
    const lectura = ++this.lecturaActual;
    this.leido.set(null);
    this.errorArchivo.set('');
    this.leyendo.set(true);
    try {
      const leido = await this.lector.leer(archivo);
      if (lectura === this.lecturaActual) {
        this.leido.set(leido);
      }
    } catch (error) {
      if (lectura === this.lecturaActual) {
        this.errorArchivo.set(error instanceof IconoInvalido ? error.message : 'Este tipo de archivo presenta problemas de lectura.');
      }
    } finally {
      if (lectura === this.lecturaActual) {
        this.leyendo.set(false);
      }
    }
  }

  protected guardar(evento: Event): void {
    evento.preventDefault();
    const abierto = this.formulario();
    if (!abierto || this.enviando()) {
      return;
    }
    this.mostrarErrores.set(true);
    const { editando } = abierto;
    const leido = this.leido();
    const imagen = leido ?? editando;
    if (!imagen && !this.errorArchivo() && !this.leyendo()) {
      this.errorArchivo.set('Seleccione un archivo SVG o PNG.');
    }
    if (this.errorNombre() || !imagen) {
      return;
    }
    const datos = { nombre: this.nombre(), archivo: imagen.archivo, imagen: imagen.imagen };
    this.enviando.set(true);
    this.error.set('');
    const pedido: Observable<unknown> = editando ? this.catalogo.editar(editando.id, datos) : this.catalogo.crear(datos);
    pedido.subscribe({
      next: () => {
        this.enviando.set(false);
        this.cerrarFormulario();
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.error.set(mensajeDeError(error));
      }
    });
  }

  protected preguntarEliminacion(icono: Icono): void {
    this.confirmacion().preguntar({
      mensaje: '¿Confirma el borrado del ícono?',
      detalle: icono.nombre,
      accion: () => this.catalogo.eliminar(icono.id),
      mensajeDeError
    });
  }
}

function mensajeDeError(error: unknown): string {
  return error instanceof CambiosRechazados ? error.message : 'No se pudo guardar el ícono. Intente nuevamente.';
}

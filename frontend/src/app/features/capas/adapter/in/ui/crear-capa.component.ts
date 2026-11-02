import { ChangeDetectionStrategy, Component, computed, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { MisCapas } from '../../../application/mis-capas';
import { CapaPropia, errorDeDescripcion, errorDeNombre, MAXIMO_DESCRIPCION, MAXIMO_NOMBRE } from '../../../domain/capa';

/** "Nueva capa" de SIATE, por ahora solo la opción "Capa en blanco": nombre y descripción. Las opciones Archivo y URL
 * llegarán a este mismo diálogo. */
@Component({
  selector: 'app-crear-capa',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './crear-capa.component.html',
  styleUrl: './editar-capa.component.css'
})
export class CrearCapaComponent {
  /** La capa recién creada, para abrirla: en SIATE se abre al terminar el asistente. */
  readonly creada = output<CapaPropia>();

  private readonly misCapas = inject(MisCapas);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private readonly campoNombre = viewChild.required<ElementRef<HTMLInputElement>>('campoNombre');

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
    this.nombre.set('');
    this.descripcion.set('');
    this.mostrarErrores.set(false);
    this.error.set('');
    this.dialogo().nativeElement.showModal();
    // El diálogo enfoca lo primero que encuentra, la ×: un Enter lo cerraría en vez de crear.
    this.campoNombre().nativeElement.focus();
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  protected texto(evento: Event): string {
    return (evento.target as HTMLInputElement | HTMLTextAreaElement).value;
  }

  protected guardar(evento: Event): void {
    evento.preventDefault();
    this.mostrarErrores.set(true);
    if (this.errorNombre() || this.errorDescripcion() || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.error.set('');
    this.misCapas.crear({ nombre: this.nombre(), descripcion: this.descripcion() }).subscribe({
      next: (capa) => {
        this.enviando.set(false);
        this.cerrar();
        this.creada.emit(capa);
      },
      error: () => {
        this.enviando.set(false);
        this.error.set('No se pudo crear la capa. Intente nuevamente.');
      }
    });
  }
}

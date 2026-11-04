import { ChangeDetectionStrategy, Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { Categoria, CategoriasApiService } from '../../../../categorias/categorias-api.service';
import { mensajeDeEscritura, MisCapas } from '../../../application/mis-capas';
import {
  CapaPropia,
  EdicionCapa,
  erroresDeEdicion,
  MAXIMO_DESCRIPCION,
  MAXIMO_NOMBRE,
  VisibilidadEditable
} from '../../../domain/capa';

/** El formulario "Editar capa" de SIATE en un diálogo. Con signals y sin `@angular/forms`, que hoy no está en la
 * aplicación y sumaría decenas de KB a lo que se descarga al abrir "Mis capas". */
@Component({
  selector: 'app-editar-capa',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './editar-capa.component.html',
  styleUrl: './editar-capa.component.css'
})
export class EditarCapaComponent {
  private readonly misCapas = inject(MisCapas);
  private readonly categoriasApi = inject(CategoriasApiService);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private readonly campoNombre = viewChild.required<ElementRef<HTMLInputElement>>('campoNombre');
  private capaId = 0;
  private original: EdicionCapa | null = null;

  protected readonly maximoNombre = MAXIMO_NOMBRE;
  protected readonly maximoDescripcion = MAXIMO_DESCRIPCION;
  protected readonly nombre = signal('');
  protected readonly descripcion = signal('');
  protected readonly categoriaId = signal(0);
  protected readonly visibilidad = signal<VisibilidadEditable>('PRIVADA');
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly sinCategorias = signal(false);
  /** Los errores se muestran al salir de un campo o al intentar guardar, no mientras se escribe por primera vez. */
  protected readonly mostrarErrores = signal(false);
  protected readonly enviando = signal(false);
  protected readonly error = signal('');

  private readonly edicion = computed<EdicionCapa>(() => ({
    nombre: this.nombre(),
    descripcion: this.descripcion(),
    categoriaId: this.categoriaId(),
    visibilidad: this.visibilidad()
  }));
  protected readonly errores = computed(() => erroresDeEdicion(this.edicion()));

  abrir(capa: CapaPropia): void {
    const visibilidad = capa.visibilidad === 'PUBLICA' ? 'PUBLICA' : 'PRIVADA';
    this.capaId = capa.id;
    this.original = { nombre: capa.nombre, descripcion: capa.descripcion, categoriaId: capa.categoriaId, visibilidad };
    this.nombre.set(capa.nombre);
    this.descripcion.set(capa.descripcion);
    this.categoriaId.set(capa.categoriaId);
    this.visibilidad.set(visibilidad);
    this.mostrarErrores.set(false);
    this.error.set('');
    this.cargarCategorias();
    this.dialogo().nativeElement.showModal();
    // El diálogo enfoca lo primero que encuentra, la ×: un Enter lo cerraría en vez de guardar.
    this.campoNombre().nativeElement.focus();
  }

  protected cargarCategorias(): void {
    this.sinCategorias.set(false);
    this.categoriasApi.listar().subscribe({
      next: (categorias) => {
        this.categorias.set(categorias);
        // Una categoría que ya no está activa no se puede guardar: hay que elegir otra.
        if (!categorias.some(({ id }) => id === this.categoriaId())) {
          this.categoriaId.set(0);
        }
      },
      error: () => this.sinCategorias.set(true)
    });
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  protected texto(evento: Event): string {
    return (evento.target as HTMLInputElement | HTMLTextAreaElement).value;
  }

  protected elegirCategoria(evento: Event): void {
    this.categoriaId.set(Number((evento.target as HTMLSelectElement).value));
  }

  /** Como SIATE, si nada cambió cierra sin escribir. */
  protected guardar(evento: Event): void {
    evento.preventDefault();
    this.mostrarErrores.set(true);
    const { nombre, descripcion, categoria } = this.errores();
    if (nombre || descripcion || categoria || this.sinCategorias() || this.enviando()) {
      return;
    }
    if (!this.hayCambios()) {
      this.cerrar();
      return;
    }
    this.enviando.set(true);
    this.error.set('');
    this.misCapas.editar(this.capaId, this.edicion()).subscribe({
      next: () => {
        this.enviando.set(false);
        this.cerrar();
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.error.set(mensajeDeEscritura(error));
      }
    });
  }

  private hayCambios(): boolean {
    const { nombre, descripcion, categoriaId, visibilidad } = this.edicion();
    const original = this.original;
    return (
      !original ||
      nombre.trim() !== original.nombre ||
      descripcion.trim() !== original.descripcion ||
      categoriaId !== original.categoriaId ||
      visibilidad !== original.visibilidad
    );
  }
}

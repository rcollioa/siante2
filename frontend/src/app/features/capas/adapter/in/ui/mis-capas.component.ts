import { afterRenderEffect, ChangeDetectionStrategy, Component, inject, untracked, viewChild } from '@angular/core';
import { ArchivosSoltados } from '../../../../../shared/archivos-soltados/archivos-soltados.service';
import { ConfirmacionComponent } from '../../../../../shared/confirmacion/confirmacion.component';
import { EdicionElementos } from '../../../application/edicion-elementos';
import { mensajeDeEscritura, MisCapas } from '../../../application/mis-capas';
import { CapaPropia, VisibilidadEditable } from '../../../domain/capa';
import { CrearCapaComponent } from './crear-capa.component';
import { EditarCapaComponent } from './editar-capa.component';
import { ListaCapasComponent } from './lista-capas.component';

/** "Mis capas" en el menú: las capas propias del usuario, que como en SIATE puede crear, editar, publicar y eliminar. */
@Component({
  selector: 'app-mis-capas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ListaCapasComponent, CrearCapaComponent, EditarCapaComponent, ConfirmacionComponent],
  template: `
    <button type="button" class="crear" (click)="creador().abrir()">Crear capa</button>
    <app-lista-capas
      [capas]="misCapas.capas()"
      [estado]="misCapas.estado()"
      descripcion="sus capas"
      vacia="Aún no tiene capas."
      (reintentar)="misCapas.cargar()">
      <ng-template #gestion let-capa>
        <button type="button" class="opcion" (click)="dibujar(capa)">Dibujar</button>
        <button type="button" class="opcion" (click)="editor().abrir(capa)">Editar</button>
        <button type="button" class="opcion" (click)="preguntarVisibilidad(capa)">
          {{ capa.visibilidad === 'PUBLICA' ? 'Despublicar' : 'Publicar' }}
        </button>
        <button type="button" class="opcion peligro" (click)="preguntarEliminacion(capa)">Eliminar</button>
      </ng-template>
    </app-lista-capas>
    <app-crear-capa (creada)="dibujar($event)" (importada)="lista().abrir($event)" />
    <app-editar-capa />
    <app-confirmacion />
  `,
  styles: `
    .crear {
      margin: 0.25rem 0.75rem 0.35rem;
      padding: 0;
      border: 0;
      background: transparent;
      color: var(--color-primary-ink);
      font: inherit;
      font-size: 0.875rem;
      text-decoration: underline;
      cursor: pointer;
    }

    .crear:focus-visible {
      outline: 2px solid var(--color-primary);
      outline-offset: 2px;
    }

    .opcion {
      padding: 0.2rem 0.6rem;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      background: var(--color-surface);
      color: var(--color-ink);
      font: inherit;
      font-size: 0.8rem;
      cursor: pointer;
    }

    .opcion:hover {
      background: var(--color-hover);
    }

    .peligro {
      color: var(--color-danger);
    }

    .opcion:focus-visible {
      outline: 2px solid var(--color-primary);
      outline-offset: 1px;
    }
  `
})
export class MisCapasComponent {
  protected readonly misCapas = inject(MisCapas);
  private readonly edicion = inject(EdicionElementos);
  private readonly soltados = inject(ArchivosSoltados);
  protected readonly lista = viewChild.required(ListaCapasComponent);
  protected readonly creador = viewChild.required(CrearCapaComponent);
  protected readonly editor = viewChild.required(EditarCapaComponent);
  private readonly confirmacion = viewChild.required(ConfirmacionComponent);

  constructor() {
    this.misCapas.cargar();
    // Después de dibujar: si el archivo fue lo que desplegó este panel, el formulario recién existe en ese momento.
    afterRenderEffect(() => {
      const archivos = this.soltados.pendientes();
      if (archivos.length > 0) {
        untracked(() => {
          this.soltados.pendientes.set([]);
          this.creador().recibirArchivos(archivos);
        });
      }
    });
  }

  /** La capa se abre si hace falta y queda lista para dibujar. Una recién creada, como en SIATE, ya está en este modo. */
  protected dibujar(capa: CapaPropia): void {
    this.lista().irAlMapa();
    this.edicion.iniciar(capa.id);
  }

  protected preguntarVisibilidad(capa: CapaPropia): void {
    const publicar = capa.visibilidad !== 'PUBLICA';
    const visibilidad: VisibilidadEditable = publicar ? 'PUBLICA' : 'PRIVADA';
    this.confirmacion().preguntar({
      mensaje: publicar ? '¿Desea publicar la capa?' : '¿Desea despublicar la capa?',
      detalle: capa.nombre,
      accion: () => this.misCapas.cambiarVisibilidad(capa.id, visibilidad),
      mensajeDeError: mensajeDeEscritura
    });
  }

  protected preguntarEliminacion(capa: CapaPropia): void {
    this.confirmacion().preguntar({
      mensaje: '¿Confirma el borrado de la capa?',
      detalle: capa.nombre,
      accion: () => this.misCapas.eliminar(capa.id),
      mensajeDeError: mensajeDeEscritura
    });
  }
}

import { afterRenderEffect, ChangeDetectionStrategy, Component, inject, untracked, viewChild } from '@angular/core';
import { of } from 'rxjs';
import { ConfirmacionComponent } from '../../../../../shared/confirmacion/confirmacion.component';
import { HerramientaDibujo } from '../../../../mapa/domain/mapa';
import { EdicionElementos } from '../../../application/edicion-elementos';

/** Lo que indica cada herramienta, porque dibujar una línea y un círculo no se hace igual. */
const INDICACIONES: Record<HerramientaDibujo, string> = {
  Point: 'Haga clic en el mapa para poner el marcador.',
  LineString: 'Haga clic en cada punto de la línea y doble clic para terminarla.',
  Box: 'Haga clic en una esquina y otro clic en la esquina opuesta.',
  Polygon: 'Haga clic en cada vértice y doble clic para cerrar el polígono.',
  Circle: 'Haga clic en el centro y otro clic para definir el radio.'
};

/** La barra de dibujo de SIATE, sobre el mapa mientras se edita una capa. Esc suelta la herramienta. */
@Component({
  selector: 'app-barra-dibujo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ConfirmacionComponent],
  templateUrl: './barra-dibujo.component.html',
  styleUrl: './barra-dibujo.component.css',
  host: { '(document:keydown.escape)': 'edicion.soltarHerramienta()', '(window:beforeunload)': 'avisarAlSalir($event)' }
})
export class BarraDibujoComponent {
  protected readonly edicion = inject(EdicionElementos);
  protected readonly indicaciones = INDICACIONES;
  protected readonly herramientas: readonly { id: HerramientaDibujo; nombre: string }[] = [
    { id: 'Point', nombre: 'Marcador' },
    { id: 'LineString', nombre: 'Línea' },
    { id: 'Box', nombre: 'Cuadro' },
    { id: 'Polygon', nombre: 'Polígono' },
    { id: 'Circle', nombre: 'Círculo' }
  ];
  private readonly confirmacion = viewChild.required(ConfirmacionComponent);

  constructor() {
    // Después de pintar: el pedido puede venir de otra página, y esta barra recién se creó al volver al mapa.
    afterRenderEffect(() => {
      if (this.edicion.salidaPedida()) {
        untracked(() => this.preguntarSiDescarta());
      }
    });
  }

  protected avisarAlSalir(evento: BeforeUnloadEvent): void {
    if (this.edicion.hayCambios()) {
      evento.preventDefault();
    }
  }

  private preguntarSiDescarta(): void {
    this.confirmacion().preguntar({
      mensaje: 'Hay cambios sin guardar en la capa. ¿Desea descartarlos?',
      detalle: this.edicion.capa()?.nombre ?? '',
      accion: () => {
        this.edicion.descartarYSalir();
        return of(null);
      },
      mensajeDeError: () => ''
    });
  }
}

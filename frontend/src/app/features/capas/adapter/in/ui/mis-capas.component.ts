import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MisCapas } from '../../../application/mis-capas';
import { ListaCapasComponent } from './lista-capas.component';

/** "Mis capas" en el menú: las capas propias del usuario. */
@Component({
  selector: 'app-mis-capas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ListaCapasComponent],
  template: `<app-lista-capas
    [capas]="misCapas.capas()"
    [estado]="misCapas.estado()"
    descripcion="sus capas"
    vacia="Aún no tiene capas."
    (reintentar)="misCapas.cargar()" />`
})
export class MisCapasComponent {
  protected readonly misCapas = inject(MisCapas);

  constructor() {
    this.misCapas.cargar();
  }
}

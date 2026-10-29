import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PantallaCargaService } from './pantalla-carga.service';

@Component({
  selector: 'app-pantalla-carga',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pantalla-carga.component.html',
  styleUrl: './pantalla-carga.component.css'
})
export class PantallaCargaComponent {
  private readonly pantallaCarga = inject(PantallaCargaService);
  protected readonly visible = this.pantallaCarga.visible;
  protected readonly mensaje = this.pantallaCarga.mensaje;
}

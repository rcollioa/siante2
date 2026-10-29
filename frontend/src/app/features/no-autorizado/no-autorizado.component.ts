import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { SesionService } from '../../shared/sesion/sesion.service';

@Component({
  selector: 'app-no-autorizado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './no-autorizado.component.html',
  styleUrl: './no-autorizado.component.css'
})
export class NoAutorizadoComponent {
  protected readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  protected reintentar(): void {
    this.router.navigateByUrl('/');
  }
}

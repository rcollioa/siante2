import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { PantallaCargaComponent } from './shared/pantalla-carga/pantalla-carga.component';
import { SesionService } from './shared/sesion/sesion.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, PantallaCargaComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  protected readonly titulo = 'SIANTE';
  protected readonly sesion = inject(SesionService);
}

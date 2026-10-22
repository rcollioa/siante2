import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MenuLateralComponent } from './shared/menu-lateral/menu-lateral.component';
import { PantallaCargaComponent } from './shared/pantalla-carga/pantalla-carga.component';
import { SesionService } from './shared/sesion/sesion.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, PantallaCargaComponent, MenuLateralComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  protected readonly titulo = 'SIANTE';
  protected readonly sesion = inject(SesionService);
}

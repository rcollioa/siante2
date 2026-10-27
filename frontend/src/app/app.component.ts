import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { GuardsCheckEnd, NavigationCancel, NavigationError, Router, RouterOutlet } from '@angular/router';
import { first, map } from 'rxjs';
import { MenuLateralComponent } from './shared/menu-lateral/menu-lateral.component';
import { PantallaCargaComponent } from './shared/pantalla-carga/pantalla-carga.component';
import { SesionService } from './shared/sesion/sesion.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, PantallaCargaComponent, MenuLateralComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  protected readonly sesion = inject(SesionService);
  /** Antes de revisar las guardas de la primera ruta el contenido está vacío, pero se dibuja a todo el ancho:
   * si se viera, el menú lo empujaría a la derecha al aparecer (salto de diseño). */
  protected readonly rutaInicialRevisada = toSignal(
    inject(Router).events.pipe(
      first((e) => e instanceof GuardsCheckEnd || e instanceof NavigationCancel || e instanceof NavigationError),
      map(() => true)
    ),
    { initialValue: false }
  );
}

import { DatePipe, NgComponentOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, DOCUMENT, inject, signal, Type } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { catchError, of } from 'rxjs';
import { SesionService } from '../sesion/sesion.service';
import { VersionApiService } from '../version/version-api.service';
import { MenuLateralService } from './menu-lateral.service';
import { Seccion, SECCIONES } from './secciones';

@Component({
  selector: 'app-menu-lateral',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, NgComponentOutlet, DatePipe],
  templateUrl: './menu-lateral.component.html',
  styleUrl: './menu-lateral.component.css'
})
export class MenuLateralComponent {
  protected readonly sesion = inject(SesionService);
  protected readonly secciones = SECCIONES;
  private readonly menu = inject(MenuLateralService);
  protected readonly abierto = this.menu.abierto;
  /** Secciones desplegadas y el componente de cada una, ya descargado. */
  protected readonly desplegadas = signal<ReadonlySet<string>>(new Set());
  protected readonly paneles = signal<Readonly<Record<string, Type<unknown>>>>({});
  /** Al pie del menú, como la página /version de SIATE. Si no llega, no se muestra: no es imprescindible. */
  protected readonly version = toSignal(inject(VersionApiService).obtener().pipe(catchError(() => of(null))), {
    initialValue: null
  });
  private readonly router = inject(Router);
  private readonly ventana = inject(DOCUMENT).defaultView;

  protected alNavegar(): void {
    this.menu.alElegir();
  }

  protected alternar(seccion: Seccion): void {
    const desplegar = !this.desplegadas().has(seccion.nombre);
    this.marcar(seccion.nombre, desplegar);
    if (desplegar && seccion.panel && !this.paneles()[seccion.nombre]) {
      seccion
        .panel()
        .then((componente) => this.paneles.update((paneles) => ({ ...paneles, [seccion.nombre]: componente })))
        // Sin conexión no se descarga: queda plegada para volver a intentarlo.
        .catch(() => this.marcar(seccion.nombre, false));
    }
  }

  /** Como SIATE, cierra la ventana. El navegador lo impide si no la abrió un script o ya tiene historial;
   * entonces queda a la vista la página de salida. No toca la sesión de WebSEAL, que es de otro sistema. */
  protected salir(): void {
    this.sesion.finalizar();
    this.router.navigateByUrl('/salida').then(() => this.ventana?.close());
  }

  private marcar(nombre: string, desplegada: boolean): void {
    this.desplegadas.update((actuales) => {
      const nuevas = new Set(actuales);
      if (desplegada) {
        nuevas.add(nombre);
      } else {
        nuevas.delete(nombre);
      }
      return nuevas;
    });
  }
}

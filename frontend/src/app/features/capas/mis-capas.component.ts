import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { MenuLateralService } from '../../shared/menu-lateral/menu-lateral.service';
import { CapaAbiertaService } from './capa-abierta.service';
import { CapasApiService, ResumenCapa } from './capas-api.service';

export const VISIBILIDADES: Record<ResumenCapa['visibilidad'], string> = {
  PRIVADA: 'Privada',
  PUBLICA: 'Pública',
  FORMAL: 'Formal',
  AREA_DE_DIBUJO: 'Área de dibujo'
};

/** Lista de "Mis capas" dentro del menú, como en SIATE: al elegir una se abre en el mapa. */
@Component({
  selector: 'app-mis-capas',
  standalone: true,
  templateUrl: './mis-capas.component.html',
  styleUrl: './mis-capas.component.css'
})
export class MisCapasComponent {
  private readonly api = inject(CapasApiService);
  private readonly router = inject(Router);
  private readonly menu = inject(MenuLateralService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly capaAbierta = inject(CapaAbiertaService);

  protected readonly estado = signal<'cargando' | 'listo' | 'error'>('cargando');
  protected readonly capas = signal<ResumenCapa[]>([]);
  protected readonly visibilidades = VISIBILIDADES;

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.estado.set('cargando');
    this.api
      .listarMias()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (capas) => {
          this.capas.set(capas);
          this.estado.set('listo');
        },
        error: () => this.estado.set('error')
      });
  }

  /** Elegir la capa abierta la cierra. Desde otra página (Ayuda), primero vuelve al mapa. */
  protected alternar(capa: ResumenCapa): void {
    if (this.capaAbierta.capa()?.id === capa.id) {
      this.capaAbierta.cerrar();
      return;
    }
    if (!this.router.url.startsWith('/mapa')) {
      void this.router.navigateByUrl('/mapa');
    }
    this.capaAbierta.abrir(capa.id);
    this.menu.alElegir();
  }
}

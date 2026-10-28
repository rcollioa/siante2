import { inject, Injectable, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { ResumenCapa } from '../domain/capa';
import { CapasRepositorio } from './port/out/capas.repositorio';

/** La lista de "Mis capas" del menú. */
@Injectable({ providedIn: 'root' })
export class MisCapas {
  private readonly repositorio = inject(CapasRepositorio);
  private pedido?: Subscription;
  private readonly estadoActual = signal<'cargando' | 'listo' | 'error'>('cargando');
  private readonly lista = signal<ResumenCapa[]>([]);

  readonly estado = this.estadoActual.asReadonly();
  readonly capas = this.lista.asReadonly();

  /** Cada vez pregunta a la API, por si las capas cambiaron (también desde SIATE). */
  cargar(): void {
    this.pedido?.unsubscribe();
    this.estadoActual.set('cargando');
    this.pedido = this.repositorio.listarMias().subscribe({
      next: (capas) => {
        this.lista.set(capas);
        this.estadoActual.set('listo');
      },
      error: () => this.estadoActual.set('error')
    });
  }
}

import { computed, inject, Injectable, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { ResumenCapa } from '../domain/capa';
import { CapasRepositorio } from './port/out/capas.repositorio';

/** Las secciones "Favoritos" y "Capas formales" del menú: una sola consulta, separada por visibilidad como en SIATE. */
@Injectable({ providedIn: 'root' })
export class Favoritas {
  private readonly repositorio = inject(CapasRepositorio);
  private pedido?: Subscription;
  private readonly estadoActual = signal<'cargando' | 'listo' | 'error'>('cargando');
  private readonly lista = signal<ResumenCapa[]>([]);

  readonly estado = this.estadoActual.asReadonly();
  /** "Favoritos": todas menos las formales. */
  readonly favoritas = computed(() => this.lista().filter(({ visibilidad }) => visibilidad !== 'FORMAL'));
  /** "Capas formales": las formales que el usuario eligió. */
  readonly formales = computed(() => this.lista().filter(({ visibilidad }) => visibilidad === 'FORMAL'));

  /** Cada vez pregunta a la API: el usuario pudo marcarlas en otra pestaña o en SIATE. */
  cargar(): void {
    this.pedido?.unsubscribe();
    this.estadoActual.set('cargando');
    this.pedido = this.repositorio.listarFavoritas().subscribe({
      next: (capas) => {
        this.lista.set(capas);
        this.estadoActual.set('listo');
      },
      error: () => this.estadoActual.set('error')
    });
  }

  /** El dueño la editó desde "Mis capas": sin esto, la lista quedaría con los datos anteriores hasta volver a abrirla. */
  actualizar(id: number, datos: Partial<ResumenCapa>): void {
    this.lista.update((capas) => capas.map((capa) => (capa.id === id ? { ...capa, ...datos } : capa)));
  }

  quitar(id: number): void {
    this.lista.update((capas) => capas.filter((capa) => capa.id !== id));
  }
}

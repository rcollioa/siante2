import { inject, Injectable, signal } from '@angular/core';
import { Observable, Subscription, tap } from 'rxjs';
import { CapaNoEncontrada, CapaPropia, EdicionCapa, NuevaCapa, ResumenCapa, VisibilidadEditable } from '../domain/capa';
import { CapasAbiertas } from './capas-abiertas';
import { Favoritas } from './favoritas';
import { CapasRepositorio } from './port/out/capas.repositorio';

/** La lista de "Mis capas" del menú y lo que su dueño puede hacer con cada capa. */
@Injectable({ providedIn: 'root' })
export class MisCapas {
  private readonly repositorio = inject(CapasRepositorio);
  private readonly capasAbiertas = inject(CapasAbiertas);
  private readonly favoritas = inject(Favoritas);
  private pedido?: Subscription;
  private readonly estadoActual = signal<'cargando' | 'listo' | 'error'>('cargando');
  private readonly lista = signal<CapaPropia[]>([]);

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

  /** Entra a la lista en su lugar por nombre, sin volver a pedirla. */
  crear(nueva: NuevaCapa): Observable<CapaPropia> {
    return this.repositorio
      .crear({ nombre: nueva.nombre.trim(), descripcion: nueva.descripcion.trim() })
      .pipe(tap((capa) => this.lista.update((capas) => porNombre([...capas, capa]))));
  }

  editar(id: number, edicion: EdicionCapa): Observable<void> {
    const limpia = { ...edicion, nombre: edicion.nombre.trim(), descripcion: edicion.descripcion.trim() };
    return this.escribir(this.repositorio.editar(id, limpia), () => {
      this.lista.update((capas) => porNombre(capas.map((capa) => (capa.id === id ? { ...capa, ...limpia } : capa))));
      const { nombre, descripcion, visibilidad } = limpia;
      this.actualizarEnLasDemasListas(id, { nombre, descripcion, visibilidad });
    });
  }

  cambiarVisibilidad(id: number, visibilidad: VisibilidadEditable): Observable<void> {
    return this.escribir(this.repositorio.cambiarVisibilidad(id, visibilidad), () => {
      this.lista.update((capas) => capas.map((capa) => (capa.id === id ? { ...capa, visibilidad } : capa)));
      this.actualizarEnLasDemasListas(id, { visibilidad });
    });
  }

  /** La capa también se cierra en el mapa y sale de las favoritas: ya no existe para nadie. */
  eliminar(id: number): Observable<void> {
    return this.escribir(this.repositorio.eliminar(id), () => {
      this.lista.update((capas) => capas.filter((capa) => capa.id !== id));
      this.capasAbiertas.cerrar(id);
      this.favoritas.quitar(id);
    });
  }

  /** Si ya no existe (la borraron en SIATE o en otra pestaña), la lista estaba desactualizada: se vuelve a pedir. */
  private escribir(pedido: Observable<void>, alTerminar: () => void): Observable<void> {
    return pedido.pipe(
      tap({
        next: alTerminar,
        error: (error: unknown) => {
          if (error instanceof CapaNoEncontrada) {
            this.cargar();
          }
        }
      })
    );
  }

  private actualizarEnLasDemasListas(id: number, datos: Partial<ResumenCapa>): void {
    this.capasAbiertas.actualizar(id, datos);
    this.favoritas.actualizar(id, datos);
  }
}

/** El mismo orden de la API, que ordena en español: "Árboles" junto a "Arroyos". */
function porNombre(capas: CapaPropia[]): CapaPropia[] {
  return [...capas].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es-CL'));
}

export function mensajeDeEscritura(error: unknown): string {
  return error instanceof CapaNoEncontrada
    ? 'La capa ya no existe o no tiene permiso para modificarla.'
    : 'No se pudo guardar el cambio. Intente nuevamente.';
}

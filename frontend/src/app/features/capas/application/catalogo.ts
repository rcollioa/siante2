import { computed, inject, Injectable, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { CapaCatalogo, coincide, SeccionCatalogo } from '../domain/capa';
import { CapasRepositorio } from './port/out/capas.repositorio';

/** El catálogo de capas de SIATE: una pestaña a la vez y un buscador sobre la pestaña elegida. */
@Injectable({ providedIn: 'root' })
export class Catalogo {
  private readonly repositorio = inject(CapasRepositorio);
  private pedido?: Subscription;
  private readonly seccionActual = signal<SeccionCatalogo>('PUBLICAS');
  private readonly estadoActual = signal<'cargando' | 'listo' | 'error'>('cargando');
  private readonly lista = signal<CapaCatalogo[]>([]);
  private readonly textoActual = signal('');

  readonly seccion = this.seccionActual.asReadonly();
  readonly estado = this.estadoActual.asReadonly();
  readonly texto = this.textoActual.asReadonly();
  /** Todas las capas de la pestaña, sin filtrar. */
  readonly total = computed(() => this.lista().length);
  readonly capas = computed(() => {
    const texto = this.textoActual();
    return this.lista().filter((capa) => coincide(capa, texto));
  });

  /** Abre el catálogo en una pestaña, sin búsqueda, y la descarga. */
  abrir(seccion: SeccionCatalogo): void {
    this.textoActual.set('');
    this.mostrar(seccion);
  }

  /** Cambiar de pestaña mantiene lo que se estaba buscando, como en SIATE. */
  mostrar(seccion: SeccionCatalogo): void {
    this.seccionActual.set(seccion);
    this.cargar();
  }

  buscar(texto: string): void {
    this.textoActual.set(texto);
  }

  cargar(): void {
    this.pedido?.unsubscribe();
    this.estadoActual.set('cargando');
    this.lista.set([]);
    this.pedido = this.repositorio.catalogo(this.seccionActual()).subscribe({
      next: (capas) => {
        this.lista.set(capas);
        this.estadoActual.set('listo');
      },
      error: () => this.estadoActual.set('error')
    });
  }
}

import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { DatosIcono, Icono } from '../domain/icono';
import { IconosRepositorio } from './port/out/iconos.repositorio';

export type EstadoIconos = 'sin-cargar' | 'cargando' | 'listo' | 'error';

/** El catálogo de SIATE: los íconos del sistema y los que sube el usuario. Se pide una vez, al primer marcador editado. */
@Injectable({ providedIn: 'root' })
export class CatalogoIconos {
  private readonly repositorio = inject(IconosRepositorio);
  private readonly lista = signal<Icono[]>([]);
  private readonly estadoActual = signal<EstadoIconos>('sin-cargar');

  readonly estado = this.estadoActual.asReadonly();
  readonly delSistema = computed(() => this.lista().filter(({ propio }) => !propio));
  readonly propios = computed(() => this.lista().filter(({ propio }) => propio));

  /** Si ya está cargado o en camino no vuelve a pedirlo. */
  cargar(): void {
    if (this.estadoActual() === 'cargando' || this.estadoActual() === 'listo') {
      return;
    }
    this.estadoActual.set('cargando');
    this.repositorio.listar().subscribe({
      next: (iconos) => {
        this.lista.set(iconos);
        this.estadoActual.set('listo');
      },
      error: () => this.estadoActual.set('error')
    });
  }

  crear(datos: DatosIcono): Observable<Icono> {
    const limpios = limpiar(datos);
    return this.repositorio.crear(limpios).pipe(tap((icono) => this.lista.update((iconos) => [...iconos, icono])));
  }

  editar(id: number, datos: DatosIcono): Observable<void> {
    const limpios = limpiar(datos);
    return this.repositorio
      .editar(id, limpios)
      .pipe(tap(() => this.lista.update((iconos) => iconos.map((icono) => (icono.id === id ? { ...icono, ...limpios } : icono)))));
  }

  eliminar(id: number): Observable<void> {
    return this.repositorio.eliminar(id).pipe(tap(() => this.lista.update((iconos) => iconos.filter((icono) => icono.id !== id))));
  }
}

function limpiar(datos: DatosIcono): DatosIcono {
  return { ...datos, nombre: datos.nombre.trim() };
}

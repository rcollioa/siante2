import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, shareReplay } from 'rxjs';
import { API_PREFIJO } from '../api/api-prefijo';
import { components } from '../api/contrato';

export type Usuario = components['schemas']['Usuario'];
export type EstadoSesion = 'pendiente' | 'activa' | 'no-autorizado' | 'error' | 'finalizada';

/** Usuario de la sesión (lo identifica WebSEAL); se consulta una vez por carga de página. */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly http = inject(HttpClient);
  private consulta?: Observable<EstadoSesion>;

  readonly usuario = signal<Usuario | null>(null);
  readonly estado = signal<EstadoSesion>('pendiente');
  readonly nombreVisible = computed(() => {
    const u = this.usuario();
    return u ? [u.nombre, u.apellido].filter(Boolean).join(' ') : '';
  });

  iniciar(): Observable<EstadoSesion> {
    this.consulta ??= this.http.get<Usuario>(`${API_PREFIJO}/usuarios/actual`).pipe(
      map((usuario): EstadoSesion => {
        this.usuario.set(usuario);
        return 'activa';
      }),
      catchError((error: unknown) => {
        const sinPermiso = error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403);
        return of<EstadoSesion>(sinPermiso ? 'no-autorizado' : 'error');
      }),
      map((estado) => {
        this.estado.set(estado);
        if (estado === 'error') {
          this.consulta = undefined;
        }
        return estado;
      }),
      shareReplay(1)
    );
    return this.consulta;
  }

  /** Al salir: oculta el menú y, si vuelve a ingresar, consulta el usuario de nuevo. */
  finalizar(): void {
    this.consulta = undefined;
    this.usuario.set(null);
    this.estado.set('finalizada');
  }
}

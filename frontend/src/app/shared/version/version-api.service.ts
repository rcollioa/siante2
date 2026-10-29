import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_PREFIJO } from '../api/api-prefijo';
import { components } from '../api/contrato';
import { sinPantallaDeCarga } from '../pantalla-carga/pantalla-carga.interceptor';

export type Version = components['schemas']['Version'];

/** Versión del backend que atiende al usuario, para el pie del menú. */
@Injectable({ providedIn: 'root' })
export class VersionApiService {
  private readonly http = inject(HttpClient);

  obtener(): Observable<Version> {
    return this.http.get<Version>(`${API_PREFIJO}/version`, { context: sinPantallaDeCarga() });
  }
}

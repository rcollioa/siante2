import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, debounce, Observable, of, Subject, switchMap, timer } from 'rxjs';
import { MapaPort } from '../mapa/application/port/out/mapa.port';
import { Direccion, DireccionesApiService } from './direcciones-api.service';

type Estado = 'inicial' | 'sugerencias' | 'sin-sugerencias' | 'buscando' | 'listo' | 'error';

export const LARGO_MINIMO = 3;
/** Pausa tras la última tecla antes de sugerir: evita una consulta por letra. */
export const ESPERA_SUGERENCIAS_MS = 400;

/** Como SIATE, sugiere mientras se escribe (Photon, en el backend), primero lo cercano a la zona del mapa. Enter sin
 * elegir una sugerencia hace la búsqueda completa (Nominatim). Combobox de WAI-ARIA: el foco queda en el campo. */
@Component({
  selector: 'app-buscador-direcciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './buscador-direcciones.component.html',
  styleUrl: './buscador-direcciones.component.css'
})
export class BuscadorDireccionesComponent {
  private readonly api = inject(DireccionesApiService);
  private readonly mapa = inject(MapaPort);
  /** Texto a sugerir; null cancela la sugerencia pendiente (al buscar, elegir o cerrar). */
  private readonly escrito = new Subject<string | null>();

  protected readonly texto = signal('');
  protected readonly estado = signal<Estado>('inicial');
  protected readonly resultados = signal<Direccion[]>([]);
  protected readonly mensajeError = signal('');
  /** Opción resaltada con las flechas; -1 si ninguna. */
  protected readonly activa = signal(-1);

  protected readonly listaAbierta = computed(
    () => (this.estado() === 'sugerencias' || this.estado() === 'listo') && this.resultados().length > 0
  );
  /** Lo que escucha un lector de pantalla: una sola región, para no leer la lista completa en cada tecla. */
  protected readonly anuncio = computed(() => {
    switch (this.estado()) {
      case 'buscando':
        return 'Buscando…';
      case 'error':
        return this.mensajeError();
      case 'sin-sugerencias':
        return 'Las sugerencias no están disponibles.';
      case 'sugerencias':
      case 'listo':
        return `${this.resultados().length} resultados.`;
      default:
        return '';
    }
  });

  constructor() {
    this.escrito
      .pipe(
        debounce((texto) => timer(texto === null ? 0 : ESPERA_SUGERENCIAS_MS)),
        // switchMap descarta la respuesta anterior si el usuario siguió escribiendo.
        switchMap((texto): Observable<Direccion[] | 'fallo' | null> =>
          texto === null
            ? of(null)
            : this.api.sugerir(texto, this.mapa.zonaVisible()).pipe(catchError(() => of('fallo' as const)))
        ),
        takeUntilDestroyed()
      )
      .subscribe((sugerencias) => {
        if (sugerencias === null) {
          return;
        }
        this.activa.set(-1);
        this.resultados.set(sugerencias === 'fallo' ? [] : sugerencias);
        this.estado.set(sugerencias === 'fallo' ? 'sin-sugerencias' : 'sugerencias');
      });
  }

  protected escribir(evento: Event): void {
    const texto = (evento.target as HTMLInputElement).value;
    this.texto.set(texto);
    if (texto.trim().length < LARGO_MINIMO) {
      this.cerrarLista();
      return;
    }
    this.escrito.next(texto.trim());
  }

  protected teclear(evento: KeyboardEvent): void {
    const total = this.listaAbierta() ? this.resultados().length : 0;
    switch (evento.key) {
      case 'ArrowDown':
        if (total) {
          evento.preventDefault();
          this.activa.update((i) => (i + 1) % total);
        }
        break;
      case 'ArrowUp':
        if (total) {
          evento.preventDefault();
          this.activa.update((i) => (i <= 0 ? total : i) - 1);
        }
        break;
      case 'Escape':
        if (this.listaAbierta() || this.estado() !== 'inicial') {
          this.cerrarLista();
        } else {
          this.limpiar();
        }
        break;
    }
  }

  /** Enter: la sugerencia resaltada o, si no hay, la búsqueda completa. */
  protected buscar(evento: Event): void {
    evento.preventDefault();
    const resaltada = this.listaAbierta() ? this.resultados()[this.activa()] : undefined;
    if (resaltada) {
      this.elegir(resaltada);
      return;
    }
    const texto = this.texto().trim();
    if (texto.length < LARGO_MINIMO) {
      this.fallar(`Escriba al menos ${LARGO_MINIMO} caracteres.`);
      return;
    }
    this.escrito.next(null);
    this.activa.set(-1);
    this.estado.set('buscando');
    this.api.buscar(texto).subscribe({
      next: (direcciones) => {
        this.resultados.set(direcciones);
        this.estado.set('listo');
      },
      error: (error: unknown) =>
        this.fallar(
          error instanceof HttpErrorResponse && error.status === 400
            ? (error.error?.message ?? 'La búsqueda no es válida.')
            : 'La búsqueda de direcciones no está disponible. Intente más tarde.'
        )
    });
  }

  protected elegir(direccion: Direccion): void {
    this.mapa.irA(direccion.longitud, direccion.latitud);
    this.texto.set(direccion.nombre);
    this.cerrarLista();
  }

  protected limpiar(): void {
    this.texto.set('');
    this.cerrarLista();
    this.mapa.quitarMarcador();
  }

  /** Al salir del campo se cierran las sugerencias; los resultados de una búsqueda completa quedan. */
  protected alSalir(): void {
    if (this.estado() === 'sugerencias' || this.estado() === 'sin-sugerencias') {
      this.cerrarLista();
    }
  }

  private cerrarLista(): void {
    this.escrito.next(null);
    this.resultados.set([]);
    this.activa.set(-1);
    this.estado.set('inicial');
  }

  private fallar(mensaje: string): void {
    this.escrito.next(null);
    this.mensajeError.set(mensaje);
    this.resultados.set([]);
    this.activa.set(-1);
    this.estado.set('error');
  }
}

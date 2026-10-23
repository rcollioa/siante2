import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MapaService } from '../mapa/mapa.service';
import { Direccion, DireccionesApiService } from './direcciones-api.service';

type Estado = 'inicial' | 'buscando' | 'listo' | 'error';

export const LARGO_MINIMO = 3;

/** Busca al enviar (Enter o botón), no al escribir: Nominatim prohíbe el autocompletado. */
@Component({
  selector: 'app-buscador-direcciones',
  standalone: true,
  templateUrl: './buscador-direcciones.component.html',
  styleUrl: './buscador-direcciones.component.css'
})
export class BuscadorDireccionesComponent {
  private readonly api = inject(DireccionesApiService);
  private readonly mapa = inject(MapaService);

  protected readonly texto = signal('');
  protected readonly estado = signal<Estado>('inicial');
  protected readonly resultados = signal<Direccion[]>([]);
  protected readonly mensajeError = signal('');

  protected escribir(evento: Event): void {
    this.texto.set((evento.target as HTMLInputElement).value);
  }

  protected buscar(evento: Event): void {
    evento.preventDefault();
    const texto = this.texto().trim();
    if (texto.length < LARGO_MINIMO) {
      this.fallar(`Escriba al menos ${LARGO_MINIMO} caracteres.`);
      return;
    }
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
    this.resultados.set([]);
    this.estado.set('inicial');
  }

  protected limpiar(): void {
    this.texto.set('');
    this.resultados.set([]);
    this.estado.set('inicial');
    this.mapa.quitarMarcador();
  }

  private fallar(mensaje: string): void {
    this.mensajeError.set(mensaje);
    this.resultados.set([]);
    this.estado.set('error');
  }
}

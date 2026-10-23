import { Component, computed, inject, signal } from '@angular/core';
import { ARCHIVOS_AYUDA, AyudaApiService, Multimedia } from './ayuda-api.service';

type Estado = 'cargando' | 'listo' | 'error';

@Component({
  selector: 'app-ayuda',
  standalone: true,
  templateUrl: './ayuda.component.html',
  styleUrl: './ayuda.component.css'
})
export class AyudaComponent {
  private readonly api = inject(AyudaApiService);
  private readonly archivos = signal<Multimedia[]>([]);

  protected readonly correo = 'mesadeayuda@contraloria.cl';
  protected readonly estado = signal<Estado>('cargando');
  protected readonly manual = computed(() => this.url(ARCHIVOS_AYUDA.manual));
  protected readonly gifAnexos = computed(() => this.url(ARCHIVOS_AYUDA.visualizarAnexos));
  protected readonly gifMetadata = computed(() => this.url(ARCHIVOS_AYUDA.visualizarMetadata));

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.estado.set('cargando');
    this.api.listarMultimedia().subscribe({
      next: (archivos) => {
        this.archivos.set(archivos);
        this.estado.set('listo');
      },
      error: () => this.estado.set('error')
    });
  }

  private url(descripcion: string): string | null {
    const archivo = this.archivos().find((a) => a.descripcion === descripcion);
    return archivo ? this.api.urlArchivo(archivo.id) : null;
  }
}

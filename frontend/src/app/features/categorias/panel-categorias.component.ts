import { Component, inject, signal } from '@angular/core';
import { Categoria, CategoriasApiService } from './categorias-api.service';

type Estado = 'cargando' | 'listo' | 'error';

@Component({
  selector: 'app-panel-categorias',
  standalone: true,
  templateUrl: './panel-categorias.component.html',
  styleUrl: './panel-categorias.component.css'
})
export class PanelCategoriasComponent {
  private readonly api = inject(CategoriasApiService);

  protected readonly estado = signal<Estado>('cargando');
  protected readonly categorias = signal<Categoria[]>([]);

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.estado.set('cargando');
    this.api.listar().subscribe({
      next: (categorias) => {
        this.categorias.set(categorias);
        this.estado.set('listo');
      },
      error: () => this.estado.set('error')
    });
  }
}

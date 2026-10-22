import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Categoria, CategoriasApiService } from './categorias-api.service';
import { PanelCategoriasComponent } from './panel-categorias.component';

describe('PanelCategoriasComponent', () => {
  function crear(listar: () => ReturnType<CategoriasApiService['listar']>) {
    TestBed.configureTestingModule({
      imports: [PanelCategoriasComponent],
      providers: [{ provide: CategoriasApiService, useValue: { listar } }]
    });
    const fixture = TestBed.createComponent(PanelCategoriasComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('lista los nombres de las categorías', () => {
    const categorias: Categoria[] = [{ id: 1, nombre: 'Salud', descripcion: '' }];
    const elemento = crear(() => of(categorias));

    expect(elemento.querySelector('li')?.textContent?.trim()).toBe('Salud');
  });

  it('si la consulta falla, avisa y ofrece reintentar', () => {
    const elemento = crear(() => throwError(() => new Error('503')));

    expect(elemento.textContent).toContain('No se pudieron cargar las categorías.');
    expect(elemento.querySelector('button')?.textContent?.trim()).toBe('Reintentar');
  });
});

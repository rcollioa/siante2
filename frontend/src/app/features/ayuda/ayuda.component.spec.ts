import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AyudaApiService, Multimedia } from './ayuda-api.service';
import { AyudaComponent } from './ayuda.component';

describe('AyudaComponent', () => {
  function crear(listarMultimedia: () => ReturnType<AyudaApiService['listarMultimedia']>): HTMLElement {
    TestBed.configureTestingModule({
      imports: [AyudaComponent],
      providers: [
        { provide: AyudaApiService, useValue: { listarMultimedia, urlArchivo: (id: number) => `/archivo/${id}` } }
      ]
    });
    const fixture = TestBed.createComponent(AyudaComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('enlaza el manual para descargar y muestra las guías animadas con texto alternativo', () => {
    const archivos: Multimedia[] = [
      { id: 1, descripcion: 'Manual SIATE' },
      { id: 2, descripcion: 'Gif Visualizar' },
      { id: 3, descripcion: 'Gif Metadata' }
    ];
    const elemento = crear(() => of(archivos));

    const manual = elemento.querySelector<HTMLAnchorElement>('a[download]');
    const imagenes = Array.from(elemento.querySelectorAll('img'));

    expect(manual?.getAttribute('href')).toBe('/archivo/1');
    expect(imagenes.map((i) => i.getAttribute('src'))).toEqual(['/archivo/2', '/archivo/3']);
    expect(imagenes.every((i) => (i.getAttribute('alt') ?? '').length > 0)).toBe(true);
  });

  it('si no se puede consultar, avisa y ofrece reintentar, pero mantiene el contacto', () => {
    const elemento = crear(() => throwError(() => new Error('503')));

    expect(elemento.textContent).toContain('No se pudo consultar el material de ayuda.');
    expect(elemento.querySelector('button')?.textContent?.trim()).toBe('Reintentar');
    expect(elemento.querySelector('a[href="mailto:mesadeayuda@contraloria.cl"]')).not.toBeNull();
  });
});
